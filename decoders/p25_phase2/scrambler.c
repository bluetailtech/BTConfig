///////////////////////////////////////////////////
// Conversion to 'C' - BlueTail Technologies
///////////////////////////////////////////////////

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include "scrambler.h"

#include "matrix.h"

static uint64_t seed;
static uint8_t _reg[44];
static uint8_t reg[44];

static uint64_t s1;
static uint64_t s2;
static uint64_t s3;
static uint64_t s4;
static uint64_t s5;
static uint64_t s6;

static uint64_t lfsr;
static uint8_t xor_dibits[4320/2];

//////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////
uint64_t asm_reg(void) { 
  s1 = (s1 & 0xf);
  s2 = (s2 & 0x1f);
  s3 = (s3 & 0x3f);
  s4 = (s4 & 0x1f);
  s5 = (s5 & 0x3fff);
  s6 = (s6 & 0x3ff);
  return (uint64_t) ((s1<<40)+(s2<<35)+(s3<<29)+(s4<<24)+(s5<<10)+s6);
}
//////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////
void disasm_reg(uint64_t r) { 
  s1 = ((r>>40) & 0xf);
  s2 = ((r>>35) & 0x1f);
  s3 = ((r>>29) & 0x3f);
  s4 = ((r>>24) & 0x1f);
  s5 = ((r>>10) & 0x3fff);
  s6 = (r & 0x3ff);
}

//////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////
void cyc_reg(void) {

  disasm_reg(lfsr);

  uint8_t cy1 = (s1 >> 3) & 1;
  uint8_t cy2 = (s2 >> 4) & 1;
  uint8_t cy3 = (s3 >> 5) & 1;
  uint8_t cy4 = (s4 >> 4) & 1;
  uint8_t cy5 = (s5 >> 13) & 1;
  uint8_t cy6 = (s6 >> 9) & 1;

  uint8_t x1 = cy1 ^ cy2;
  uint8_t x2 = cy1 ^ cy3;
  uint8_t x3 = cy1 ^ cy4;
  uint8_t x4 = cy1 ^ cy5;
  uint8_t x5 = cy1 ^ cy6;

  s1 = (s1 << 1) & 0xf;
  s2 = (s2 << 1) & 0x1f;
  s3 = (s3 << 1) & 0x3f;
  s4 = (s4 << 1) & 0x1f;
  s5 = (s5 << 1) & 0x3fff;
  s6 = (s6 << 1) & 0x3ff;

  s1 |= (x1 & 1);
  s2 |= (x2 & 1);
  s3 |= (x3 & 1);
  s4 |= (x4 & 1);
  s5 |= (x5 & 1);
  s6 |= (cy1 & 1);

  lfsr = asm_reg();
}

//////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////
void scrambler_init(uint32_t wacn, uint16_t sys_id, uint16_t nac) {
  int i;
  seed = (uint64_t) (16777216*(uint64_t)wacn + 4096*(uint64_t)sys_id + (uint64_t)nac);

  if(seed==0) seed = 0xfffffffffff; //44 one bits 

  ////////////////////////
  //44-bit seed vector
  ////////////////////////
  int pos=43;
  for(i=0;i<44;i++) {
    _reg[i] = (seed>>pos-- & 0x01);
  }

  int row=0;
  int col=0;

  memset(reg,0x00,44);

  #if 0
  printf("\r\nmatrix:\r\n");
  for(row=0;row<44;row++) {
    for(col=0;col<44;col++) {
      printf("%d ", tdma_matrix_M[row][col]);
    }
    printf("\r\n");
  }
  #endif

  /////////////////////////////////
  //dot product  (seed _dot_ M)
  /////////////////////////////////
  for(col=0;col<44;col++) {
    for(row=0;row<44;row++) {
      reg[col] ^= tdma_matrix_M[row][col] * _reg[row];  
    }
  }

  lfsr = 0;

  //printf("\r\nlfsr:\r\n");
  for(i=0;i<44;i++) {
    lfsr<<=1;
    if(reg[i]) lfsr|=0x01; 
    //printf("%d ", lfsr&0x01 );
  }

  memset(xor_dibits,0x00,4320/2);

  for(i=0;i<4320/2;i++) {
    xor_dibits[i] = (lfsr>>43&0x01);
    xor_dibits[i] <<= 1;

    cyc_reg();

    xor_dibits[i] |= (lfsr>>43&0x01);

    cyc_reg();
  }

  p2_set_xormask(xor_dibits);  //   4320/2 dibit array
}
