// P25 TDMA Decoder (C) Copyright 2013, 2014 Max H. Parke KA1RBI
// Copyright 2017 Graham J. Norbury (modularization rewrite)
// 
// This file is part of OP25
// 
// OP25 is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 3, or (at your option)
// any later version.
// 
// OP25 is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
// or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
// License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with OP25; see the file COPYING. If not, write to the Free
// Software Foundation, Inc., 51 Franklin Street, Boston, MA
// 02110-1301, USA.


///////////////////////////////////////////////////
// Conversion to 'C' - BlueTail Technologies
///////////////////////////////////////////////////

#include <stdint.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#include "p2_duid.h"
#include "p2_tdma.h"
#include "p2_sync.h"
//#include "p25p2_const.h"
#include "Golay.h"

#define ARM_MATH_CM7 1
#include "arm_common_tables.h"
#include "arm_math.h"
#define SINF arm_sin_f32 

#define BURST_SIZE 180
#define SUPERFRAME_SIZE (12*BURST_SIZE)
static uint8_t tdma_xormask[SUPERFRAME_SIZE];
static int d_slotid=0;
static int burst_id;
static uint8_t ESS_A[28];
static uint8_t ESS_B[16];
static uint8_t ess_keyid;
static uint16_t ess_algid;
static uint8_t ess_mi[9] = {0};
static int packets;

static char ambe_fr[4][24];
static char ambe_d[49];
static int errors=0;
static int errors2=0;

static int tone_mod;
static int tone_idx;
static int sin_gen;

#if 0
/////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
unsigned int Golay23_CorrectAndGetErrCount( unsigned int *block )
{
  unsigned int i, errs = 0, in = *block;

  Golay23_Correct( block );

  in >>= 11;
  in ^= *block;
  for( i = 0; i < 12; i++ ) {
    if( ( in >> i ) & 1 ) {
      errs++;
    }
  }

  return ( errs );
};
#endif

/////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
int encrypted(void) { 
  return (ess_algid != 0x80); 
}
/////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
//int track_vb(int burst_type) { 
 // return burst_id = (burst_type == 0) ? (++burst_id % 5) : 4; 
//}

#if 0
/////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
static uint16_t crc12(const uint8_t bits[], unsigned int len) {
	uint16_t crc=0;
	static const unsigned int K = 12;
	static const uint8_t poly[K+1] = {1,1,0,0,0,1,0,0,1,0,1,1,1}; // p25 p2 crc 12 poly
	uint8_t buf[256];
	if (len+K > sizeof(buf)) {
		//fprintf (stderr, "crc12: buffer length %u exceeds maximum %lu\n", len+K, sizeof(buf));
		return 0;
	}
	memset (buf, 0, sizeof(buf));
	for (int i=0; i<len; i++){
		buf[i] = bits[i];
	}
	for (int i=0; i<len; i++)
		if (buf[i])
			for (int j=0; j<K+1; j++)
				buf[i+j] ^= poly[j];
	for (int i=0; i<K; i++){
		crc = (crc << 1) + buf[len + i];
	}
	return crc ^ 0xfff;
}

////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////
static int crc12_ok(const uint8_t bits[], unsigned int len) {
	uint16_t crc = 0;
	for (int i=0; i < 12; i++) {
		crc = (crc << 1) + bits[len+i];
	}
	return (crc == crc12(bits,len));
}
#endif

////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////
#if 0
static const uint8_t mac_msg_len[256] = {
	 0,  7,  8,  7,  0, 16,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
	 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
	 0, 14, 15,  0,  0, 15,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
	 5,  7,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
	 9,  7,  9,  0,  9,  8,  9,  0,  0,  0,  9,  0,  0,  0,  0,  0, 
	 0,  0,  0,  0,  9,  7,  0,  0,  0,  0,  7,  0,  0,  8, 14,  7, 
	 9,  9,  0,  0,  9,  0,  0,  9,  0,  0,  7,  0,  0,  7,  0,  0, 
	 0,  0,  0,  9,  9,  9,  0,  0,  9,  9,  9, 11,  9,  9,  0,  0, 
	 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
	 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
	 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
	 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
	11,  0,  0,  8, 15, 12, 15,  0,  0,  0,  0,  0,  0,  0,  0,  0, 
	 0,  0,  0,  0,  0,  0,  9,  0,  0,  0, 11,  0,  0,  0,  0, 11, 
	 0,  0,  0,  0,  0,  0,  0,  0,  0,  8, 11,  0,  0,  0,  0,  0, 
	 0,  0,  0,  0,  0,  0,  0,  0,  0,  0, 11, 13, 11,  0,  0,  0 };
#endif

//////////////////////////////////////////////////////////////////////////////////////////////////////////
//  init
//////////////////////////////////////////////////////////////////////////////////////////////////////////
#if 0
p25p2_tdma::p25p2_tdma(const op25_audio& udp, int slotid, int debug, bool do_msgq, gr::msg_queue::sptr queue, std::deque<int16_t> &qptr, bool do_audio_output) :	// constructor
        op25audio(udp),
	write_bufp(0),
	tdma_xormask(new uint8_t[SUPERFRAME_SIZE]),
	symbols_received(0),
	packets(0),
	d_slotid(slotid),
	d_nac(0),
	d_do_msgq(do_msgq),
	d_msg_queue(queue),
	output_queue_decode(qptr),
	d_debug(debug),
	d_do_audio_output(do_audio_output),
        burst_id(-1),
        ESS_A(28,0),
        ESS_B(16,0),
        ess_algid(0x80),
        ess_keyid(0),
	p2framer()
{
	assert (slotid == 0 || slotid == 1);
	mbe_initMbeParms (&cur_mp, &prev_mp, &enh_mp);
}
#endif


