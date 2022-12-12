package org.rdfhdt.hdt.dictionary.impl.kcat;

import org.rdfhdt.hdt.compact.bitmap.ModifiableBitmap;
import org.rdfhdt.hdt.util.io.Closer;

import java.io.Closeable;
import java.io.IOException;

public class BitmapTriple implements Closeable {
	private final ModifiableBitmap subjects;
	private final ModifiableBitmap predicates;
	private final ModifiableBitmap objects;

	public BitmapTriple(ModifiableBitmap subjects, ModifiableBitmap predicates, ModifiableBitmap objects) {
		this.subjects = subjects;
		this.predicates = predicates;
		this.objects = objects;
	}

	public ModifiableBitmap getSubjects() {
		return subjects;
	}

	public ModifiableBitmap getPredicates() {
		return predicates;
	}

	public ModifiableBitmap getObjects() {
		return objects;
	}

	@Override
	public void close() throws IOException {
		Closer.closeAll(subjects, predicates, objects);
	}
}
