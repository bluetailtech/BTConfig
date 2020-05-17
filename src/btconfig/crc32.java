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

public class crc32
{

  private static final int CRC32_POLYNOMIAL = 0xEDB88320;

  ////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////
  private static int CRC32Value(int i)
  {
    short j;
    int ulCRC;
    ulCRC = i;

    for (j = 8; j > 0; j--) {
      if ((ulCRC & 1) == 1)
        ulCRC = (ulCRC >>> 1) ^ CRC32_POLYNOMIAL;
      else
        ulCRC >>>= 1;
    }
    return ulCRC;
  }

  ///////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////////
  public static int crc32_range(byte[] buffer, int length)
  {
    int ulTemp1;
    int ulTemp2;
    int ulCRC = 0;

    for(int i = 0; i < length; i++) {
      ulTemp1 = ( ulCRC >>> 8 ) & 0x00FFFFFF;
      ulTemp2 = CRC32Value( ((int) ulCRC ^ buffer[i] ) & 0xff );
      ulCRC = ulTemp1 ^ ulTemp2;
    }
    return ulCRC;
  }

  public static void main( String[] args)
  {

    byte[] test_buffer = new byte[128 * 1024];
    for( int i=0; i< 128 * 1024; i++) {
      test_buffer[i] = (byte) 0xff;
    }

    int crc = crc32_range(test_buffer, 128 * 1024);

    System.out.println( String.format(" crc: 0x%08x", crc) );
  }

}
