

///////////////////////////////////////////////////
// Conversion to 'C' - BlueTail Technologies
///////////////////////////////////////////////////

void handle_4V2V_ess(const uint8_t dibits[]);
int handle_packet(const uint8_t dibits[]);
void handle_voice_frame(const uint8_t dibits[], int bt);
void reset_vb(void);
void p2_set_xormask(const char *p);
void p2_set_slotid(int slotid);
int encrypted(void);
void Golay23_Correct( unsigned int *block );
unsigned int Golay23_CorrectAndGetErrCount( unsigned int *block );
int process_mac_pdu(const uint8_t byte_buf[], const unsigned int len);
void handle_mac_end_ptt(const uint8_t byte_buf[], const unsigned int len);
void handle_mac_idle(const uint8_t byte_buf[], const unsigned int len);
void handle_mac_active(const uint8_t byte_buf[], const unsigned int len);
void handle_mac_hangtime(const uint8_t byte_buf[], const unsigned int len);

struct mbe_tones
{
    int ID;
    int AD;
    int n;
};

typedef struct mbe_tones mbe_tone;
uint16_t crc12(const uint8_t bits[], unsigned int len);
int crc12_ok(const uint8_t bits[], unsigned int len);

int mbe_dequantizeAmbeTone(mbe_tone * tone, const int *u);
void decode_tone(int _ID, int _AD, float * _n);
void decode_ambe_tone_hack( float *buffer );
extern volatile int is_phase2;
