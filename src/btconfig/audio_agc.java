//MIT License
//
//Copyright (c) 2020 bluetailtech
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

package btconfig;

public class audio_agc {

  //audio agc related
  final static int AUD_AGC_LEN=32; //must be power of two
  float[] audio_max = new float[AUD_AGC_LEN];
  int audio_max_idx;
  float aout_gain=1.0f;
  float aud_agc_max;
  float gainfactor;
  float gaindelta;
  float maxbuf;

  public audio_agc() {
  }

  //////////////////////////////////////////////////////////////////////////////////
  // audio agc
  //////////////////////////////////////////////////////////////////////////////////
  int[] update_gain_s16(int[] audio_in, int len, float target, float log_mult, float rate) {

    float[] tmp_buffer_f = new float[len];

    for(int i=0;i<len;i++) {
      tmp_buffer_f[i] = ((float) audio_in[i]) / 10.0f; 
    }

    int[] audio_out = update_gain_f32(tmp_buffer_f,len, target, log_mult, rate);

    return audio_out;
  }
  //////////////////////////////////////////////////////////////////////////////////
  // audio agc
  //////////////////////////////////////////////////////////////////////////////////
  int[] update_gain_f32(float[] audio, int len, float target, float log_mult, float rate) {

    audio_max_idx &= (AUD_AGC_LEN-1);

    // detect max level
    aud_agc_max = 0;
    for (int n = 0; n < len; n++) {
      float in = audio[n];
      if(in<0) in*=-1.0f;
      if(in > aud_agc_max) aud_agc_max = in; 
    }
    audio_max[audio_max_idx++] = aud_agc_max;
    audio_max_idx &= (AUD_AGC_LEN-1);

    aud_agc_max = 0;
    // lookup max history
    for(int i = 0; i < AUD_AGC_LEN; i++) {
      if(audio_max[i] > aud_agc_max) aud_agc_max = audio_max[i];
    }

    // determine optimal gain level
    if (aud_agc_max > 0.0f) {
      gainfactor = (target / aud_agc_max);
    } else {
      gainfactor = 40.0f+0.1f; 
    }
    if (gainfactor < aout_gain) {
      aout_gain = gainfactor;
      gaindelta = 0.0f;
    } else {
      if (gainfactor > 40.0f) {
          //gainfactor = (float) java.lang.Math.log10(gainfactor+1.0f)*29.42f;
          gainfactor = 40.0f; 
      }
      gaindelta = gainfactor - aout_gain;
      if (gaindelta > (rate * aout_gain)) {
          gaindelta = (rate * aout_gain);
      }
    }

    // adjust output gain
    aout_gain += gaindelta;
    for(int n = 0; n < len; n++) {
      audio[n] *= aout_gain;
    }

    //System.out.println("aout_gain "+aout_gain);

    int[] audio_out = new int[len];
    for(int i=0;i<len;i++) {
      audio_out[i] = (int) audio[i];
    }

    return audio_out;
  }
}
