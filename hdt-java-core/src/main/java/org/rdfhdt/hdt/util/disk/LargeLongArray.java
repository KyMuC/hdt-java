package org.rdfhdt.hdt.util.disk;

import org.rdfhdt.hdt.unsafe.UnsafeLongArray;
import org.rdfhdt.hdt.util.io.IOUtil;

import java.io.IOException;

/**
 * Implementation of {@link LongArray} using a LargeArray
 *
 * @author Antoine Willerval
 */
public class LargeLongArray implements LongArray {
    private UnsafeLongArray array;

    /**
     * @param array large array
     */
    public LargeLongArray(UnsafeLongArray array) {
        this.array = array;
    }

    @Override
    public long get(long index) {
        return array.get(index);
    }

    @Override
    public void set(long index, long value) {
        array.set(index, value);
    }

    @Override
    public long length() {
        return array.length();
    }

    @Override
    public int sizeOf() {
        return array.sizeOf();
    }

    @Override
    public void resize(long newSize) throws IOException {
        if (newSize > 0) {
            if (array.length() != newSize) {
                UnsafeLongArray a = IOUtil.createLargeArray(newSize, false);
                UnsafeLongArray.arraycopy(array, 0, a, 0, Math.min(newSize, array.length()));
                array = a;
            }
        }
    }

    @Override
    public void clear() {
        array.clear();
    }
}