///////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////
void p2_set_slotid(int slotid) {
	d_slotid = slotid;

    tone_mod=0;
    tone_idx=0;
    sin_gen=0;
}

///////////////////////////////////////////////////////////////////
// p = is vector of dibit mask
// 4320 total mask bits = (SUPERFRAME_SIZE * 2)
///////////////////////////////////////////////////////////////////
void p2_set_xormask(const char*p) {
  int i;
	for (i=0; i<SUPERFRAME_SIZE; i++) { //number of dibits
		tdma_xormask[i] = p[i] & 3;
  }
}

#if 0
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
int process_mac_pdu(const uint8_t byte_buf[], const unsigned int len) {

	unsigned int opcode = (byte_buf[0] >> 5) & 0x7;
	unsigned int offset = (byte_buf[0] >> 2) & 0x7;

        #if 0
        if (d_debug >= 10) {
           fprintf(stderr, "%s process_mac_pdu: opcode %d len %d\n", logts.get(), opcode, len);
        }
        #endif

        switch (opcode)
        {
                case 1: // MAC_PTT
                        handle_mac_ptt(byte_buf, len);
                        break;

                case 2: // MAC_END_PTT
                        handle_mac_end_ptt(byte_buf, len);
                        break;

                case 3: // MAC_IDLE
                        handle_mac_idle(byte_buf, len);
                        break;

                case 4: // MAC_ACTIVE
                        handle_mac_active(byte_buf, len);
                        break;

                case 6: // MAC_HANGTIME
                        handle_mac_hangtime(byte_buf, len);

                        //TODO: empty audio buffer here
                        //op25audio.send_audio_flag(op25_audio::DRAIN);
                        break;
        }
	// maps sacch opcodes into phase I duid values 
	static const int opcode_map[8] = {3, 5, 15, 15, 5, 3, 3, 3};
	return opcode_map[opcode];
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
void handle_mac_ptt(const uint8_t byte_buf[], const unsigned int len) {

        uint32_t srcaddr = (byte_buf[13] << 16) + (byte_buf[14] << 8) + byte_buf[15];
        uint16_t grpaddr = (byte_buf[16] << 8) + byte_buf[17];
        //std::string s = "{\"srcaddr\" : " + std::to_string(srcaddr) + ", \"grpaddr\": " + std::to_string(grpaddr) + ", \"nac\" : " + std::to_string(d_nac) + "}";
        //send_msg(s, -3);

        //if (d_debug >= 10) {
         //       fprintf(stderr, "%s MAC_PTT: srcaddr=%u, grpaddr=%u", logts.get(), srcaddr, grpaddr);
        //}
        for (int i = 0; i < 9; i++) {
                ess_mi[i] = byte_buf[i+1];
        }
        ess_algid = byte_buf[10];
        ess_keyid = (byte_buf[11] << 8) + byte_buf[12];
        //if (d_debug >= 10) {
         //       fprintf(stderr, ", algid=%x, keyid=%x, mi=", ess_algid, ess_keyid);
          //      for (int i = 0; i < 9; i++) {
           //             fprintf(stderr,"%02x ", ess_mi[i]);
            //    }
        //}
	//s = "{\"nac\" : " + std::to_string(d_nac) + ", \"algid\" : " + std::to_string(ess_algid) + ", \"alg\" : \"" + lookup(ess_algid, ALGIDS, ALGIDS_SZ) + "\", \"keyid\": " + std::to_string(ess_keyid) + "}";
	//send_msg(s, -3);

        //if (d_debug >= 10) {
         //       fprintf(stderr, "\n");
        //}

        reset_vb();
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
void p25p2_tdma::handle_mac_end_ptt(const uint8_t byte_buf[], const unsigned int len) 
{
        uint16_t colorcd = ((byte_buf[1] & 0x0f) << 8) + byte_buf[2];
        uint32_t srcaddr = (byte_buf[13] << 16) + (byte_buf[14] << 8) + byte_buf[15];
        uint16_t grpaddr = (byte_buf[16] << 8) + byte_buf[17];

        //if (d_debug >= 10)
         //       fprintf(stderr, "%s MAC_END_PTT: colorcd=0x%03x, srcaddr=%u, grpaddr=%u\n", logts.get(), colorcd, srcaddr, grpaddr);

        //std::string s = "{\"srcaddr\" : " + std::to_string(srcaddr) + ", \"grpaddr\": " + std::to_string(grpaddr) + "}";
        //send_msg(s, -3);	// can cause data display issues if this message is processed after the DUID15

        //TODO:  drain audio here
        //op25audio.send_audio_flag(op25_audio::DRAIN);
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
void p25p2_tdma::handle_mac_idle(const uint8_t byte_buf[], const unsigned int len) 
{
        //if (d_debug >= 10)
         //       fprintf(stderr, "%s MAC_IDLE: ", logts.get());

        decode_mac_msg(byte_buf, len);
        //op25audio.send_audio_flag(op25_audio::DRAIN);

        //TODO:  drain audio here
        //if (d_debug >= 10)
         //       fprintf(stderr, "\n");
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
void p25p2_tdma::handle_mac_active(const uint8_t byte_buf[], const unsigned int len) 
{
        //if (d_debug >= 10)
         //       fprintf(stderr, "%s MAC_ACTIVE: ", logts.get());

        decode_mac_msg(byte_buf, len);

        //if (d_debug >= 10)
         //       fprintf(stderr, "\n");
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
void p25p2_tdma::handle_mac_hangtime(const uint8_t byte_buf[], const unsigned int len) 
{
        //if (d_debug >= 10)
        //        fprintf(stderr, "%s MAC_HANGTIME: ", logts.get());

        decode_mac_msg(byte_buf, len);
        //op25audio.send_audio_flag(op25_audio::DRAIN);

        //TODO:  drain audio here
        //if (d_debug >= 10)
         //       fprintf(stderr, "\n");
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
void decode_mac_msg(const uint8_t byte_buf[], const unsigned int len) {

	//std::string s;
	uint8_t b1b2, cfva, mco, lra, rfss, site_id, ssc, svcopts[3], msg_ptr, msg_len;
  uint16_t chan[3], ch_t[2], ch_r[2], colorcd, grpaddr[3], sys_id;
  uint32_t srcaddr, wacn_id;

	for (msg_ptr = 1; msg_ptr < len; ) {

    b1b2 = byte_buf[msg_ptr] >> 6;
    mco  = byte_buf[msg_ptr] & 0x3f;
		msg_len = mac_msg_len[(b1b2 << 6) + mco];

		//if (d_debug >= 10)
     //          		fprintf(stderr, "mco=%01x/%02x", b1b2, mco);

		switch(byte_buf[msg_ptr])
                {
			case 0x00: // Null message
				break;
			case 0x40: // Group Voice Channel Grant Abbreviated
				svcopts[0] = (byte_buf[msg_ptr+1]     )                      ;
				chan[0]    = (byte_buf[msg_ptr+2] << 8) + byte_buf[msg_ptr+3];
				grpaddr[0] = (byte_buf[msg_ptr+4] << 8) + byte_buf[msg_ptr+5];
				srcaddr    = (byte_buf[msg_ptr+6] << 16) + (byte_buf[msg_ptr+7] << 8) + byte_buf[msg_ptr+8];
				//if (d_debug >= 10)
				//	fprintf(stderr, ", svcopts=0x%02x, ch=%u, grpaddr=%u, srcaddr=%u", svcopts[0], chan[0], grpaddr[0], srcaddr);
				break;
			case 0xc0: // Group Voice Channel Grant Extended
				svcopts[0] = (byte_buf[msg_ptr+1]     )                      ;
				ch_t[0]    = (byte_buf[msg_ptr+2] << 8) + byte_buf[msg_ptr+3];
				ch_r[0]    = (byte_buf[msg_ptr+4] << 8) + byte_buf[msg_ptr+5];
				grpaddr[0] = (byte_buf[msg_ptr+6] << 8) + byte_buf[msg_ptr+7];
				srcaddr    = (byte_buf[msg_ptr+8] << 16) + (byte_buf[msg_ptr+9] << 8) + byte_buf[msg_ptr+10];
				//if (d_debug >= 10)
				//	fprintf(stderr, ", svcopts=0x%02x, ch_t=%u, ch_t=%u, grpaddr=%u, srcaddr=%u", svcopts[0], ch_t[0], ch_r[0], grpaddr[0], srcaddr);
				break;

      case 0x01: // Group Voice Channel User Message Abbreviated
                                grpaddr[0] = (byte_buf[msg_ptr+2] << 8) + byte_buf[msg_ptr+3];
                                srcaddr    = (byte_buf[msg_ptr+4] << 16) + (byte_buf[msg_ptr+5] << 8) + byte_buf[msg_ptr+6];
                                //if (d_debug >= 10)
                              	 //       fprintf(stderr, ", grpaddr=%u, srcaddr=%u", grpaddr[0], srcaddr);
                                //s = "{\"srcaddr\" : " + std::to_string(srcaddr) + ", \"grpaddr\": " + std::to_string(grpaddr[0]) + ", \"nac\" : " + std::to_string(d_nac) + "}";
				//send_msg(s, -3);
      break;

			case 0x42: // Group Voice Channel Grant Update
				chan[0]    = (byte_buf[msg_ptr+1] << 8) + byte_buf[msg_ptr+2];
				grpaddr[0] = (byte_buf[msg_ptr+3] << 8) + byte_buf[msg_ptr+4];
				chan[1]    = (byte_buf[msg_ptr+5] << 8) + byte_buf[msg_ptr+6];
				grpaddr[1] = (byte_buf[msg_ptr+7] << 8) + byte_buf[msg_ptr+8];
				//if (d_debug >= 10)
				//	fprintf(stderr, ", ch_1=%u, grpaddr1=%u, ch_2=%u, grpaddr2=%u", chan[0], grpaddr[0], chan[1], grpaddr[1]);
				break;
			case 0xc3: // Group Voice Channel Grant Update Explicit
				svcopts[0] = (byte_buf[msg_ptr+1]     )                      ;
				ch_t[0]    = (byte_buf[msg_ptr+2] << 8) + byte_buf[msg_ptr+3];
				ch_r[0]    = (byte_buf[msg_ptr+4] << 8) + byte_buf[msg_ptr+5];
				grpaddr[0] = (byte_buf[msg_ptr+6] << 8) + byte_buf[msg_ptr+7];
				//if (d_debug >= 10)
				//	fprintf(stderr, ", svcopts=0x%02x, ch_t=%u, ch_r=%u, grpaddr=%u", svcopts[0], ch_t[0], ch_r[0], grpaddr[0]);
				break;
			case 0x05: // Group Voice Channel Grant Update Multiple
				svcopts[0] = (byte_buf[msg_ptr+ 1]     )                       ;
				chan[0]    = (byte_buf[msg_ptr+ 2] << 8) + byte_buf[msg_ptr+ 3];
				grpaddr[0] = (byte_buf[msg_ptr+ 4] << 8) + byte_buf[msg_ptr+ 5];
				svcopts[1] = (byte_buf[msg_ptr+ 6]     )                       ;
				chan[1]    = (byte_buf[msg_ptr+ 7] << 8) + byte_buf[msg_ptr+ 8];
				grpaddr[1] = (byte_buf[msg_ptr+ 9] << 8) + byte_buf[msg_ptr+10];
				svcopts[2] = (byte_buf[msg_ptr+11]     )                       ;
				chan[2]    = (byte_buf[msg_ptr+12] << 8) + byte_buf[msg_ptr+13];
				grpaddr[2] = (byte_buf[msg_ptr+14] << 8) + byte_buf[msg_ptr+15];
				//if (d_debug >= 10)
				//	fprintf(stderr, ", svcopt1=0x%02x, ch_1=%u, grpaddr1=%u, svcopt2=0x%02x, ch_2=%u, grpaddr2=%u, svcopt3=0x%02x, ch_3=%u, grpaddr3=%u", svcopts[0], chan[0], grpaddr[0], svcopts[1], chan[1], grpaddr[1], svcopts[2], chan[2], grpaddr[2]);
				break;
			case 0x25: // Group Voice Channel Grant Update Multiple Explicit
				svcopts[0] = (byte_buf[msg_ptr+ 1]     )                       ;
				ch_t[0]    = (byte_buf[msg_ptr+ 2] << 8) + byte_buf[msg_ptr+ 3];
				ch_r[0]    = (byte_buf[msg_ptr+ 4] << 8) + byte_buf[msg_ptr+ 5];
				grpaddr[0] = (byte_buf[msg_ptr+ 6] << 8) + byte_buf[msg_ptr+ 7];
				svcopts[1] = (byte_buf[msg_ptr+ 8]     )                       ;
				ch_t[1]    = (byte_buf[msg_ptr+ 9] << 8) + byte_buf[msg_ptr+10];
				ch_r[1]    = (byte_buf[msg_ptr+11] << 8) + byte_buf[msg_ptr+12];
				grpaddr[1] = (byte_buf[msg_ptr+13] << 8) + byte_buf[msg_ptr+14];
				//if (d_debug >= 10)
				//	fprintf(stderr, ", svcopt1=0x%02x, ch_t1=%u, ch_r1=%u, grpaddr1=%u, svcopt2=0x%02x, ch_t2=%u, ch_r2=%u, grpaddr2=%u", svcopts[0], ch_t[0], ch_r[0], grpaddr[0], svcopts[1], ch_t[1], ch_r[1], grpaddr[1]);
				break;
			case 0x7b: // Network Status Broadcast Abbreviated
				lra     =   byte_buf[msg_ptr+1];
				wacn_id =  (byte_buf[msg_ptr+2] << 12) + (byte_buf[msg_ptr+3] << 4) + (byte_buf[msg_ptr+4] >> 4);
				sys_id  = ((byte_buf[msg_ptr+4] & 0x0f) << 8) + byte_buf[msg_ptr+5];
				chan[0] =  (byte_buf[msg_ptr+6] << 8) + byte_buf[msg_ptr+7];
				ssc     =   byte_buf[msg_ptr+8];
				colorcd = ((byte_buf[msg_ptr+9] & 0x0f) << 8) + byte_buf[msg_ptr+10];
				//if (d_debug >= 10)
				//	fprintf(stderr, ", lra=0x%02x, wacn_id=0x%05x, sys_id=0x%03x, ch=%u, ssc=0x%02x, colorcd=%03x", lra, wacn_id, sys_id, chan[0], ssc, colorcd);
				break;
			case 0x7c: // Adjacent Status Broadcast Abbreviated
				lra     =   byte_buf[msg_ptr+1];
				cfva    =  (byte_buf[msg_ptr+2] >> 4);
				sys_id  = ((byte_buf[msg_ptr+2] & 0x0f) << 8) + byte_buf[msg_ptr+3];
				rfss    =   byte_buf[msg_ptr+4];
				site_id =   byte_buf[msg_ptr+5];
				chan[0] =  (byte_buf[msg_ptr+6] << 8) + byte_buf[msg_ptr+7];
				ssc     =   byte_buf[msg_ptr+8];
				//if (d_debug >= 10)
				//	fprintf(stderr, ", lra=0x%02x, cfva=0x%01x, sys_id=0x%03x, rfss=%u, site=%u, ch=%u, ssc=0x%02x", lra, cfva, sys_id, rfss, site_id, chan[0], ssc);
				break;
			case 0xfc: // Adjacent Status Broadcast Extended
				break;
			case 0xfb: // Network Status Broadcast Extended
				colorcd = ((byte_buf[msg_ptr+11] & 0x0f) << 8) + byte_buf[msg_ptr+12];
				//if (d_debug >= 10)
				//	fprintf(stderr, ", colorcd=%03x", colorcd);
				break;
               	}
		msg_ptr = (msg_len == 0) ? len : (msg_ptr + msg_len); // TODO: handle variable length messages
		//if ((d_debug >= 10) && (msg_ptr < len))
		//	fprintf(stderr,", ");
	}
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
int handle_acch_frame(const uint8_t dibits[], int fast) {

	int i, j, rc;
	uint8_t bits[512];
	std::vector<uint8_t> HB(63,0);
	std::vector<int> Erasures;
	uint8_t byte_buf[32];
	unsigned int bufl=0;
	unsigned int len=0;
	if (fast) {
		for (i=11; i < 11+36; i++) {
			bits[bufl++] = (dibits[i] >> 1) & 1;
			bits[bufl++] = dibits[i] & 1;
		}
		for (i=48; i < 48+31; i++) {
			bits[bufl++] = (dibits[i] >> 1) & 1;
			bits[bufl++] = dibits[i] & 1;
		}
		for (i=100; i < 100+32; i++) {
			bits[bufl++] = (dibits[i] >> 1) & 1;
			bits[bufl++] = dibits[i] & 1;
		}
		for (i=133; i < 133+36; i++) {
			bits[bufl++] = (dibits[i] >> 1) & 1;
			bits[bufl++] = dibits[i] & 1;
		}
	} else {
		for (i=11; i < 11+36; i++) {
			bits[bufl++] = (dibits[i] >> 1) & 1;
			bits[bufl++] = dibits[i] & 1;
		}
		for (i=48; i < 48+84; i++) {
			bits[bufl++] = (dibits[i] >> 1) & 1;
			bits[bufl++] = dibits[i] & 1;
		}
		for (i=133; i < 133+36; i++) {
			bits[bufl++] = (dibits[i] >> 1) & 1;
			bits[bufl++] = dibits[i] & 1;
		}
	}

	// Reed-Solomon
	if (fast) {
		j = 9;
		len = 270;
		Erasures = {0,1,2,3,4,5,6,7,8,54,55,56,57,58,59,60,61,62};
	}
	else {
		j = 5;
		len = 312;
		Erasures = {0,1,2,3,4,57,58,59,60,61,62};
	}

	for (i = 0; i < len; i += 6) { // convert bits to hexbits
		HB[j] = (bits[i] << 5) + (bits[i+1] << 4) + (bits[i+2] << 3) + (bits[i+3] << 2) + (bits[i+4] << 1) + bits[i+5];
		j++;
	}
	rc = rs28.decode(HB, Erasures);
	if (rc < 0)
		return -1;

	if (fast) {
		j = 9;
		len = 144;
	}
	else {
		j = 5;
		len = 168;
	}
	for (i = 0; i < len; i += 6) { // convert hexbits back to bits
		bits[i]   = (HB[j] & 0x20) >> 5;
		bits[i+1] = (HB[j] & 0x10) >> 4;
		bits[i+2] = (HB[j] & 0x08) >> 3;
		bits[i+3] = (HB[j] & 0x04) >> 2;
		bits[i+4] = (HB[j] & 0x02) >> 1;
		bits[i+5] = (HB[j] & 0x01);
		j++;
	}

	rc = -1;
	if (crc12_ok(bits, len)) { // TODO: rewrite crc12 so we don't have to do so much bit manipulation
		for (int i=0; i<len/8; i++) {
			byte_buf[i] = (bits[i*8 + 0] << 7) + (bits[i*8 + 1] << 6) + (bits[i*8 + 2] << 5) + (bits[i*8 + 3] << 4) + (bits[i*8 + 4] << 3) + (bits[i*8 + 5] << 2) + (bits[i*8 + 6] << 1) + (bits[i*8 + 7] << 0);
		}
		rc = process_mac_pdu(byte_buf, len/8);
	}
	return rc;
}
#endif

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
void handle_voice_frame(const uint8_t dibits[]) {

  #if 0
    printf("\r\nhandle ambe voice frame... NOT HANDLED");
  #else
    process_ambe_arm(dibits, 0);
  #endif


  #if 0
	static const int NSAMP_OUTPUT=160;
	int b[9];
	int16_t snd;
	int K;
	int rc = -1;

	vf.process_vcw(dibits, b);
	if (b[0] < 120) // anything above 120 is an erasure or special frame
		rc = mbe_dequantizeAmbe2250Parms (&cur_mp, &prev_mp, b);
	/* FIXME: check RC */
	K = 12;
	if (cur_mp.L <= 36)
		K = int(float(cur_mp.L + 2.0) / 3.0);
	if (rc == 0)
		software_decoder.decode_tap(cur_mp.L, K, cur_mp.w0, &cur_mp.Vl[1], &cur_mp.Ml[1]);
	audio_samples *samples = software_decoder.audio();
	write_bufp = 0;
	for (int i=0; i < NSAMP_OUTPUT; i++) {
		if (samples->size() > 0) {
			snd = (int16_t)(samples->front());
			samples->pop_front();
		} else {
			snd = 0;
		}
		write_buf[write_bufp++] = snd & 0xFF ;
		write_buf[write_bufp++] = snd >> 8;
	}
	if (d_do_audio_output && (write_bufp >= 0)) { 
		op25audio.send_audio(write_buf, write_bufp);
		write_bufp = 0;
	}

	mbe_moveMbeParms (&cur_mp, &prev_mp);
	mbe_moveMbeParms (&cur_mp, &enh_mp);
  #endif
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/* returns true if in sync and slot matches current active slot d_slotid */
int handle_packet(const uint8_t dibits[]) {

	int rc = -1;
	static const int which_slot[] = {0,1,0,1,0,1,0,1,0,1,1,0};
	packets++;

  #if 1
	tdma_check_confidence(dibits);
	if (!tdma_in_sync()) {
    //printf("\r\n!tdma_in_sync");
		return -1;
  }
  #endif

	uint8_t xored_burst[BURST_SIZE - 10];

	const uint8_t* burstp = &dibits[10];
	int burst_type = duid_lookup(extract_duid(burstp));

	if (which_slot[tdma_slotid()] != d_slotid) { // active slot?
    //printf("\r\nwrong slot id");
		return -1;
  }

	for (int i=0; i<BURST_SIZE - 10; i++) {
		xored_burst[i] = burstp[i] ^ tdma_xormask[ tdma_slotid() * BURST_SIZE + i];
	}

  //if (d_debug >= 10) {
		//fprintf(stderr, "%s TDMA burst type=%d\n", logts.get(), burst_type);
	//}
  //printf("\r\n$TDMA: burst type: %d, slot %d", burst_type, which_slot[tdma_slotid()]);

  #if 0
	if (burst_type == 0 || burst_type == 6)	{       // 4V or 2V burst
                track_vb(burst_type);
                handle_4V2V_ess(&xored_burst[84]);
                if ( !encrypted() ) {
                        handle_voice_frame(&xored_burst[11]);
                        handle_voice_frame(&xored_burst[48]);
                        if (burst_type == 0) {
                                handle_voice_frame(&xored_burst[96]);
                                handle_voice_frame(&xored_burst[133]);
                        }
                }
		return -1;
	} else if (burst_type == 3) {                   // scrambled sacch
		rc = handle_acch_frame(xored_burst, 0);
	} else if (burst_type == 9) {                   // scrambled facch
		rc = handle_acch_frame(xored_burst, 1);
	} else if (burst_type == 12) {                  // unscrambled sacch
		rc = handle_acch_frame(burstp, 0);
	} else if (burst_type == 15) {                  // unscrambled facch
		rc = handle_acch_frame(burstp, 1);
	} else {
		// unsupported type duid
		return -1;
	}
	return rc;
  #endif

	if (burst_type == 0 || burst_type == 6)	{       // 4V or 2V burst

    #if 0
    track_vb(burst_type);
    #else
    //printf("\r\nburst_id %d", burst_id);
    burst_id = (burst_type == 0) ? (++burst_id % 5) : 4; 
    #endif


    #if 0
    //handle_4V2V_ess(&xored_burst[84]);
    //if ( !encrypted() ) { //have to get reed solomon working first
    #endif
      handle_voice_frame(&xored_burst[11]);
      handle_voice_frame(&xored_burst[48]);
      if (burst_type == 0) {
        handle_voice_frame(&xored_burst[96]);
        handle_voice_frame(&xored_burst[133]);
      }
    //}
  }
  return 0; 
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void handle_4V2V_ess(const uint8_t dibits[]) {
  int i, j, k, ec;
  #if 0

//	std::string s = "";
 //       if (d_debug >= 10) {
	//	fprintf(stderr, "%s %s_BURST ", logts.get(), (burst_id < 4) ? "4V" : "2V");
	//}

    if (burst_id < 4) {
      for (i=0; i < 12; i += 3) { // ESS-B is 4 hexbits / 12 dibits
        ESS_B[(4 * burst_id) + (i / 3)] = (uint8_t) ((dibits[i] << 4) + (dibits[i+1] << 2) + dibits[i+2]);
      }
    }
    else {

      j = 0;
      for (i = 0; i < 28; i++) { // ESS-A is 28 hexbits / 84 dibits
              ESS_A[i] = (uint8_t) ((dibits[j] << 4) + (dibits[j+1] << 2) + dibits[j+2]);
              j = (i == 15) ? (j + 4) : (j + 3);  // skip dibit containing DUID#3
      }

      ec = rs28.decode(ESS_B, ESS_A);

      if (ec >= 0) { // save info if good decode
        ess_algid = (ESS_B[0] << 2) + (ESS_B[1] >> 4);
        ess_keyid = ((ESS_B[1] & 15) << 12) + (ESS_B[2] << 6) + ESS_B[3]; 

        j = 0;
        for (i = 0; i < 9;) {
                 ess_mi[i++] = (uint8_t)  (ESS_B[j+4]         << 2) + (ESS_B[j+5] >> 4);
                 ess_mi[i++] = (uint8_t) ((ESS_B[j+5] & 0x0f) << 4) + (ESS_B[j+6] >> 2);
                 ess_mi[i++] = (uint8_t) ((ESS_B[j+6] & 0x03) << 6) +  ESS_B[j+7];
                 j += 4;
        }
        //s = "{\"nac\" : " + std::to_string(d_nac) + ", \"algid\" : " + std::to_string(ess_algid) + ", \"alg\" : \"" + lookup(ess_algid, ALGIDS, ALGIDS_SZ) + "\", \"keyid\": " + std::to_string(ess_keyid) + "}";
        //send_msg(s, -3);
      }
    }

    //if (d_debug >= 10) {
     //       fprintf(stderr, "ESS: algid=%x, keyid=%x, mi=", ess_algid, ess_keyid);        
      //      for (int i = 0; i < 9; i++) {
       //             fprintf(stderr,"%02x ", ess_mi[i]);
        //    }
//fprintf(stderr, "\n");
    //}
  #endif
}

#if 0
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void p25p2_tdma::send_msg(const std::string msg_str, long msg_type)
{
	if (!d_do_msgq || d_msg_queue->full_p())
		return;

	gr::message::sptr msg = gr::message::make_from_string(msg_str, msg_type, 0, 0);
	d_msg_queue->insert_tail(msg);
}
#endif



#if 0
////////////////////////////////////////////////////////////////////////////////////////
//  this demod function is taken from dsd-dmr fork of dsd
////////////////////////////////////////////////////////////////////////////////////////
void demodAmbe3600x24x0Data (int *errs2, char ambe_fr[4][24], char *ambe_d)
{
  int i, j, k;
  unsigned int block = 0, errs = 0;
  unsigned short pr[115], foo;
  char *ambe = ambe_d;

  for (j = 22; j >= 0; j--) {
      block <<= 1;
      block |= ambe_fr[0][j+1];
  }

  errs = Golay23_CorrectAndGetErrCount(&block);

  // create pseudo-random modulator
  foo = block;
  pr[0] = (16 * foo);
  for (i = 1; i < 24; i++) {
      pr[i] = (173 * pr[i - 1]) + 13849 - (65536 * (((173 * pr[i - 1]) + 13849) >> 16));
  }
  for (i = 1; i < 24; i++) {
      pr[i] >>= 15;
  }

  // just copy C0
  for (j = 11; j >= 0; j--) {
      *ambe++ = ((block >> j) & 1);
  }

  // demodulate C1 with pr
  // then, ecc and copy C1
  k = 1;
  for (j = 22; j >= 0; j--) {
      block <<= 1;
      block |= (ambe_fr[1][j] ^ pr[k++]);
  }

  errs += Golay23_CorrectAndGetErrCount(&block);


  for (j = 11; j >= 0; j--) {
      *ambe++ = ((block >> j) & 1);
  }

  // just copy C2
  for (j = 10; j >= 0; j--) {
      *ambe++ = ambe_fr[2][j];
  }

  // just copy C3
  for (j = 13; j >= 0; j--) {
      *ambe++ = ambe_fr[3][j];
  }

  *errs2 = errs;
}


////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////
void process_ambe_arm(uint8_t *ambe_dibits, int is_last) {
  unsigned int i, dibit;

  //dmr_s->dmr_voice_timeout=40;

  errors=0;
  errors2=0;

  //de-interleave
    for (i = 0; i < 36; i++) {
      dibit = ambe_dibits[i];
      ambe_fr[rW[2*i+0]][rX[2*i+0]] = (1 & (dibit >> 1));        // bit 1
      ambe_fr[rW[2*i+1]][rX[2*i+1]] = (1 & dibit);               // bit 0
    }

  //demod and use Golay to correct errors
  demodAmbe3600x24x0Data(&errors2, ambe_fr, ambe_d);

  //if(errors<=3 && errors2<=3) {
  if(errors==0 && errors2==0) {
    printf("\r\nambe frame is correct, %d, %d", errors, errors2);

    printf("\r\n$AMBE ");
    for(i=0;i<49;i++) {
      printf("0x%02x,", ambe_d[i]); 
    }
  }
  else {
    printf("\r\nambe_errors: %d, %d", errors, errors2);
  }

  #if 0
  process_mbe( ambe_d, errors, errors2, 0, is_last, 0);

  if(++voice_count_dmr>VOICE_MOD ) {
    uint32_t prim = __get_PRIMASK();
    __disable_irq();
      voice_count_dmr=0;
      is_playing=1;
    if( !prim ) {
      __enable_irq();
    }
  }
  #endif

}
///////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////
static unsigned int GolayGenerator[12] = {
  0x63a, 0x31d, 0x7b4, 0x3da, 0x1ed, 0x6cc, 0x366, 0x1b3, 0x6e3, 0x54b, 0x49f, 0x475
};

///////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////
void Golay23_Correct( unsigned int *block )
{
  unsigned int i, syndrome = 0;
  unsigned int mask, block_l = *block;

  mask = 0x400000l;
  for( i = 0; i < 12; i++ ) {
    if( ( block_l & mask ) != 0 ) {
      syndrome ^= GolayGenerator[i];
    }
    mask = mask >> 1;
  }

  syndrome ^= ( block_l & 0x07ff );
  *block = ( ( block_l >> 11 ) ^ GolayMatrix[syndrome] );
}
#endif



///////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////
int mbe_dequantizeAmbeTone(mbe_tone * tone, const int *u) {

	int bitchk1, bitchk2;
	int AD, ID1, ID2, ID3, ID4;
	bitchk1 = (u[0] >> 6) & 0x3f;
	bitchk2 = (u[3] & 0xf);

	if ((bitchk1 != 63) || (bitchk2 != 0))
		return -1; // Not a valid tone frame

	AD = ((u[0] & 0x3f) << 1) + ((u[3] >> 4) & 0x1);
	ID1 = ((u[1] & 0xfff) >> 4);
	ID2 = ((u[1] & 0xf) << 4) + ((u[2] >> 7) & 0xf);
	ID3 = ((u[2] & 0x7f) << 1) + ((u[3] >> 13) & 0x1);
	ID4 = ((u[3] & 0x1fe0) >> 5);

	if ((ID1 == ID2) && (ID1 == ID3) && (ID1 == ID4) &&
	    (((ID1 >= 5) && (ID1 <= 122)) || ((ID1 >= 128) && (ID1 <= 163)) || (ID1 == 255))) {
		if (tone->ID == ID1) {
			tone->AD = AD;
		} else {
			tone->n = 0;
			tone->ID = ID1;
			tone->AD = AD;
		}
		return 0; // valid in-range tone frequency 
	}

	return -1;
}


///////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////
void decode_ambe_tone_hack( float *buffer ) {

  tone_mod++;
  if(tone_mod%3==0) tone_idx++;
  tone_idx&=0x03;

  switch(tone_idx) {
    case  0 :
      decode_tone(5, 256, buffer);
    break;
    case  1 :
      decode_tone(128, 256, buffer);
    break;
    case  2 :
      decode_tone(5, 256, buffer);
    break;
    case  3 :
      decode_tone(139, 256, buffer);
    break;
  }

}

///////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////
void decode_tone(int _ID, int _AD, float * _n) {

   int en;
   float step1, step2, sample, amplitude;
   float freq1 = 0, freq2 = 0;


   switch(_ID) {
      // single tones, set frequency
      case 5:
         freq1 = 156.25; freq2 = freq1;
         break;
      case 6:
         freq1 = 187.5; freq2 = freq1;
         break;
      // DTMF
      case 128:
         freq1 = 1336; freq2 = 941;
         break;
      case 129:
         freq1 = 1209; freq2 = 697;
         break;
      case 130:
         freq1 = 1336; freq2 = 697;
         break;
      case 131:
         freq1 = 1477; freq2 = 697;
         break;
      case 132:
         freq1 = 1209; freq2 = 770;
         break;
      case 133:
         freq1 = 1336; freq2 = 770;
         break;
      case 134:
         freq1 = 1477; freq2 = 770;
         break;
      case 135:
         freq1 = 1209; freq2 = 852;
         break;
      case 136:
         freq1 = 1336; freq2 = 852;
         break;
      case 137:
         freq1 = 1477; freq2 = 852;
         break;
      case 138:
         freq1 = 1633; freq2 = 697;
         break;
      case 139:
         freq1 = 1633; freq2 = 770;
         break;
      case 140:
         freq1 = 1633; freq2 = 852;
         break;
      case 141:
         freq1 = 1633; freq2 = 941;
         break;
      case 142:
         freq1 = 1209; freq2 = 941;
         break;
      case 143:
         freq1 = 1477; freq2 = 941;
         break;
      // KNOX
      case 144:
         freq1 = 1162; freq2 = 820;
         break;
      case 145:
         freq1 = 1052; freq2 = 606;
         break;
      case 146:
         freq1 = 1162; freq2 = 606;
         break;
      case 147:
         freq1 = 1279; freq2 = 606;
         break;
      case 148:
         freq1 = 1052; freq2 = 672;
         break;
      case 149:
         freq1 = 1162; freq2 = 672;
         break;
      case 150:
         freq1 = 1279; freq2 = 672;
         break;
      case 151:
         freq1 = 1052; freq2 = 743;
         break;
      case 152:
         freq1 = 1162; freq2 = 743;
         break;
      case 153:
         freq1 = 1279; freq2 = 743;
         break;
      case 154:
         freq1 = 1430; freq2 = 606;
         break;
      case 155:
         freq1 = 1430; freq2 = 672;
         break;
      case 156:
         freq1 = 1430; freq2 = 743;
         break;
      case 157:
         freq1 = 1430; freq2 = 820;
         break;
      case 158:
         freq1 = 1052; freq2 = 820;
         break;
      case 159:
         freq1 = 1279; freq2 = 820;
         break;
      // dual tones
      case 160:
         freq1 = 440; freq2 = 350;
         break;
      case 161:
         freq1 = 480; freq2 = 440;
         break;
      case 162:
         freq1 = 620; freq2 = 480;
         break;
      case 163:
         freq1 = 490; freq2 = 350;
         break;
      // zero amplitude
      case 255:
         freq1 = 0; freq2 = 0;
      default:
      // single tones, calculated frequency
         if ((_ID >= 7) && (_ID <= 122)) {
            freq1 = 31.25 * _ID; freq2 = freq1;
         }
   }

   // Zero Amplitude and unimplemented tones
   if ((freq1 == 0) && (freq2 == 0)) {
      for(en = 0; en <= 159; en++) {
        _n[en] = 0.0f;
      }
      return;
   }

   // Synthesize tones
   step1 = 2 * M_PI * freq1 / 8000;
   step2 = 2 * M_PI * freq2 / 8000;
   amplitude = _AD * 75; // make adjustment to overall tone amplitude here
   for (en = 0; en<=159; en++) {
      sample =  amplitude * (SINF((sin_gen) * step1)/2 + SINF((sin_gen) * step2)/2);
     sin_gen++;
     _n[en] = sample; 
   }
}
