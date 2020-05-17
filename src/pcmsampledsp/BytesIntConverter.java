/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package pcmsampledsp;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>
 * Simple converter from bytes to ints and back, while taking into account
 * the sign, bytes per int and byte order.
 * </p>
 * <p>
 * Specialized implementations (i.e. fast versions) are provided for signed 16bit and 24bit formats.
 * </p>
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public abstract class BytesIntConverter implements Converter<int[]> {

    /**
     * A constant holding the minimum value a <code>signed24bit</code> can
     * have, -2<sup>23</sup>.
     */
    private static final int MIN_VALUE_24BIT = -2 << 22;

    /**
     * A constant holding the maximum value a <code>signed24bit</code> can
     * have, 2<sup>23</sup>-1.
     */
    private static final int MAX_VALUE_24BIT = -MIN_VALUE_24BIT - 1;


    private static final BytesIntConverter BE_16BIT_SIGNED = new BytesIntBE16BitSigned();
    private static final BytesIntConverter LE_16BIT_SIGNED = new BytesIntLE16BitSigned();
    private static final BytesIntConverter BE_24BIT_SIGNED = new BytesIntBE24BitSigned();
    private static final BytesIntConverter LE_24BIT_SIGNED = new BytesIntLE24BitSigned();

    private static final int[] SIGNED_MAX = {Byte.MAX_VALUE, Short.MAX_VALUE, MAX_VALUE_24BIT, Integer.MAX_VALUE};
    private static final int[] SIGNED_MIN = {Byte.MIN_VALUE, Short.MIN_VALUE, MIN_VALUE_24BIT, Integer.MIN_VALUE};
    private static final int[] UNSIGNED_MAX = {(2 << 7)-1, (2 << 15)-1, (2 << 23)-1, (2 << 31)-1};
    private static final int[] UNSIGNED_MIN = {0, 0, 0, 0};


    public static int maxPossibleValue(final int bitsPerSample, final boolean signed) {
        final int bytesPerSample = bitsPerSample / 8;
        final int max;
        if (signed) {
            max = SIGNED_MAX[bytesPerSample - 1];
        } else {
            max = UNSIGNED_MAX[bytesPerSample - 1];
        }
        return max;
    }

    public static int minPossibleValue(final int bitsPerSample, final boolean signed) {
        final int bytesPerSample = bitsPerSample / 8;
        final int min;
        if (signed) {
            min = SIGNED_MIN[bytesPerSample - 1];
        } else {
            min = UNSIGNED_MIN[bytesPerSample - 1];
        }
        return min;
    }

    /**
     * Returns a suitable instance.
     *
     * @param bytesPerSample bytes per sample
     * @param bigEndian big endian
     * @param signed signed
     * @return suitable converter instance
     */
    public static BytesIntConverter getInstance(final int bytesPerSample, final boolean bigEndian, final boolean signed) {
        if (bytesPerSample == 2 && signed) {
            return bigEndian ? BE_16BIT_SIGNED : LE_16BIT_SIGNED;
        } else if (bytesPerSample == 3 && signed) {
            return bigEndian ? BE_24BIT_SIGNED : LE_24BIT_SIGNED;
        } else {
            return new BytesIntConverterGeneric(bytesPerSample, bigEndian, signed);
        }
    }

    /**
     * Reads bytes from the provided buffer and converts them into an int.
     *
     * @param source byte buffer
     * @return integer
     * @throws java.io.IOException if the conversion fails
     */
    public abstract int decode(final ByteBuffer source) throws IOException;

    @Override
    public void decode(final ByteBuffer source, final int[] ibuf) throws IOException {
        for (int sampleNumber = 0; sampleNumber < ibuf.length; sampleNumber++) {
            ibuf[sampleNumber] = decode(source);
        }
    }

    /**
     * Writes the given value to the given buffer.
     *
     * @param value a integer value
     * @param target byte buffer
     */
    public abstract void encode(final int value, final ByteBuffer target);

    @Override
    public void encode(final int[] ibuf, final ByteBuffer target) {
        for (final int i : ibuf) {
            encode(i, target);
        }
    }

    @Override
    public void encode(final int[] ibuf, final int length, final ByteBuffer target) {
        for (int i=0; i<length; i++) {
            encode(ibuf[i], target);
        }
    }

    private static class BytesIntBE16BitSigned extends BytesIntConverter {
        @Override
        public int decode(final ByteBuffer source) {
            final int byte1 = source.get() & 0xff;
            final int byte2 = source.get() & 0xff;
            return (short)((byte1 << 8) + byte2);
        }

        @Override
        public void encode(final int value, final ByteBuffer target) {
            // avoid artifacts due to clipping
            final int v = value > Short.MAX_VALUE
                    ? Short.MAX_VALUE
                    : value < Short.MIN_VALUE ? Short.MIN_VALUE : value;

            target.put((byte) ((v >>> 8) & 0xFF));
            target.put((byte) (v & 0xFF));
        }
    }

    private static class BytesIntLE16BitSigned extends BytesIntConverter {
        @Override
        public int decode(final ByteBuffer source) {
            final int byte1 = source.get() & 0xff;
            final int byte2 = source.get() & 0xff;
            return (short)((byte2 << 8) + byte1);
        }

        @Override
        public void encode(final int value, final ByteBuffer target) {
            // avoid artifacts due to clipping
            final int v = value > Short.MAX_VALUE
                    ? Short.MAX_VALUE
                    : value < Short.MIN_VALUE ? Short.MIN_VALUE : value;

            target.put((byte) (v & 0xFF));
            target.put((byte) ((v >>> 8) & 0xFF));
        }
    }

    private static class BytesIntBE24BitSigned extends BytesIntConverter {
        @Override
        public int decode(final ByteBuffer source) {
            final int byte1 = source.get() & 0xff;
            final int byte2 = source.get() & 0xff;
            final int byte3 = source.get() & 0xff;
            final int sample = (byte1 << 16) + (byte2 << 8) + byte3;
            return sample > MAX_VALUE_24BIT ? sample + MIN_VALUE_24BIT + MIN_VALUE_24BIT : sample;
        }

        @Override
        public void encode(final int value, final ByteBuffer target) {
            // avoid artifacts due to clipping
            final int v = value > MAX_VALUE_24BIT
                    ? MAX_VALUE_24BIT
                    : value < MIN_VALUE_24BIT ? MIN_VALUE_24BIT : value;

            target.put((byte) ((v >>> 16) & 0xFF));
            target.put((byte) ((v >>> 8) & 0xFF));
            target.put((byte) (v & 0xFF));
        }
    }

    private static class BytesIntLE24BitSigned extends BytesIntConverter {
        @Override
        public int decode(final ByteBuffer source) {
            final int byte1 = source.get() & 0xff;
            final int byte2 = source.get() & 0xff;
            final int byte3 = source.get() & 0xff;
            int sample = ((byte3 << 16) + (byte2 << 8) + byte1);
            return sample > MAX_VALUE_24BIT ? sample + MIN_VALUE_24BIT + MIN_VALUE_24BIT : sample;
        }

        @Override
        public void encode(final int value, final ByteBuffer target) {
            // avoid artifacts due to clipping
            final int v = value > MAX_VALUE_24BIT
                    ? MAX_VALUE_24BIT
                    : value < MIN_VALUE_24BIT ? MIN_VALUE_24BIT : value;

            target.put((byte) (v & 0xFF));
            target.put((byte) ((v >>> 8) & 0xFF));
            target.put((byte) ((v >>> 16) & 0xFF));
        }
    }

    private static class BytesIntConverterGeneric extends BytesIntConverter {

        private boolean bigEndian;
        private boolean signed;
        private int bytesPerSample;
        private int min;
        private int max;

        private BytesIntConverterGeneric(final int bytesPerSample, final boolean bigEndian, final boolean signed) {
            this.bigEndian = bigEndian;
            this.bytesPerSample = bytesPerSample;
            this.signed = signed;
            this.max = maxPossibleValue(bytesPerSample*8, signed);
            this.min = minPossibleValue(bytesPerSample*8, signed);
        }

        public void encode(final int value, final ByteBuffer target) {
            // avoid artifacts due to clipping
            final int v = value > max
                    ? max
                    : value < min ? min : value;

            if (bigEndian) {
                intToByteBigEndian(v, target);
            } else {
                intToByteLittleEndian(v, target);
            }
        }

        private void intToByteBigEndian(final int value, final ByteBuffer target) {
            for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
                final int shift = (bytesPerSample - byteIndex - 1) * 8;
                final int i = (value >>> shift) & 0xFF;
                target.put((byte) i);
            }
        }

        private void intToByteLittleEndian(final int value, final ByteBuffer target) {
            for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
                final int shift = byteIndex * 8;
                final int i = (value >>> shift) & 0xFF;
                target.put((byte) i);
            }
        }

        public int decode(final ByteBuffer source) throws IOException {
            final int sample = bigEndian ? byteToIntBigEndian(source) : byteToIntLittleEndian(source);
            final int value;

            if (signed) {
                switch (bytesPerSample) {
                    case 1:
                        final byte byteSample = (byte) sample;
                        value = byteSample;
                        break;
                    case 2:
                        final short shortSample = (short) sample;
                        value = shortSample;
                        break;
                    case 3:
                        final int threeByteSample = sample > MAX_VALUE_24BIT ? sample + MIN_VALUE_24BIT + MIN_VALUE_24BIT : sample;
                        value = threeByteSample;
                        break;
                    case 4:
                        value = sample;
                        break;
                    default:
                        throw new IOException(bytesPerSample + " bytes per channel not supported.");
                }
            } else {
                if (bytesPerSample > 3)
                    throw new IOException(bytesPerSample + " bytes per channel not supported with signed = " + signed);
                value = sample;
            }
            return value;
        }

        private int byteToIntLittleEndian(final ByteBuffer source) {
            int sample = 0;
            for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
                final int aByte = source.get() & 0xff;
                sample += aByte << 8 * (byteIndex);
            }
            return sample;
        }

        public int byteToIntBigEndian(final ByteBuffer source) {
            int sample = 0;
            for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
                final int aByte = source.get() & 0xff;
                sample += aByte << (8 * (bytesPerSample - byteIndex - 1));
            }
            return sample;
        }
    }


}
