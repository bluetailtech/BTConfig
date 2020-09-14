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

#include <stdio.h>
#include <stdint.h>
#include "p2_sync.h"
#include "p2_isch.h"

static int32_t	sync_confidence;
static uint32_t _tdma_slotid;
static uint32_t packets;

///////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////
uint32_t tdma_slotid(void) { 
  return _tdma_slotid; 
}
///////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////
int tdma_in_sync(void) {
	return (sync_confidence != 0);
}

///////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////
void tdma_check_confidence (const uint8_t dibits[]) {

	int rc, cnt, fr, loc, chn, checkval;
	static const int expected_sync[] = {0, 1, -2, -2, 4, 5, -2, -2, 8, 9, -2, -2};

	_tdma_slotid++;

	packets++;

	if (_tdma_slotid >= 12) {
    _tdma_slotid = 0;
  }

	rc = isch_lookup(dibits);

  //printf("\r\nisch_lookup: %d", rc);

	checkval = cnt = fr = loc = chn = rc;

	if (rc >= 0) {
		cnt = rc & 3;
		rc = rc >> 2;
		fr = rc & 1;
		rc = rc >> 1;
		loc = rc & 3;
		rc = rc >> 2;
		chn = rc & 3;
		checkval = loc*4 + chn;
	}

	if (expected_sync[_tdma_slotid] != checkval && checkval != -1) {
		sync_confidence = 0;
  }

	if (chn >= 0) {
		sync_confidence = 1;
		_tdma_slotid = checkval;
	}
}
