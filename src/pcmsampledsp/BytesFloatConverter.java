/*
 * =================================================
 * Copyright 2014 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package pcmsampledsp;

import java.nio.ByteBuffer;

/**
 * BytesFloatConverter.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class BytesFloatConverter implements Converter<float[]> {

    private static final int BYTES_PER_SAMPLE = 4;
    private static final BytesFloatConverter BIG_ENDIAN = new BytesFloatConverter(true);
    private static final BytesFloatConverter LITTLE_ENDIAN = new BytesFloatConverter(false);
    private boolean bigEndian;

    private BytesFloatConverter(final boolean bigEndian) {
        this.bigEndian = bigEndian;
    }

    public static BytesFloatConverter getInstance(final boolean bigEndian) {
        return bigEndian ? BIG_ENDIAN : LITTLE_ENDIAN;
    }


    @Override
    public void decode(final ByteBuffer source, final float[] array) {
        for (int sampleNumber = 0; sampleNumber < array.length; sampleNumber++) {
            array[sampleNumber] = decode(source);
        }
    }

    @Override
    public void encode(final float[] array, final ByteBuffer target) {
        for (final float i : array) {
            encode(i, target);
        }
    }

    @Override
    public void encode(final float[] array, final int length, final ByteBuffer target) {
        for (int i=0; i<length; i++) {
            encode(array[i], target);
        }
    }

    /**
     * Writes the given value to the given buffer.
     *
     * @param value a float value
     * @param target byte buffer
     */
    private void encode(final float value, final ByteBuffer target) {
        final int v = Float.floatToRawIntBits(value);
        if (bigEndian) {
            intBitsToByteBigEndian(v, target);
        } else {
            intBitsToByteLittleEndian(v, target);
        }
    }

    private void intBitsToByteBigEndian(final int value, final ByteBuffer target) {
        for (int byteIndex = 0; byteIndex < BYTES_PER_SAMPLE; byteIndex++) {
            final int shift = (BYTES_PER_SAMPLE - byteIndex - 1) * 8;
            final int i = (value >>> shift) & 0xFF;
            target.put((byte) i);
        }
    }

    private void intBitsToByteLittleEndian(final int value, final ByteBuffer target) {
        for (int byteIndex = 0; byteIndex < BYTES_PER_SAMPLE; byteIndex++) {
            final int shift = byteIndex * 8;
            final int i = (value >>> shift) & 0xFF;
            target.put((byte) i);
        }
    }

    /**
     * Reads bytes from the provided buffer and converts them into a float.
     *
     * @param source byte buffer
     * @return float
     */
    private float decode(final ByteBuffer source) {
        final int sample = bigEndian
                ? byteToIntBitsBigEndian(source)
                : byteToIntBitsLittleEndian(source);
        return Float.intBitsToFloat(sample);
    }

    private int byteToIntBitsLittleEndian(final ByteBuffer source) {
        int sample = 0;
        for (int byteIndex = 0; byteIndex < BYTES_PER_SAMPLE; byteIndex++) {
            final int aByte = source.get() & 0xff;
            sample += aByte << 8 * (byteIndex);
        }
        return sample;
    }

    private int byteToIntBitsBigEndian(final ByteBuffer source) {
        int sample = 0;
        for (int byteIndex = 0; byteIndex < BYTES_PER_SAMPLE; byteIndex++) {
            final int aByte = source.get() & 0xff;
            sample += aByte << (8 * (BYTES_PER_SAMPLE - byteIndex - 1));
        }
        return sample;
    }
}


