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
 * Converts a mono signal to stereo by duplicating the mono channel.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see StereofyAudioInputStream
 */
public class StereofyConversionProvider extends FormatConversionProvider {

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[]{AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[]{AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(final AudioFormat sourceFormat) {
        return new AudioFormat.Encoding[] {sourceFormat.getEncoding()};
    }

    @Override
    public AudioFormat[] getTargetFormats(final AudioFormat.Encoding targetEncoding, final AudioFormat sourceFormat) {
        if (sourceFormat.getChannels() != 1) return new AudioFormat[0];
        return new AudioFormat[] {toStereoAudioFormat(sourceFormat)};
    }

    private static AudioFormat toStereoAudioFormat(final AudioFormat monoFormat) {
        return new AudioFormat(
                monoFormat.getEncoding(),
                monoFormat.getSampleRate(),
                monoFormat.getSampleSizeInBits(),
                2,
                monoFormat.getFrameSize() * 2,
                monoFormat.getFrameRate(),
                monoFormat.isBigEndian(),
                monoFormat.properties()
       );
    }

    @Override
    public AudioInputStream getAudioInputStream(final AudioFormat targetFormat, final AudioInputStream sourceStream) {
        if (sourceStream.getFormat().getEncoding().equals(targetFormat.getEncoding())) {
            if (sourceStream.getFormat().getChannels() == 1 && targetFormat.getChannels() == 1) {
                return sourceStream;
            }
            if (sourceStream.getFormat().getChannels() == 1 && targetFormat.getChannels() == 2) {
                // stereofy
                return new StereofyAudioInputStream(sourceStream, toStereoAudioFormat(sourceStream.getFormat()));
            }
        }
        throw new IllegalArgumentException("Conversion from " + sourceStream.getFormat() + " to " + targetFormat + " is not supported.");
    }

    @Override
    public AudioInputStream getAudioInputStream(final AudioFormat.Encoding targetEncoding, final AudioInputStream sourceStream) {
        if (sourceStream.getFormat().getEncoding().equals(targetEncoding)) {
            return sourceStream;
        }
        throw new IllegalArgumentException("Conversion from " + sourceStream.getFormat().getEncoding() + " to " + targetEncoding + " is not supported.");
    }

}
