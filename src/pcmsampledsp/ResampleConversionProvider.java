/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package pcmsampledsp;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED;

/**
 * Allows the re-sampling of linear PCM encoded audio.
 * Since we don't compute FIR filter coefficients at runtime
 * not all re-sampling factors are supported. However, a reasonable effort is made.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see ResamplerAudioInputStream
 */
public class ResampleConversionProvider extends FormatConversionProvider {

    private static int[] PRIMES = new int[]{2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] { PCM_SIGNED, PCM_UNSIGNED, PCM_FLOAT };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] { PCM_SIGNED, PCM_UNSIGNED, PCM_FLOAT };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(final AudioFormat sourceFormat) {
        return new AudioFormat.Encoding[] {sourceFormat.getEncoding()};
    }

    @Override
    public AudioFormat[] getTargetFormats(final AudioFormat.Encoding targetEncoding, final AudioFormat sourceFormat) {
        // make sure we don't try to resample audio with unknown sample rate
        if (sourceFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED) {
            return new AudioFormat[0];
        }
        // make an exception for com.tagtraum.casampledsp.CA*** audio formats
        // they should rather be converted by classes from their own package (taking advantage of native code)
        if (sourceFormat.getClass().getName().startsWith("com.tagtraum.casampledsp.CA")) {
            return new AudioFormat[0];
        }
        if (sourceFormat.getEncoding().equals(targetEncoding)) return new AudioFormat[]{
                new AudioFormat(
                        sourceFormat.getEncoding(),
                        AudioSystem.NOT_SPECIFIED,
                        sourceFormat.getSampleSizeInBits(),
                        sourceFormat.getChannels(),
                        sourceFormat.getFrameSize(),
                        AudioSystem.NOT_SPECIFIED,
                        sourceFormat.isBigEndian(),
                        sourceFormat.properties()
                )
        };
        return new AudioFormat[0];
    }

    @Override
    public AudioInputStream getAudioInputStream(final AudioFormat targetFormat, final AudioInputStream sourceStream) {
        if (sourceStream.getFormat().getEncoding().equals(targetFormat.getEncoding())) {
            final Rational factor = Rational.valueOf(targetFormat.getSampleRate() / sourceStream.getFormat().getSampleRate());
            try {
                final Rational[] factors = resamplingFactors(factor);
                AudioInputStream parent = sourceStream;
                for (final Rational f : factors) {
                    parent = new ResamplerAudioInputStream(parent, f);
                }
                return parent;
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

    /**
     * Factorize re-sampling factor in order to use already built-in low pass filters.
     * The resulting factors are arranged so that factors greater than 1 come first (in order to not downsample
     * prematurely and loose information).
     *
     * @param factor re-sampling factor
     * @return multiple factors, starting with factors greater than 1
     */
    private static Rational[] resamplingFactors(final Rational factor) {
        int[] numerators = primeFactors(factor.getNumerator());
        int[] denominators = primeFactors(factor.getDenominator());
        int[] tempNumerators = numerators;
        int[] tempDenominators = denominators;
        final int minFactors = Math.min(numerators.length, denominators.length)-1;
        for (int factors = Math.max(numerators.length, denominators.length); factors>=minFactors && containsNoFactorGreaterThan(10, tempNumerators) && containsNoFactorGreaterThan(10, tempDenominators); factors--) {
            numerators = tempNumerators;
            denominators = tempDenominators;
            tempNumerators = toNFactors(factors, numerators);
            tempDenominators = toNFactors(factors, denominators);
        }

        final int steps = numerators.length;
        final Rational[] factors = new Rational[steps];
        for (int i=0; i<steps; i++) {
            final int numerator = numerators[numerators.length - i - 1];
            // prefer factors that are greater than 1, as that means we're not losing information
            for (int j=steps-1; j>=0; j--) {
                final int denominator = denominators[j];
                if (denominator > 0 && denominator < numerator) {
                    factors[i] = new Rational(numerator, denominator);
                    // mark as used
                    denominators[j] = 0;
                    break;
                }
            }
            // we didn't find a factor greater than 1, let's go with the one that's closest to 1
            if (factors[i] == null) {
                for (int j=steps-1; j>=0; j--) {
                    final int denominator = denominators[j];
                    if (denominator > 0) {
                        factors[i] = new Rational(numerator, denominator);
                        // mark as used
                        denominators[j] = 0;
                        break;
                    }
                }
            }
        }
        return factors;
    }
    
    private static boolean containsNoFactorGreaterThan(final int factor, final int[] factors) {
        for (final int i : factors) {
            if (i > factor) return false;
        }
        return true;
    }

    /**
     * Reduces the given factors to a list of <code>n</code> factors with the same product.
     * This function attempts to keep each individual factor low.
     *
     * @param length new number of factors
     * @param primeFactors given prime factors in ascending order
     * @return new factors in ascending order
     */
    private static int[] toNFactors(final int length, final int[] primeFactors) {
        final int[] newFactors = new int[length];
        if (primeFactors.length < length) {
            Arrays.fill(newFactors, 1);
            System.arraycopy(primeFactors, 0, newFactors, 0, primeFactors.length);
        } else {
            // fill from the end, large numbers
            System.arraycopy(primeFactors, primeFactors.length-length, newFactors, 0, length);
            // fill the rest, attempting to keep each factor low
            for (int i=primeFactors.length-length-1; i>=0; i--) {
                final int primeFactor = primeFactors[i];
                for (int nf=0; nf<newFactors.length; nf++) {
                    if (nf == newFactors.length-1 || newFactors[nf]*primeFactor<newFactors[nf+1]) {
                        newFactors[nf]*=primeFactor;
                        break;
                    }
                }
            }
        }
        Arrays.sort(newFactors);
        return newFactors;
    }

    /**
     * Factorize number using primes less than 100.
     *
     * @param number number of factorize
     * @return prime factors, ordered ascending by value
     * @throws IllegalArgumentException if the number contains a prime factor greater than 100
     */
    private static int[] primeFactors(final int number) throws IllegalArgumentException {
        final ArrayList<Integer> list = new ArrayList<>();
        primeFactors(number, list);
        final int[] primeFactors = new int[list.size()];
        for (int i=0; i<primeFactors.length; i++) {
            primeFactors[i] = list.get(i);
        }
        return primeFactors;
    }

    /**
     * Factorize number using primes less than 100.
     *
     * @param number number of factorize
     * @param primeFactors factors
     * @throws IllegalArgumentException if the number contains a prime factor greater than 100
     */
    private static void primeFactors(final int number, final List<Integer> primeFactors) throws IllegalArgumentException {
        if (number == 1) {
            primeFactors.add(1);
            return;
        }
        for (int prime : PRIMES) {
            if (number % prime == 0) {
                primeFactors.add(prime);
                final int rest = number / prime;
                if (rest != 1)
                    primeFactors(rest, primeFactors);
                return;
            }
        }
        throw new IllegalArgumentException("Failed to factorize " + number + " with primes less than 100.");
    }

}
