/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package pcmsampledsp;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.FormatConversionProvider;

/**
 * Is capable of converting audio of a given bit depth to a different bit depth,
 * as long as it is 8, 16, 24, or 32 bits.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class SampleSizeConversionProvider extends FormatConversionProvider {

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] { AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] { AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(final AudioFormat sourceFormat) {
        return new AudioFormat.Encoding[] {sourceFormat.getEncoding()};
    }

    @Override
    public AudioFormat[] getTargetFormats(final AudioFormat.Encoding targetEncoding, final AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(targetEncoding)) return new AudioFormat[]{
                new AudioFormat(
                        sourceFormat.getEncoding(),
                        sourceFormat.getSampleRate(),
                        8,
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels(),
                        sourceFormat.getFrameRate(),
                        sourceFormat.isBigEndian(),
                        sourceFormat.properties()
                ),
                new AudioFormat(
                        sourceFormat.getEncoding(),
                        sourceFormat.getSampleRate(),
                        16,
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels() * 2,
                        sourceFormat.getFrameRate(),
                        sourceFormat.isBigEndian(),
                        sourceFormat.properties()
                ),
                new AudioFormat(
                        sourceFormat.getEncoding(),
                        sourceFormat.getSampleRate(),
                        24,
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels() * 3,
                        sourceFormat.getFrameRate(),
                        sourceFormat.isBigEndian(),
                        sourceFormat.properties()
                ),
                new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sourceFormat.getSampleRate(),
                        32,
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels() * 4,
                        sourceFormat.getFrameRate(),
                        sourceFormat.isBigEndian(),
                        sourceFormat.properties()
                )
        };
        return new AudioFormat[0];
    }

    @Override
    public AudioInputStream getAudioInputStream(final AudioFormat targetFormat, final AudioInputStream sourceStream) {
        if (sourceStream.getFormat().getEncoding().equals(targetFormat.getEncoding())) {
            try {
                return new SampleSizeAudioInputStream(sourceStream, targetFormat.getSampleSizeInBits());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Conversion from " + sourceStream.getFormat() + " to " + targetFormat + " is not supported.", e);
            }
        }
        throw new IllegalArgumentException("Conversion from " + sourceStream.getFormat() + " to " + targetFormat + " is not supported.");
    }

    @Override
    public AudioInputStream getAudioInputStream(final AudioFormat.Encoding targetEncoding, final AudioInputStream sourceStream) {
        if (sourceStream.getFormat().getEncoding().equals(targetEncoding)) return sourceStream;
        throw new IllegalArgumentException("Conversion from " + sourceStream.getFormat() + " to " + targetEncoding + " is not supported.");
    }

}
