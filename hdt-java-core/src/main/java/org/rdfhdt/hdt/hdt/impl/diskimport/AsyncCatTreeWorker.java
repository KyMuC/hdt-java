package org.rdfhdt.hdt.hdt.impl.diskimport;

import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTFactory;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.HDTSupplier;
import org.rdfhdt.hdt.iterator.utils.FluxStopTripleStringIterator;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.HDTOptionsKeys;
import org.rdfhdt.hdt.options.HideHDTOptions;
import org.rdfhdt.hdt.rdf.RDFFluxStop;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.Profiler;
import org.rdfhdt.hdt.util.concurrent.ExceptionThread;
import org.rdfhdt.hdt.util.concurrent.HeightTree;
import org.rdfhdt.hdt.util.listener.PrefixListener;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AsyncCatTreeWorker implements Closeable {
	private final CatTreeImpl impl;
	private final ExceptionThread mergeThread;

	private final FluxStopTripleStringIterator it;
	private final int kcat;
	private final HDTSupplier supplier;
	private final String baseURI;
	private final ProgressListener listener;
	private final Profiler catProfiler;
	private final Profiler profiler;
	private final HeightTree<CatTreeImpl.HDTFile> tree = new HeightTree<>();
	private boolean endread;
	private final Path hdtStore;
	private final Path hdtCatLocationPath;
	private HDT hdt;

	public AsyncCatTreeWorker(CatTreeImpl impl, RDFFluxStop fluxStop, HDTSupplier supplier, Iterator<TripleString> iterator, String baseURI, ProgressListener listener) throws IOException {
		this.impl = impl;
		kcat = impl.getkHDTCat();
		this.it = new FluxStopTripleStringIterator(iterator, fluxStop);
		this.supplier = supplier;
		this.baseURI = baseURI;
		this.listener = listener;
		this.catProfiler = Profiler.createOrLoadSubSection("asynccatloader", impl.getHdtFormat(), false, true);
		profiler = impl.getProfiler();

		hdtStore = impl.getBasePath().resolve("hdt-store");
		hdtCatLocationPath = impl.getBasePath().resolve("cat");

		Files.createDirectories(hdtStore);
		Files.createDirectories(hdtCatLocationPath);

		this.mergeThread = new ExceptionThread(this::runMergeThread, "CatTreeMergeThread")
				.attach(new ExceptionThread(this::runGenThread, "CatTreeGenThread"));
	}

	public void start() {
		mergeThread.startAll();
	}

	public HDT buildHDT() throws ParserException, IOException {
		try {
			mergeThread.joinAndCrashIfRequired();
		} catch (InterruptedException e) {
			throw new ParserException(e);
		} catch (ExceptionThread.ExceptionThreadException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			}
			if (e.getCause() instanceof ParserException) {
				throw (ParserException) e.getCause();
			}
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
			if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			}
			throw e;
		}

		return hdt;
	}

	private void runGenThread() throws IOException, ParserException {

		long gen = 0;
		boolean nextFile;
		do {
			// generate the hdt
			gen++;
			profiler.pushSection("generateHDT #" + gen);
			PrefixListener il = PrefixListener.of("gen#" + gen, listener);
			Path hdtLocation = hdtStore.resolve("hdt-" + gen + ".hdt");
			// help memory flooding algorithm
			System.gc();
			supplier.doGenerateHDT(it, baseURI, impl.getHdtFormat(), il, hdtLocation);
			il.clearThreads();

			nextFile = it.hasNextFlux();
			synchronized (tree) {
				tree.addElement(new CatTreeImpl.HDTFile(hdtLocation, 1), 1);
				endread = !nextFile;
				tree.notifyAll();
			}
			profiler.popSection();
		} while (nextFile);
	}

	private void runMergeThread() throws IOException, InterruptedException {
		long cat = 0;

		HideHDTOptions spec = new HideHDTOptions(impl.getHdtFormat(), Function.identity());

		spec.set(HDTOptionsKeys.HDTCAT_LOCATION, hdtCatLocationPath);

		while (!endread) {
			List<CatTreeImpl.HDTFile> lst;
			synchronized (tree) {
				while (true) {
					lst = tree.getMax(kcat);
					if (lst == null && !endread) {
						tree.wait();
					} else {
						break;
					}
				}
			}

			if (lst == null) {
				break; // end read
			}

			List<String> gen = lst.stream()
					.map(CatTreeImpl.HDTFile::getHdtFile)
					.map(Path::toAbsolutePath)
					.map(Path::toString)
					.collect(Collectors.toList());

			cat++;
			profiler.pushSection("catHDT #" + cat);
			PrefixListener ilc = PrefixListener.of("cat#" + cat, listener);
			Path hdtCatFileLocation = hdtStore.resolve("hdtcat-" + cat + ".hdt");

			// override the value to create the cat into hdtCatFileLocation
			spec.overrideValue(HDTOptionsKeys.LOADER_CATTREE_FUTURE_HDT_LOCATION_KEY, hdtCatFileLocation.toAbsolutePath());

			try (HDT abcat = HDTManager.catHDT(
					gen,
					spec,
					ilc)) {
				abcat.saveToHDT(hdtCatFileLocation.toAbsolutePath().toString(), ilc);
			}

			spec.overrideValue(HDTOptionsKeys.LOADER_CATTREE_FUTURE_HDT_LOCATION_KEY, null);

			ilc.clearThreads();

			// delete previous chunks
			for (CatTreeImpl.HDTFile nextHDT : lst) {
				Files.delete(nextHDT.getHdtFile());
			}
			// note the new hdt file and the number of chunks
			int chunks = (int) lst.stream().mapToLong(CatTreeImpl.HDTFile::getChunks).max().orElseThrow() + 1;
			synchronized (tree) {
				tree.addElement(new CatTreeImpl.HDTFile(hdtCatFileLocation, chunks), chunks);
			}

			profiler.popSection();
		}
		// end read, we can merge everything we can

		while (tree.size() > 1) {
			List<CatTreeImpl.HDTFile> lst = tree.getAll(kcat);

			List<String> gen = lst.stream()
					.map(CatTreeImpl.HDTFile::getHdtFile)
					.map(Path::toAbsolutePath)
					.map(Path::toString)
					.collect(Collectors.toList());

			cat++;
			profiler.pushSection("catHDT #" + cat);
			PrefixListener ilc = PrefixListener.of("cat#" + cat, listener);
			Path hdtCatFileLocation = hdtStore.resolve("hdtcat-" + cat + ".hdt");

			// override the value to create the cat into hdtCatFileLocation
			spec.overrideValue(HDTOptionsKeys.LOADER_CATTREE_FUTURE_HDT_LOCATION_KEY, hdtCatFileLocation.toAbsolutePath());

			try (HDT abcat = HDTManager.catHDT(
					gen,
					spec,
					ilc)) {
				abcat.saveToHDT(hdtCatFileLocation.toAbsolutePath().toString(), ilc);
			}

			spec.overrideValue(HDTOptionsKeys.LOADER_CATTREE_FUTURE_HDT_LOCATION_KEY, null);

			ilc.clearThreads();

			// delete previous chunks
			for (CatTreeImpl.HDTFile nextHDT : lst) {
				Files.delete(nextHDT.getHdtFile());
			}
			// note the new hdt file and the number of chunks
			int chunks = (int) lst.stream().mapToLong(CatTreeImpl.HDTFile::getChunks).max().orElseThrow() + 1;
			tree.addElement(new CatTreeImpl.HDTFile(hdtCatFileLocation, chunks), chunks);
		}

		if (tree.size() == 0) {
			// empty hdt
			hdt = HDTFactory.createHDT();
		} else {
			List<CatTreeImpl.HDTFile> hdts = tree.getAll(1);
			assert hdts.size() == 1;

			Path hdtFile = hdts.get(0).getHdtFile();

			try {
				// if a future HDT location has been asked, move to it and map the HDT
				if (impl.getFutureHDTLocation() != null) {
					Files.createDirectories(impl.getFutureHDTLocation().toAbsolutePath().getParent());
					Files.deleteIfExists(impl.getFutureHDTLocation());
					Files.move(hdtFile, impl.getFutureHDTLocation());
					hdt = new MapOnCallHDT(impl.getFutureHDTLocation());
				} else {
					// if no future location has been asked, load the HDT and delete it after
					hdt = HDTManager.loadHDT(hdtFile.toAbsolutePath().toString());
				}
			} finally {
				Files.deleteIfExists(hdtFile);
				profiler.stop();
				profiler.writeProfiling();
			}
		}
	}

	@Override
	public void close() throws IOException {
		catProfiler.close();
	}
}
