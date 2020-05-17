/*
 * =================================================
 * Copyright 2011 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package pcmsampledsp;

import java.util.Arrays;

/**
 * <p>
 * FIR (finite impulse response) filter.
 * </p>
 * <p>
 * To compute the coefficients you might want to use Matlab or the free package
 * <a href="http://www.gnu.org/software/octave/">Octave</a> (with additional Octave-Forge packages needed for signals).
 * E.g. for a low pass filter you could execute:</p>
 * <pre>
 * b = fir1(16,0.125)
 * </pre>
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see <a href="http://en.wikipedia.org/wiki/Finite_impulse_response">Wikipedia on FIR</a>
 */
public class FIRFilter {

    private int length;
    private double[] delayLine;
    private double[] impulseResponse;
    private int count = -1;
    private double[] coefficients;

    public FIRFilter(double[] coefficients) {
        setCoefficients(coefficients);
    }

    private void setCoefficients(final double[] coefs) {
        this.coefficients = coefs.clone();
        this.length = coefs.length;
        this.impulseResponse = coefs.clone();
        this.delayLine = new double[length];
        this.count = -1;
    }

    public double[] getCoefficients() {
        return coefficients.clone();
    }

    public float[] filter(final float[] data) {
        final float[] out = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            final float sample = data[i];
            addToDelayLine(sample);
            out[i] = (float) filter();
        }
        return out;
    }

    protected void addToDelayLine(final double sample) {
        this.count = (count + 1) % length;
        this.delayLine[count] = sample;
    }

    protected double filter() {
        double result = 0.0;
        int index = count;
        for (int i = 0; i < length; i++) {
            result += impulseResponse[i] * delayLine[index--];
            if (index < 0) index = length - 1;
        }
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FIRFilter firFilter = (FIRFilter) o;

        if (!Arrays.equals(coefficients, firFilter.coefficients)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return coefficients != null ? Arrays.hashCode(coefficients) : 0;
    }

    @Override
    public String toString() {
        return "FIRFilter{" +
                "coefficients=" + Arrays.toString(coefficients) +
                '}';
    }

    /**
     * 29th order Fir1 (Matlab/Octave) lowpass filter that lets <code>factor</code>-th-Nyquist pass (&#x03C9;=1/factor).
     * Supported factors are 2 to 10.
     *
     * @param factor factor
     * @return filter
     * @throws IllegalArgumentException if the factor is not supported.
     */
    public static FIRFilter createFir1_29thOrderLowpass(final int factor) throws IllegalArgumentException {
        if (factor == 2) return createFir1_29thOrderLowpassCutoffHalf();
        if (factor == 3) return createFir1_29thOrderLowpassCutoff3rd();
        if (factor == 4) return createFir1_29thOrderLowpassCutoff4th();
        if (factor == 5) return createFir1_29thOrderLowpassCutoff5th();

        //if (factor == 6) return createFir1_29thOrderLowpassCutoff6th();
        //if (factor == 6) return createFir1_29thOrderLowpassCutoff10th();
        if (factor == 6) return createFir1_29thOrderLowpassCutoff9th();

        if (factor == 7) return createFir1_29thOrderLowpassCutoff7th();
        if (factor == 8) return createFir1_29thOrderLowpassCutoff8th();
        if (factor == 9) return createFir1_29thOrderLowpassCutoff9th();
        if (factor == 10) return createFir1_29thOrderLowpassCutoff10th();
        throw new IllegalArgumentException("Frequency factor " + factor + " is not supported.");
    }

    /**
     * 29th order (30 taps) Fir1 (Octave) lowpass filter that lets half-Nyquist pass (&#x03C9;=0.5).
     *
     * @return filter
     */
    private static FIRFilter createFir1_29thOrderLowpassCutoffHalf() {
        return new FIRFilter(
                new double[]{
                        -1.18646547823891e-03,
                        1.57585664015070e-03,
                        2.12164099205833e-03,
                        -3.52228955614070e-03,
                        -5.02584407183489e-03,
                        7.91566084494587e-03,
                        1.07564874478023e-02,
                        -1.58207695221185e-02,
                        -2.08612682967759e-02,
                        2.95718934522116e-02,
                        3.93912792338188e-02,
                        -5.68381953130699e-02,
                        -8.35068242125112e-02,
                        1.47200709296976e-01,
                        4.48524372276723e-01,
                        4.48524372276723e-01,
                        1.47200709296976e-01,
                        -8.35068242125112e-02,
                        -5.68381953130699e-02,
                        3.93912792338188e-02,
                        2.95718934522116e-02,
                        -2.08612682967759e-02,
                        -1.58207695221185e-02,
                        1.07564874478023e-02,
                        7.91566084494587e-03,
                        -5.02584407183489e-03,
                        -3.52228955614070e-03,
                        2.12164099205833e-03,
                        1.57585664015070e-03,
                        -1.18646547823891e-03,
                }
        );
    }

    /**
     * 29th order (30 taps) Fir1 (Octave) lowpass filter that lets third-Nyquist pass (&#x03C9;=1/3).
     *
     * @return filter
     */
    private static FIRFilter createFir1_29thOrderLowpassCutoff3rd() {
        return new FIRFilter(
                new double[]{
                        9.42678838042993e-04,
                        2.13326449678540e-03,
                        1.45205292697755e-03,
                        -2.54537326325320e-03,
                        -7.32365723740407e-03,
                        -5.14997870184591e-03,
                        8.14086157465924e-03,
                        2.18141213835782e-02,
                        1.44947193348719e-02,
                        -2.11121234353532e-02,
                        -5.63501453101618e-02,
                        -3.89304647367042e-02,
                        6.01519704055127e-02,
                        2.06730986795091e-01,
                        3.16053549004653e-01,
                        3.16053549004653e-01,
                        2.06730986795091e-01,
                        6.01519704055127e-02,
                        -3.89304647367042e-02,
                        -5.63501453101618e-02,
                        -2.11121234353533e-02,
                        1.44947193348719e-02,
                        2.18141213835782e-02,
                        8.14086157465925e-03,
                        -5.14997870184591e-03,
                        -7.32365723740407e-03,
                        -2.54537326325320e-03,
                        1.45205292697755e-03,
                        2.13326449678540e-03,
                        9.42678838042993e-04
                }
        );
    }

    /**
     * 29th order (30 taps) Fir1 (Octave) lowpass filter that lets quarter-Nyquist pass (&#x03C9;=0.25).
     *
     * @return filter
     */
    private static FIRFilter createFir1_29thOrderLowpassCutoff4th() {
        return new FIRFilter(
                new double[]{
                        -1.65165839781937e-03,
                        -1.94230115504218e-03,
                        -1.08299487218008e-03,
                        1.99734819354979e-03,
                        6.87301504785140e-03,
                        9.92395158975519e-03,
                        5.59860967542652e-03,
                        -8.83095445147214e-03,
                        -2.80296244036242e-02,
                        -3.77154441372856e-02,
                        -2.08878510383046e-02,
                        3.12101241762320e-02,
                        1.10254841606404e-01,
                        1.91003553938589e-01,
                        2.42114028568806e-01,
                        2.42114028568806e-01,
                        1.91003553938589e-01,
                        1.10254841606404e-01,
                        3.12101241762320e-02,
                        -2.08878510383046e-02,
                        -3.77154441372856e-02,
                        -2.80296244036242e-02,
                        -8.83095445147215e-03,
                        5.59860967542652e-03,
                        9.92395158975518e-03,
                        6.87301504785140e-03,
                        1.99734819354979e-03,
                        -1.08299487218008e-03,
                        -1.94230115504218e-03,
                        -1.65165839781937e-03,
                }
        );
    }

    /**
     * 29th order (30 taps) Fir1 (Octave) lowpass filter that lets fifth-Nyquist pass (&#x03C9;=0.2).
     *
     * @return filter
     */
    private static FIRFilter createFir1_29thOrderLowpassCutoff5th() {
        return new FIRFilter(
                new double[]{
                        6.25052293837658e-04,
                        1.80681574437331e-03,
                        3.16157856283395e-03,
                        3.84346302890074e-03,
                        2.07197272649255e-03,
                        -3.71260787541076e-03,
                        -1.30516605652246e-02,
                        -2.21708979278681e-02,
                        -2.43317276577893e-02,
                        -1.22157084664883e-02,
                        1.84502152762094e-02,
                        6.57442660025131e-02,
                        1.20670511095905e-01,
                        1.69419009540448e-01,
                        1.98119384105892e-01,
                        1.98119384105892e-01,
                        1.69419009540448e-01,
                        1.20670511095905e-01,
                        6.57442660025131e-02,
                        1.84502152762094e-02,
                        -1.22157084664883e-02,
                        -2.43317276577893e-02,
                        -2.21708979278681e-02,
                        -1.30516605652246e-02,
                        -3.71260787541076e-03,
                        2.07197272649255e-03,
                        3.84346302890073e-03,
                        3.16157856283395e-03,
                        1.80681574437331e-03,
                        6.25052293837658e-04
                }
        );
    }

    /**
     * 29th order (30 taps) Fir1 (Octave) lowpass filter that lets 6th-Nyquist pass (&#x03C9;=1/6).
     *
     * @return filter
     */
    private static FIRFilter createFir1_29thOrderLowpassCutoff6th() {
        return new FIRFilter(
                new double[]{
                        1.74817503022625e-03,
                        1.51305867163446e-03,
                        7.21859296943263e-04,
                        -1.47051278883614e-03,
                        -5.59370178625812e-03,
                        -1.10502181970320e-02,
                        -1.56357824667153e-02,
                        -1.57696241385074e-02,
                        -7.52697086254368e-03,
                        1.18108982446199e-02,
                        4.22713426542169e-02,
                        8.04221043000035e-02,
                        1.19759324419102e-01,
                        1.52264297529731e-01,
                        1.70670956055283e-01,
                        1.70670956055283e-01,
                        1.52264297529731e-01,
                        1.19759324419102e-01,
                        8.04221043000035e-02,
                        4.22713426542169e-02,
                        1.18108982446199e-02,
                        -7.52697086254367e-03,
                        -1.57696241385074e-02,
                        -1.56357824667153e-02,
                        -1.10502181970319e-02,
                        -5.59370178625812e-03,
                        -1.47051278883614e-03,
                        7.21859296943264e-04,
                        1.51305867163446e-03,
                        1.74817503022625e-03,
                }
        );
    }

    /**
     * 29th order (30 taps) Fir1 (Octave) lowpass filter that lets 7th-Nyquist pass (&#x03C9;=1/7).
     *
     * @return filter
     */
    private static FIRFilter createFir1_29thOrderLowpassCutoff7th() {
        return new FIRFilter(
                new double[]{
                        3.38765915155771e-04,
                        -6.06032058776432e-04,
                        -2.19634901862048e-03,
                        -4.74911745470822e-03,
                        -7.91185251886683e-03,
                        -1.04097224047845e-02,
                        -1.01488404577784e-02,
                        -4.71469506967458e-03,
                        7.84795424175555e-03,
                        2.82136395149552e-02,
                        5.52026291907091e-02,
                        8.56926164235778e-02,
                        1.15110876912909e-01,
                        1.38415151191537e-01,
                        1.51303445075143e-01,
                        1.51303445075143e-01,
                        1.38415151191537e-01,
                        1.15110876912909e-01,
                        8.56926164235778e-02,
                        5.52026291907091e-02,
                        2.82136395149552e-02,
                        7.84795424175555e-03,
                        -4.71469506967459e-03,
                        -1.01488404577784e-02,
                        -1.04097224047844e-02,
                        -7.91185251886682e-03,
                        -4.74911745470822e-03,
                        -2.19634901862048e-03,
                        -6.06032058776432e-04,
                        3.38765915155771e-04,}
        );
    }

    /**
     * 29th order (30 taps) Fir1 (Octave) lowpass filter that lets 8th-Nyquist pass (&#x03C9;=1/8).
     *
     * @return filter
     */
    private static FIRFilter createFir1_29thOrderLowpassCutoff8th() {
        return new FIRFilter(
                new double[]{
                        -1.16008074058640e-03,
                        -2.03844238252229e-03,
                        -3.43753338351021e-03,
                        -5.22375046285788e-03,
                        -6.66093931984797e-03,
                        -6.44286213484060e-03,
                        -2.95152670639897e-03,
                        5.30684550801002e-03,
                        1.92004049666275e-02,
                        3.85509693668942e-02,
                        6.19314115125609e-02,
                        8.67670420912017e-02,
                        1.09746001430178e-01,
                        1.27456292790466e-01,
                        1.37098976053053e-01,
                        1.37098976053053e-01,
                        1.27456292790466e-01,
                        1.09746001430178e-01,
                        8.67670420912017e-02,
                        6.19314115125609e-02,
                        3.85509693668942e-02,
                        1.92004049666275e-02,
                        5.30684550801003e-03,
                        -2.95152670639897e-03,
                        -6.44286213484060e-03,
                        -6.66093931984797e-03,
                        -5.22375046285788e-03,
                        -3.43753338351021e-03,
                        -2.03844238252229e-03,
                        -1.16008074058640e-03,
                }
        );
    }


    /**
     * 29th order (30 taps) Fir1 (Octave) lowpass filter that lets 9th-Nyquist pass (&#x03C9;=1/9).
     *
     * @return filter
     */
    private static FIRFilter createFir1_29thOrderLowpassCutoff9th() {
        return new FIRFilter(
                new double[]{
                        -1.93231722275351e-03,
                        -2.46596726411797e-03,
                        -3.33172036258063e-03,
                        -4.12197067794713e-03,
                        -3.99602094866851e-03,
                        -1.81707864244671e-03,
                        3.58858765099438e-03,
                        1.31068238295347e-02,
                        2.70252560918722e-02,
                        4.48212943153840e-02,
                        6.51139483964651e-02,
                        8.58082658961385e-02,
                        1.04415031060388e-01,
                        1.18484309836151e-01,
                        1.26060896281202e-01,
                        1.26060896281202e-01,
                        1.18484309836151e-01,
                        1.04415031060388e-01,
                        8.58082658961385e-02,
                        6.51139483964651e-02,
                        4.48212943153840e-02,
                        2.70252560918722e-02,
                        1.31068238295348e-02,
                        3.58858765099438e-03,
                        -1.81707864244671e-03,
                        -3.99602094866851e-03,
                        -4.12197067794713e-03,
                        -3.33172036258063e-03,
                        -2.46596726411797e-03,
                        -1.93231722275351e-03,}
        );
    }


    /**
     * 29th order (30 taps) Fir1 (Octave) lowpass filter that lets 10th-Nyquist pass (&#x03C9;=1/10).
     *
     * @return filter
     */
    private static FIRFilter createFir1_29thOrderLowpassCutoff10th() {
        return new FIRFilter(
                new double[]{
                        -2.04736255720791e-03,
                        -2.21894363276270e-03,
                        -2.52256082926747e-03,
                        -2.41737448885047e-03,
                        -1.08819958075000e-03,
                        2.39606503693533e-03,
                        8.86497253920739e-03,
                        1.88162117275586e-02,
                        3.22254836211222e-02,
                        4.84443956325368e-02,
                        6.62175684184414e-02,
                        8.38255040713128e-02,
                        9.93324818008713e-02,
                        1.10894979486115e-01,
                        1.17071360890398e-01,
                        1.17071360890398e-01,
                        1.10894979486115e-01,
                        9.93324818008713e-02,
                        8.38255040713128e-02,
                        6.62175684184414e-02,
                        4.84443956325368e-02,
                        3.22254836211222e-02,
                        1.88162117275586e-02,
                        8.86497253920740e-03,
                        2.39606503693533e-03,
                        -1.08819958075000e-03,
                        -2.41737448885047e-03,
                        -2.52256082926747e-03,
                        -2.21894363276270e-03,
                        -2.04736255720791e-03,
                }
        );
    }

}

