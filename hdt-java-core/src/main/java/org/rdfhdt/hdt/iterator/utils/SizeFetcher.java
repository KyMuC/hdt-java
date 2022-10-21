package org.rdfhdt.hdt.iterator.utils;

import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;

import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/**
 * Size a supplier AFTER the last element, the maximum sum size will be maxSize - 1 + sizeOf(lastElement)
 *
 * @param <E> supplier type
 */
public class SizeFetcher<E> implements Supplier<E> {
    public static SizeFetcher<TripleString> ofTripleString(Supplier<TripleString> supplier, long maxSize) {
        return new SizeFetcher<>(supplier, FileTripleIterator::estimateSize, maxSize);
    }

    public static SizeFetcher<TripleID> ofTripleLong(Supplier<TripleID> supplier, long maxSize) {
        return new SizeFetcher<>(supplier, tripleID -> 4L * Long.BYTES, maxSize);
    }

    private final Supplier<E> supplier;
    private final ToLongFunction<E> sizeGetter;

    private final long maxSize;

    private long size;

    public SizeFetcher(Supplier<E> supplier, ToLongFunction<E> sizeGetter, long maxSize) {
        this.supplier = supplier;
        this.sizeGetter = sizeGetter;
        this.maxSize = maxSize;
    }

    @Override
    public E get() {
        if (!canContinue()) {
            return null;
        }
        E e = supplier.get();

        if (e == null) {
            return null;
        }

        size += sizeGetter.applyAsLong(e);

        return e;
    }

    private boolean canContinue() {
        return size < maxSize;
    }

    public long getSize() {
        return size;
    }
}
