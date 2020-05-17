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
import java.io.IOException;

/**
 * Stream that converts a mono stream into a stereo stream by duplicating the mono channel.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see StereofyConversionProvider
 */
public class StereofyAudioInputStream extends AudioInputStream {

    /**
     * The position where a mark was set.
     */
    private long markpos;

    /**
     * When the underlying stream could only return
     * a non-integral number of frames, store
     * the remainder in a temporary buffer
     */
    private byte[] pushBackBuffer = null;

    /**
     * number of valid bytes in the pushBackBuffer
     */
    private int pushBackLen = 0;

    /**
     * MarkBuffer at mark position
     */
    private byte[] markPushBackBuffer = null;

    /**
     * number of valid bytes in the markPushBackBuffer
     */
    private int markPushBackLen = 0;
    private AudioInputStream stream;

    public StereofyAudioInputStream(final AudioInputStream sourceStream, final AudioFormat audioFormat) {
        super(sourceStream, audioFormat, sourceStream.getFrameLength());
        this.stream = sourceStream;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        // make sure we don't read fractions of a frame.
        if ((len % frameSize) != 0) {
            len -= (len % frameSize);
            if (len == 0) {
                return 0;
            }
        }

        if (frameLength != AudioSystem.NOT_SPECIFIED) {
            if (framePos >= frameLength) {
                return -1;
            } else {

                // don't try to read beyond our own set length in frames
                if ((len / frameSize) > (frameLength - framePos)) {
                    len = (int) (frameLength - framePos) * frameSize;
                }
            }
        }

        int bytesRead = 0;
        int thisOff = off;

        // if we've bytes left from last call to read(),
        // use them first
        if (pushBackLen > 0 && len >= pushBackLen) {
            System.arraycopy(pushBackBuffer, 0,
                    b, off, pushBackLen);
            thisOff += pushBackLen;
            len -= pushBackLen;
            bytesRead += pushBackLen;
            pushBackLen = 0;
        }

        final byte[] mono = new byte[len/2];
        int monoBytesRead = stream.read(mono);
        if (monoBytesRead == -1) {
            return -1;
        }
        final int bytesPerSample = format.getSampleSizeInBits() / 8;
        for (int i=0; i<monoBytesRead/bytesPerSample; i++) {
            System.arraycopy(mono, i*bytesPerSample, b, thisOff+i*bytesPerSample*2, bytesPerSample);
            System.arraycopy(mono, i*bytesPerSample, b, thisOff+bytesPerSample+i*bytesPerSample*2, bytesPerSample);
        }

        int thisBytesRead = monoBytesRead * 2;
        if (thisBytesRead > 0) {
            bytesRead += thisBytesRead;
        }
        if (bytesRead > 0) {
            pushBackLen = bytesRead % frameSize;
            if (pushBackLen > 0) {
                // copy everything we got from the beginning of the frame
                // to our pushback buffer
                if (pushBackBuffer == null) {
                    pushBackBuffer = new byte[frameSize];
                }
                System.arraycopy(b, off + bytesRead - pushBackLen,
                        pushBackBuffer, 0, pushBackLen);
                bytesRead -= pushBackLen;
            }
            // make sure to update our framePos
            framePos += bytesRead / frameSize;
        }
        return bytesRead;
    }

    /**
     * Marks the current position in this audio input stream.
     *
     * @param readlimit the maximum number of bytes that can be read before
     *                  the mark position becomes invalid.
     * @see #reset
     * @see #markSupported
     */

    public void mark(int readlimit) {

        stream.mark(readlimit/2);
        if (markSupported()) {
            markpos = framePos;
            // remember the pushback buffer
            markPushBackLen = pushBackLen;
            if (markPushBackLen > 0) {
                if (markPushBackBuffer == null) {
                    markPushBackBuffer = new byte[frameSize];
                }
                System.arraycopy(pushBackBuffer, 0, markPushBackBuffer, 0, markPushBackLen);
            }
        }
    }


    /**
     * Repositions this audio input stream to the position it had at the time its
     * <code>mark</code> method was last invoked.
     *
     * @throws IOException if an input or output error occurs.
     * @see #mark
     * @see #markSupported
     */
    public void reset() throws IOException {

        stream.reset();
        framePos = markpos;
        // re-create the pushback buffer
        pushBackLen = markPushBackLen;
        if (pushBackLen > 0) {
            if (pushBackBuffer == null) {
                pushBackBuffer = new byte[frameSize - 1];
            }
            System.arraycopy(markPushBackBuffer, 0, pushBackBuffer, 0, pushBackLen);
        }
    }

    @Override
    public int available() throws IOException {
        return super.available()*2;
    }

    @Override
    public long skip(final long n) throws IOException {
        return super.skip(n/2);
    }
}
