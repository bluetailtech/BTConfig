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

import static javax.sound.sampled.AudioFormat.Encoding.*;

/**
 * Resamples an input audio stream on the fly. Note that not all conversion are supported.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class ResamplerAudioInputStream extends AudioInputStream {

    private AudioInputStream stream;
    private ByteBuffer filteredBuffer = ByteBuffer.allocate(0);
    private ByteBuffer sourceBuffer;
    private Rational factor;
    private ResampleBufferMethod method;

    /**
     * Constructs an {@link AudioInputStream} for the desired sampling rate.
     *
     * @param stream stream to read from
     * @param factor re-sampling factor (numerator=upFactor, denominator=downFactor)
     * @throws IllegalArgumentException if a filter for the factor <code>max(upFactor,downFactor)</code> is not supported or the format is not supported
     */
    public ResamplerAudioInputStream(final AudioInputStream stream, final Rational factor) {
        super(stream, toTargetFormat(stream.getFormat(), factor), toTargetFrameLength(stream.getFrameLength(), factor));
        this.stream = stream;
        this.sourceBuffer = ByteBuffer.allocate(stream.getFormat().getFrameSize() * 1024 * 8);
        this.factor = factor.reduce();
        final int channels = stream.getFormat().getChannels();
        final Resampler[] resamplers = new Resampler[channels];
        for (int channel = 0; channel < channels; channel++) {
            resamplers[channel] = new Resampler(factor);
        }
        final int bytesPerSample = stream.getFormat().getSampleSizeInBits() / 8;
        final boolean bigEndian = stream.getFormat().isBigEndian();
        final boolean signed = PCM_SIGNED.equals(stream.getFormat().getEncoding())
                || PCM_FLOAT.equals(stream.getFormat().getEncoding());

        if (PCM_SIGNED.equals(stream.getFormat().getEncoding())
                || PCM_UNSIGNED.equals(stream.getFormat().getEncoding())) {
            method = new ResampleBufferInt(BytesIntConverter.getInstance(bytesPerSample, bigEndian, signed), resamplers);
        } else if (PCM_FLOAT.equals(stream.getFormat().getEncoding())) {
            if (stream.getFormat().getSampleSizeInBits() == 32) {
                method = new ResampleBufferFloat(BytesFloatConverter.getInstance(bigEndian), resamplers);
            } else if (stream.getFormat().getSampleSizeInBits() == 64) {
                method = new ResampleBufferDouble(BytesDoubleConverter.getInstance(bigEndian), resamplers);
            } else {
                throw new IllegalArgumentException("Sample size not supported: " + format.getSampleSizeInBits());
            }
        } else {
            throw new IllegalArgumentException("Encoding not supported: " + format.getEncoding());
        }
    }

    private static AudioFormat toTargetFormat(final AudioFormat sourceFormat, final Rational factor) {
        // weird conversion to double to avoid numerical problems
        return new AudioFormat(
                sourceFormat.getEncoding(),
                (float)(sourceFormat.getSampleRate() * factor.doubleValue()),
                sourceFormat.getSampleSizeInBits(),
                sourceFormat.getChannels(),
                sourceFormat.getFrameSize(),
                (float)(sourceFormat.getFrameRate() * factor.doubleValue()),
                sourceFormat.isBigEndian(),
                sourceFormat.properties()
        );
    }

    private static long toTargetFrameLength(final long length, final Rational factor) {
        if (length < 0) return length;
        // do we round correctly?
        return (long) (length * factor.doubleValue());
    }


    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (!filteredBuffer.hasRemaining()) {
            method.resampleBuffer();
        }
        if (!filteredBuffer.hasRemaining()) return -1;

        int transferred = 0;
        while (filteredBuffer.hasRemaining() && transferred < len) {
            final int chunk = Math.min(len - transferred, filteredBuffer.remaining());
            filteredBuffer.get(b, off + transferred, chunk);
            transferred += chunk;
            if (!filteredBuffer.hasRemaining()) {
                method.resampleBuffer();
            }
        }
        return transferred;
    }

    @Override
    public int available() throws IOException {
        if (!filteredBuffer.hasRemaining()) {
            method.resampleBuffer();
        }
        return filteredBuffer.remaining();
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
        if (!filteredBuffer.hasRemaining()) {
            method.resampleBuffer();
        }
        while (filteredBuffer.hasRemaining() && skipped < len) {
            final int chunk = (int) Math.min(len - skipped, filteredBuffer.remaining());
            filteredBuffer.position(filteredBuffer.position() + chunk);
            skipped += chunk;
            if (!filteredBuffer.hasRemaining()) {
                method.resampleBuffer();
            }
        }
        return skipped;
    }

    /**
     * Encapsulates access to the actual resampling code.
     */
    private interface ResampleBufferMethod {

        /**
         * Reads data from the stream, resamples it and fills the filteredBuffer with the result.
         *
         * @throws IOException should something go wrong
         */
        void resampleBuffer() throws IOException;
    }

    private class ResampleBufferInt implements ResampleBufferMethod {

        private final Converter<int[]> converter;
        private final Resampler[] resamplers;

        private ResampleBufferInt(final Converter<int[]> converter, final Resampler[] resamplers) {
            this.converter = converter;
            this.resamplers = resamplers;
        }

        @Override
        public void resampleBuffer() throws IOException {
            // read from underlying stream
            final int newLimit = stream.read(sourceBuffer.array());
            if (newLimit >= 0) {
                sourceBuffer.position(0);
                sourceBuffer.limit(newLimit);
            } else {
                sourceBuffer.limit(0);
            }
            // convert to int[] buffer
            final int bytesPerSample = stream.getFormat().getSampleSizeInBits() / 8;
            final int channelCount = resamplers.length;
            final int[] in = new int[newLimit / bytesPerSample];
            converter.decode(sourceBuffer, in);
            // re-sample
            int resampledLength = (int)  Math.ceil((long) in.length * factor.doubleValue()) / channelCount * (channelCount + 1);

            final int[] out = new int[resampledLength];
            int realOutLength = out.length;
            for (int channel = 0; channel < channelCount; channel++) {
                final int length = resamplers[channel].resample(in, out, channel, channelCount);
                realOutLength = Math.min(length, realOutLength);
                //System.out.println("realOutLength: " + realOutLength);
            }
            // convert back to byte[] buffer
            if (filteredBuffer == null || filteredBuffer.capacity() != out.length * bytesPerSample) {
                filteredBuffer = ByteBuffer.allocate(out.length * bytesPerSample);
            }
            filteredBuffer.limit(filteredBuffer.capacity());
            filteredBuffer.rewind();
            converter.encode(out, realOutLength, filteredBuffer);
            filteredBuffer.flip();
        }
    }

    private class ResampleBufferFloat implements ResampleBufferMethod {

        private final Converter<float[]> converter;
        private final Resampler[] resamplers;

        private ResampleBufferFloat(final Converter<float[]> converter, final Resampler[] resamplers) {
            this.converter = converter;
            this.resamplers = resamplers;
        }

        @Override
        public void resampleBuffer() throws IOException {
            // read from underlying stream
            final int newLimit = stream.read(sourceBuffer.array());
            if (newLimit >= 0) {
                sourceBuffer.position(0);
                sourceBuffer.limit(newLimit);
            } else {
                sourceBuffer.limit(0);
            }
            // convert to int[] buffer
            final int bytesPerSample = stream.getFormat().getSampleSizeInBits() / 8;
            final int channelCount = resamplers.length;
            final float[] in = new float[newLimit / bytesPerSample];
            converter.decode(sourceBuffer, in);
            // re-sample
            int resampledLength = (int)  Math.ceil((long) in.length * factor.doubleValue()) / channelCount * (channelCount + 1);

            final float[] out = new float[resampledLength];
            int realOutLength = out.length;
            for (int channel = 0; channel < channelCount; channel++) {
                final int length = resamplers[channel].resample(in, out, channel, channelCount);
                realOutLength = Math.min(length, realOutLength);
                //System.out.println("realOutLength: " + realOutLength);
            }
            // convert back to byte[] buffer
            if (filteredBuffer == null || filteredBuffer.capacity() != out.length * bytesPerSample) {
                filteredBuffer = ByteBuffer.allocate(out.length * bytesPerSample);
            }
            filteredBuffer.limit(filteredBuffer.capacity());
            filteredBuffer.rewind();
            converter.encode(out, realOutLength, filteredBuffer);
            filteredBuffer.flip();
        }
    }

    private class ResampleBufferDouble implements ResampleBufferMethod {

        private final Converter<double[]> converter;
        private final Resampler[] resamplers;

        private ResampleBufferDouble(final Converter<double[]> converter, final Resampler[] resamplers) {
            this.converter = converter;
            this.resamplers = resamplers;
        }

        @Override
        public void resampleBuffer() throws IOException {
            // read from underlying stream
            final int newLimit = stream.read(sourceBuffer.array());
            if (newLimit >= 0) {
                sourceBuffer.position(0);
                sourceBuffer.limit(newLimit);
            } else {
                sourceBuffer.limit(0);
            }
            // convert to int[] buffer
            final int bytesPerSample = stream.getFormat().getSampleSizeInBits() / 8;
            final int channelCount = resamplers.length;
            final double[] in = new double[newLimit / bytesPerSample];
            converter.decode(sourceBuffer, in);
            // re-sample
            int resampledLength = (int)  Math.ceil((long) in.length * factor.doubleValue()) / channelCount * (channelCount + 1);

            final double[] out = new double[resampledLength];
            int realOutLength = out.length;
            for (int channel = 0; channel < channelCount; channel++) {
                final int length = resamplers[channel].resample(in, out, channel, channelCount);
                realOutLength = Math.min(length, realOutLength);
                //System.out.println("realOutLength: " + realOutLength);
            }
            // convert back to byte[] buffer
            if (filteredBuffer == null || filteredBuffer.capacity() != out.length * bytesPerSample) {
                filteredBuffer = ByteBuffer.allocate(out.length * bytesPerSample);
            }
            filteredBuffer.limit(filteredBuffer.capacity());
            filteredBuffer.rewind();
            converter.encode(out, realOutLength, filteredBuffer);
            filteredBuffer.flip();
        }
    }
}
