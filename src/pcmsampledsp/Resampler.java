/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package pcmsampledsp;

/**
 * Resamples the input by upsampling, low pass filtering and then downsampling by
 * the given factors. The implementation aims for efficiency by not computing samples
 * that are later dropped anyway (polyphase decomposition).
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Resampler {


    private Rational factor;
    private FIRFilter[] filters;
    private double[] originalCoefficients;
    private long inOffset;
    private long outOffset;

    /**
     * Creates a resampler using a simple fir1 29th order (=30taps) low pass filter and the given up- and down-sample
     * factors. The filter used is based on the maximum of the two factors. Internally the up- and down-factors
     * are divided by their greatest common divisor to increase efficiency.
     *
     * @param factor resample factor
     * @throws IllegalArgumentException if a filter for the factor <code>max(upFactor,downFactor)</code> is not supported
     * @see FIRFilter
     */
    public Resampler(final Rational factor) {
        this(createFilter(factor).getCoefficients(), factor);
    }

    /**
     * Creates a resampler using a FIR filter based on the given coefficients and the given up- and down-sample
     * factors. The coefficients must be appropriate for the given factors. Internally the up- and down-factors
     * are divided by their greatest common divisor to increase efficiency.
     *
     * @param coefficients FIR filter coefficients
     * @param factor resample factor
     */
    public Resampler(final double[] coefficients, final Rational factor) {
        this.factor = factor.reduce();
        this.originalCoefficients = coefficients.clone();
        setCoefficients(coefficients);
    }

    /**
     * Creates a resampler using a FIR low pass filter and the given up- and down-sample
     * factors. The filter must be appropriate for the given factors. Internally the up- and down-factors
     * are divided by their greatest common divisor to increase efficiency.
     *
     * @param filter FIR filter
     * @param factor resample factor
     */
    public Resampler(final FIRFilter filter, final Rational factor) {
        this(filter.getCoefficients(), factor);
    }

    private static FIRFilter createFilter(final Rational factor) {
        final Rational reduced = factor.reduce();
        return FIRFilter.createFir1_29thOrderLowpass(Math.max(reduced.getNumerator(), reduced.getDenominator()));
    }

    private void setCoefficients(final double[] coeff) {
        // make sure we have a multiple
        final double[] coefficients;
        final int upFactor = this.factor.getNumerator();
        if (coeff.length % upFactor == 0) {
            coefficients = coeff;
        } else {
            // append zeros, if coeff.length isn't a multiple of factor
            coefficients = new double[(coeff.length/ upFactor + 1)* upFactor];
            System.arraycopy(coeff, 0, coefficients, 0, coeff.length);
        }
        // create multiple FIRFilters
        filters = new FIRFilter[upFactor];
        for (int i=0; i< upFactor; i++) {
            final double[] subCoefficients = new double[coefficients.length / upFactor];
            for (int j=0; j<subCoefficients.length; j++) {
                // in case of upsampling adjust coefficients, so that loudness/volume/magnitude/energy is preserved
                // we do this by multiplying with "upFactor"
                subCoefficients[j] = coefficients[i + j* upFactor] * upFactor;
            }
            filters[i] = new FIRFilter(subCoefficients);
        }
    }

    /**
     * Resampling factor.
     *
     * @return factor
     */
    public Rational getFactor() {
        return factor;
    }

    public int resample(final int[] in, final int[] out, final int channel, final int channelCount) {
        if (out == null || out.length == 0) return 0;
        final int upFactor = this.factor.getNumerator();
        final int downFactor = this.factor.getDenominator();

        int lastOutIndex = 0;
        for (int i=0; i<in.length; i+=channelCount) {
            final float sample = in[i+channel];
            final long upsampledIndexBase = (i + inOffset) * upFactor;
            for (int j=0; j<upFactor; j++) {
                final FIRFilter filter = filters[j];
                filter.addToDelayLine(sample);
                final long upsampledIndex = upsampledIndexBase + j * channelCount;
                if (upsampledIndex/channelCount % downFactor == 0) {
                    final long index = upsampledIndex / downFactor + channel - outOffset;
                    //System.out.println("index: " + index + " up: " + upsampledIndex);
                    //if (channel == 0) System.out.println("index: " + (upsampledIndex / downFactor + channel) + " up: " + upsampledIndex + " channel: " + channel + " outOffset: " + outOffset);
                    lastOutIndex = (int)index;
                    out[lastOutIndex] = (int)Math.round(filter.filter());
                }
            }
        }
        // remember offsets, so that we can pretend in and out are two ginormous arrays
        this.inOffset+=in.length;
        final int realLength = lastOutIndex + channelCount - channel;
        this.outOffset+= realLength;
        return realLength;
    }

    public int resample(final float[] in, final float[] out, final int channel, final int channelCount) {
        if (out == null || out.length == 0) return 0;
        final int upFactor = this.factor.getNumerator();
        final int downFactor = this.factor.getDenominator();

        int lastOutIndex = 0;
        for (int i=0; i<in.length; i+=channelCount) {
            final float sample = in[i+channel];
            final long upsampledIndexBase = (i + inOffset) * upFactor;
            for (int j=0; j<upFactor; j++) {
                final FIRFilter filter = filters[j];
                filter.addToDelayLine(sample);
                final long upsampledIndex = upsampledIndexBase + j * channelCount;
                if (upsampledIndex/channelCount % downFactor == 0) {
                    final long index = upsampledIndex / downFactor + channel - outOffset;
                    //System.out.println("index: " + index + " up: " + upsampledIndex);
                    //if (channel == 0) System.out.println("index: " + (upsampledIndex / downFactor + channel) + " up: " + upsampledIndex + " channel: " + channel + " outOffset: " + outOffset);
                    lastOutIndex = (int)index;
                    out[lastOutIndex] = (float)filter.filter();
                }
            }
        }
        // remember offsets, so that we can pretend in and out are two ginormous arrays
        this.inOffset+=in.length;
        final int realLength = lastOutIndex + channelCount - channel;
        this.outOffset+= realLength;
        return realLength;
    }

    public int resample(final double[] in, final double[] out, final int channel, final int channelCount) {
        final int upFactor = this.factor.getNumerator();
        final int downFactor = this.factor.getDenominator();

        int lastOutIndex = 0;
        for (int i=0; i<in.length; i+=channelCount) {
            final double sample = in[i+channel];
            final long upsampledIndexBase = (i + inOffset) * upFactor;
            for (int j=0; j<upFactor; j++) {
                final FIRFilter filter = filters[j];
                filter.addToDelayLine(sample);
                final long upsampledIndex = upsampledIndexBase + j * channelCount;
                if (upsampledIndex/channelCount % downFactor == 0) {
                    final long index = upsampledIndex / downFactor + channel - outOffset;
                    //System.out.println("index: " + index + " up: " + upsampledIndex);
                    //if (channel == 0) System.out.println("index: " + (upsampledIndex / downFactor + channel) + " up: " + upsampledIndex + " channel: " + channel + " outOffset: " + outOffset);
                    lastOutIndex = (int)index;
                    out[lastOutIndex] = filter.filter();
                }
            }
        }
        // remember offsets, so that we can pretend in and out are two ginormous arrays
        this.inOffset+=in.length;
        final int realLength = lastOutIndex + channelCount - channel;
        this.outOffset+= realLength;
        return realLength;
    }

    @Override
    public String toString() {
        return "Resampler{" +
                "factor=" + factor +
                '}';
    }
}
