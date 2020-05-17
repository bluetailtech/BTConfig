/*
 * =================================================
 * Copyright 2014 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package pcmsampledsp;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Converter.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public interface Converter<A> {

    /**
     * Reads samples from the byte buffer and writes the resulting values to the
     * provided array.
     *
     * @param source byte buffer
     * @param array  value array (output)
     * @throws java.io.IOException if the conversion fails
     */
    void decode(final ByteBuffer source, final A array) throws IOException;

    /**
     * Writes the given array to the target byte buffer using the given
     * byte order, bytes per sample etc.
     *
     * @param array  value array
     * @param target target byte buffer
     */
    void encode(final A array, final ByteBuffer target);

    /**
     * Writes the given array to the target byte buffer using the given
     * byte order, bytes per sample etc.
     *
     * @param array value array
     * @param length number of values from array to actually convert
     * @param target target byte buffer
     */
    void encode(final A array, final int length, final ByteBuffer target);
}
