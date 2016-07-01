//***************************************************************************
//
//	Description.: регистратор
//
//  Target(s) mcu...: mega8
//
//  Compiler....: gcc-4.3.3 (WinAVR 2010.01.10)
//	
//***************************************************************************

#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include <util/atomic.h>
#include <stdbool.h>
#include "adc.h"
#include "usart.h"
#include "debug.h"

// Delay between translated data
#define	TX_BYTE_DELAY		_delay_ms(60)

// If this flag true - command for start measure process and sending data to USART
// otherwise - command for start measure process.
bool isTranslateData = false;

/*
* Converted value and status (number of channel) to two Bytes.
* val - adc value, 10 bit max [0 ... 1023]
* st - status, command, 4 bit max [0 ... 15]
*/
void cmdAndDataConv(unsigned int val, unsigned int st, unsigned char *high, unsigned char *low);


int main(void){
	unsigned char low; // low Byte
	unsigned char high; // high Byte
	unsigned char sym;
	
	cli();
	init_debug();
	init_adc();
	init_usart();
	sei();
	
	ADCSRA |= (1<<ADSC); // запускаем первое АЦП преобразование
	
	while(1){
		sym = getCharOfUSART();

		if(sym == 't'){
			isTranslateData = !isTranslateData;
		} 

		if (isTranslateData){
			cmdAndDataConv(get_adc_val_0(), 0, &high, &low);
			sendCharToUSART(low);
			TX_BYTE_DELAY;
			sendCharToUSART(high);
			TX_BYTE_DELAY;

			cmdAndDataConv(get_adc_val_1(), 1, &high, &low);
			sendCharToUSART(low);
			TX_BYTE_DELAY;
			sendCharToUSART(high);
			TX_BYTE_DELAY;

			cmdAndDataConv(get_adc_val_2(), 2, &high, &low);
			sendCharToUSART(low);
			TX_BYTE_DELAY;
			sendCharToUSART(high);
			TX_BYTE_DELAY;

			cmdAndDataConv(get_adc_val_3(), 3, &high, &low);
			sendCharToUSART(low);
			TX_BYTE_DELAY;
			sendCharToUSART(high);
			TX_BYTE_DELAY;
		}
	}
}

void cmdAndDataConv(unsigned int val, unsigned int st, unsigned char *high, unsigned char *low){
	*low = val & 0x7F;
    *high = (val >> 7) & 0x7;
    *high = ((st << 3) & 0x78) | *high;
}

