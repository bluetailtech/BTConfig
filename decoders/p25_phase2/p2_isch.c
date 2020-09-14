
// P25 TDMA Decoder (C) Copyright 2013, 2014 Max H. Parke KA1RBI
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
#include <stdio.h>
#include "p2_isch.h"


///////////////////////////////////////////////////
///////////////////////////////////////////////////
int16_t p2_isch(uint64_t cw) {

	if(cw==0x184229d461) return 0;
	if(cw==0x18761451f6) return 1;
	if(cw==0x181ae27e2f) return 2;
	if(cw==0x182edffbb8) return 3;
	if(cw==0x18df8a7510) return 4;
	if(cw==0x18ebb7f087) return 5;
	if(cw==0x188741df5e) return 6;
	if(cw==0x18b37c5ac9) return 7;
	if(cw==0x1146a44f13) return 8;
	if(cw==0x117299ca84) return 9;
	if(cw==0x111e6fe55d) return 10;
	if(cw==0x112a5260ca) return 11;
	if(cw==0x11db07ee62) return 12;
	if(cw==0x11ef3a6bf5) return 13;
	if(cw==0x1183cc442c) return 14;
	if(cw==0x11b7f1c1bb) return 15;
	if(cw==0x1a4a2e239e) return 16;
	if(cw==0x1a7e13a609) return 17;
	if(cw==0x1a12e589d0) return 18;
	if(cw==0x1a26d80c47) return 19;
	if(cw==0x1ad78d82ef) return 20;
	if(cw==0x1ae3b00778) return 21;
	if(cw==0x1a8f4628a1) return 22;
	if(cw==0x1abb7bad36) return 23;
	if(cw==0x134ea3b8ec) return 24;
	if(cw==0x137a9e3d7b) return 25;
	if(cw==0x13166812a2) return 26;
	if(cw==0x1322559735) return 27;
	if(cw==0x13d300199d) return 28;
	if(cw==0x13e73d9c0a) return 29;
	if(cw==0x138bcbb3d3) return 30;
	if(cw==0x13bff63644) return 31;
	if(cw==0x1442f705ef) return 32;
	if(cw==0x1476ca8078) return 33;
	if(cw==0x141a3cafa1) return 34;
	if(cw==0x142e012a36) return 35;
	if(cw==0x14df54a49e) return 36;
	if(cw==0x14eb692109) return 37;
	if(cw==0x14879f0ed0) return 38;
	if(cw==0x14b3a28b47) return 39;
	if(cw==0x1d467a9e9d) return 40;
	if(cw==0x1d72471b0a) return 41;
	if(cw==0x1d1eb134d3) return 42;
	if(cw==0x1d2a8cb144) return 43;
	if(cw==0x1ddbd93fec) return 44;
	if(cw==0x1defe4ba7b) return 45;
	if(cw==0x1d831295a2) return 46;
	if(cw==0x1db72f1035) return 47;
	if(cw==0x164af0f210) return 48;
	if(cw==0x167ecd7787) return 49;
	if(cw==0x16123b585e) return 50;
	if(cw==0x162606ddc9) return 51;
	if(cw==0x16d7535361) return 52;
	if(cw==0x16e36ed6f6) return 53;
	if(cw==0x168f98f92f) return 54;
	if(cw==0x16bba57cb8) return 55;
	if(cw==0x1f4e7d6962) return 56;
	if(cw==0x1f7a40ecf5) return 57;
	if(cw==0x1f16b6c32c) return 58;
	if(cw==0x1f228b46bb) return 59;
	if(cw==0x1fd3dec813) return 60;
	if(cw==0x1fe7e34d84) return 61;
	if(cw==0x1f8b15625d) return 62;
	if(cw==0x1fbf28e7ca) return 63;
	if(cw==0x84d62c339) return 64;
	if(cw==0x8795f46ae) return 65;
	if(cw==0x815a96977) return 66;
	if(cw==0x82194ece0) return 67;
	if(cw==0x8d0c16248) return 68;
	if(cw==0x8e4fce7df) return 69;
	if(cw==0x8880ac806) return 70;
	if(cw==0x8bc374d91) return 71;
	if(cw==0x149ef584b) return 72;
	if(cw==0x17dd2dddc) return 73;
	if(cw==0x11124f205) return 74;
	if(cw==0x125197792) return 75;
	if(cw==0x1d44cf93a) return 76;
	if(cw==0x1e0717cad) return 77;
	if(cw==0x18c875374) return 78;
	if(cw==0x1b8bad6e3) return 79;
	if(cw==0xa456534c6) return 80;
	if(cw==0xa7158b151) return 81;
	if(cw==0xa1dae9e88) return 82;
	if(cw==0xa29931b1f) return 83;
	if(cw==0xad8c695b7) return 84;
	if(cw==0xaecfb1020) return 85;
	if(cw==0xa800d3ff9) return 86;
	if(cw==0xab430ba6e) return 87;
	if(cw==0x341e8afb4) return 88;
	if(cw==0x375d52a23) return 89;
	if(cw==0x3192305fa) return 90;
	if(cw==0x32d1e806d) return 91;
	if(cw==0x3dc4b0ec5) return 92;
	if(cw==0x3e8768b52) return 93;
	if(cw==0x38480a48b) return 94;
	if(cw==0x3b0bd211c) return 95;
	if(cw==0x44dbc12b7) return 96;
	if(cw==0x479819720) return 97;
	if(cw==0x41577b8f9) return 98;
	if(cw==0x4214a3d6e) return 99;
	if(cw==0x4d01fb3c6) return 100;
	if(cw==0x4e4223651) return 101;
	if(cw==0x488d41988) return 102;
	if(cw==0x4bce99c1f) return 103;
	if(cw==0xd493189c5) return 104;
	if(cw==0xd7d0c0c52) return 105;
	if(cw==0xd11fa238b) return 106;
	if(cw==0xd25c7a61c) return 107;
	if(cw==0xdd49228b4) return 108;
	if(cw==0xde0afad23) return 109;
	if(cw==0xd8c5982fa) return 110;
	if(cw==0xdb864076d) return 111;
	if(cw==0x645bbe548) return 112;
	if(cw==0x6718660df) return 113;
	if(cw==0x61d704f06) return 114;
	if(cw==0x6294dca91) return 115;
	if(cw==0x6d8184439) return 116;
	if(cw==0x6ec25c1ae) return 117;
	if(cw==0x680d3ee77) return 118;
	if(cw==0x6b4ee6be0) return 119;
	if(cw==0xf41367e3a) return 120;
	if(cw==0xf750bfbad) return 121;
	if(cw==0xf19fdd474) return 122;
	if(cw==0xf2dc051e3) return 123;
	if(cw==0xfdc95df4b) return 124;
	if(cw==0xfe8a85adc) return 125;
	if(cw==0xf845e7505) return 126;
	if(cw==0xfb063f092) return 127;

  return -1;
}

///////////////////////////////////////////////////////
///////////////////////////////////////////////////////
int16_t isch_lookup(const uint8_t dibits[]) {
  int i;
	uint64_t cw = 0;
	for(i=0; i<20; i++) {
    cw <<= 2;
		cw |= dibits[i];
  }

  if(cw == 0x575d57f7ff) return -2; 

  return p2_isch(cw); //-1 indicates error in code word

}
