/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package pcmsampledsp;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Changes the sample size on the fly. Note that not all conversion are supported.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class SampleSizeAudioInputStream extends AudioInputStream {

    private static final NoDither NO_DITHER = new NoDither();
    private AudioInputStream stream;
    private ByteBuffer targetBuffer = ByteBuffer.allocate(0);
    private ByteBuffer sourceBuffer;
    private Converter<int[]> decoder;
    private Converter<int[]> encoder;
    private int fromBytesPerSample;
    private int toBytesPerSample;
    private Dither dither;
    private int rightShift;

    /**
     * Constructs an {@link javax.sound.sampled.AudioInputStream} for the desired sample size.
     *
     * @param stream stream to read from
     * @param sampleSizeInBits new sample size in bits
     */
    public SampleSizeAudioInputStream(final AudioInputStream stream, final int sampleSizeInBits) {
        super(stream, toTargetFormat(stream.getFormat(), sampleSizeInBits), stream.getFrameLength());
        this.stream = stream;
        this.sourceBuffer = ByteBuffer.allocate(stream.getFormat().getFrameSize() * 1024 * 8);
        final int fromBitsPerSample = stream.getFormat().getSampleSizeInBits();
        final int toBitsPerSample = sampleSizeInBits;

        this.rightShift = fromBitsPerSample - toBitsPerSample;
        this.fromBytesPerSample = fromBitsPerSample / 8;
        this.toBytesPerSample = toBitsPerSample / 8;

        final boolean bigEndian = stream.getFormat().isBigEndian();
        final boolean signed = AudioFormat.Encoding.PCM_SIGNED.equals(stream.getFormat().getEncoding());

        this.decoder = BytesIntConverter.getInstance(fromBytesPerSample, bigEndian, signed);
        this.encoder = BytesIntConverter.getInstance(toBytesPerSample, bigEndian, signed);

        if (fromBytesPerSample > toBytesPerSample) {
            dither = new TPDFDither(fromBitsPerSample - toBitsPerSample);
        } else {
            dither = NO_DITHER;
        }
    }

    private static AudioFormat toTargetFormat(final AudioFormat sourceFormat, final int sampleSizeInBits) {
        // weird conversion to double to avoid numerical problems
        return new AudioFormat(
                sourceFormat.getEncoding(),
                sourceFormat.getSampleRate(),
                sampleSizeInBits,
                sourceFormat.getChannels(),
                (sampleSizeInBits/8)*sourceFormat.getChannels(),
                sourceFormat.getFrameRate(),
                sourceFormat.isBigEndian(),
                sourceFormat.properties()
        );
    }

    private void fillBuffer() throws IOException {
        // read from underlying stream
        final int newLimit = stream.read(sourceBuffer.array());
        if (newLimit >= 0) {
            sourceBuffer.position(0);
            sourceBuffer.limit(newLimit);
        } else {
            sourceBuffer.limit(0);
        }
        // convert to int[] buffer
        final int[] ints = new int[newLimit / fromBytesPerSample];
        decoder.decode(sourceBuffer, ints);
        // scale
        if (rightShift > 0) {
            if (dither != NO_DITHER && fromBytesPerSample == 4) {
                // if we have 4 bytes, things may overflow,
                // therefore we must add dither noise to a long instead of an int
                for (int i=0; i<ints.length; i++) {
                    final long aLong = ints[i];
                    ints[i] = (int)((aLong + dither.next()) >> rightShift);
                }
            } else {
                for (int i=0; i<ints.length; i++) {
                    ints[i] = (ints[i] + dither.next()) >> rightShift;
                }
            }
        } else {
            for (int i=0; i<ints.length; i++) {
                ints[i] = ints[i] << -rightShift;
            }
        }
        // convert back to byte[] buffer
        if (targetBuffer == null || targetBuffer.capacity() != ints.length * toBytesPerSample) {
            targetBuffer = ByteBuffer.allocate(ints.length * toBytesPerSample);
        }
        targetBuffer.limit(targetBuffer.capacity());
        targetBuffer.rewind();
        encoder.encode(ints, targetBuffer);
        targetBuffer.flip();
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (!targetBuffer.hasRemaining()) {
            fillBuffer();
        }
        if (!targetBuffer.hasRemaining()) return -1;

        int transferred = 0;
        while (targetBuffer.hasRemaining() && transferred < len) {
            final int chunk = Math.min(len - transferred, targetBuffer.remaining());
            targetBuffer.get(b, off + transferred, chunk);
            transferred += chunk;
            if (!targetBuffer.hasRemaining()) {
                fillBuffer();
            }
        }
        return transferred;
    }

    @Override
    public int available() throws IOException {
        if (!targetBuffer.hasRemaining()) {
            fillBuffer();
        }
        return targetBuffer.remaining();
    }

    @Override
    public void mark(final int readlimit) {
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public long skip(final long len) throws IOException {
        int skipped = 0;
        if (!targetBuffer.hasRemaining()) {
            fillBuffer();
        }
        while (targetBuffer.hasRemaining() && skipped < len) {
            final int chunk = (int) Math.min(len - skipped, targetBuffer.remaining());
            targetBuffer.position(targetBuffer.position() + chunk);
            skipped += chunk;
            if (!targetBuffer.hasRemaining()) {
                fillBuffer();
            }
        }
        return skipped;
    }


    /**
     * Dither function used when reducing bit depth.
     *
     * @see <a href="http://www.users.qwest.net/~volt42/cadenzarecording/DitherExplained.pdf">Intro to dithering</a>
     */
    public interface Dither {
        int next();
    }

    private static class NoDither implements Dither{
        public int next() {
            return 0;
        }
    }

    private static class TPDFDither implements Dither{

        private Random random = new Random();
        private int n;

        private TPDFDither(final int sampleSizeDifferenceInBits) {
            this.n = 2 << (sampleSizeDifferenceInBits-1);
        }

        public int next() {
            return random.nextInt(n) - random.nextInt(n);
        }
    }
}
