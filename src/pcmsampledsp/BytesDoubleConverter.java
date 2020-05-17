/*
 * =================================================
 * Copyright 2014 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package pcmsampledsp;

import java.nio.ByteBuffer;

/**
 * BytesDoubleConverter.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class BytesDoubleConverter implements Converter<double[]> {

    private static final int BYTES_PER_SAMPLE = 8;
    private static final BytesDoubleConverter BIG_ENDIAN = new BytesDoubleConverter(true);
    private static final BytesDoubleConverter LITTLE_ENDIAN = new BytesDoubleConverter(false);
    private boolean bigEndian;

    private BytesDoubleConverter(final boolean bigEndian) {
        this.bigEndian = bigEndian;
    }

    public static BytesDoubleConverter getInstance(final boolean bigEndian) {
        return bigEndian ? BIG_ENDIAN : LITTLE_ENDIAN;
    }


    @Override
    public void decode(final ByteBuffer source, final double[] array) {
        for (int sampleNumber = 0; sampleNumber < array.length; sampleNumber++) {
            array[sampleNumber] = decode(source);
        }
    }

    @Override
    public void encode(final double[] array, final ByteBuffer target) {
        for (final double i : array) {
            encode(i, target);
        }
    }

    @Override
    public void encode(final double[] array, final int length, final ByteBuffer target) {
        for (int i=0; i<length; i++) {
            encode(array[i], target);
        }
    }

    /**
     * Writes the given value to the given buffer.
     *
     * @param value a double value
     * @param target byte buffer
     */
    private void encode(final double value, final ByteBuffer target) {
        final long v = Double.doubleToRawLongBits(value);
        if (bigEndian) {
            longBitsToByteBigEndian(v, target);
        } else {
            longBitsToByteLittleEndian(v, target);
        }
    }

    private void longBitsToByteBigEndian(final long value, final ByteBuffer target) {
        for (int byteIndex = 0; byteIndex < BYTES_PER_SAMPLE; byteIndex++) {
            final int shift = (BYTES_PER_SAMPLE - byteIndex - 1) * 8;
            final long i = (value >>> shift) & 0xFF;
            target.put((byte) i);
        }
    }

    private void longBitsToByteLittleEndian(final long value, final ByteBuffer target) {
        for (int byteIndex = 0; byteIndex < BYTES_PER_SAMPLE; byteIndex++) {
            final int shift = byteIndex * 8;
            final long i = (value >>> shift) & 0xFF;
            target.put((byte) i);
        }
    }

    /**
     * Reads bytes from the provided buffer and converts them into a double.
     *
     * @param source byte buffer
     * @return double
     */
    private double decode(final ByteBuffer source) {
        final long sample = bigEndian
                ? byteToLongBitsBigEndian(source)
                : byteToLongBitsLittleEndian(source);
        return Double.longBitsToDouble(sample);
    }

    private long byteToLongBitsLittleEndian(final ByteBuffer source) {
        long sample = 0;
        for (int byteIndex = 0; byteIndex < BYTES_PER_SAMPLE; byteIndex++) {
            final long aByte = source.get() & 0xff;
            sample += aByte << 8 * (byteIndex);
        }
        return sample;
    }

    private long byteToLongBitsBigEndian(final ByteBuffer source) {
        long sample = 0;
        for (int byteIndex = 0; byteIndex < BYTES_PER_SAMPLE; byteIndex++) {
            final long aByte = source.get() & 0xff;
            sample += aByte << (8 * (BYTES_PER_SAMPLE - byteIndex - 1));
        }
        return sample;
    }
}


