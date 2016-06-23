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

// If this flag true - command for start measure process and sending data to USART
// otherwise - command for start measure process.
bool isTranslateData = false;


int main(void){
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

		_delay_ms(100); 

		if (isTranslateData){
			if (sym == '0'){
				sendCharToUSART((unsigned char)(get_adc_val_0()/4));
			} else if(sym == '1'){
				sendCharToUSART((unsigned char)(get_adc_val_1()/4));
			} else if(sym == '2'){
				sendCharToUSART((unsigned char)(get_adc_val_2()/4));
			} else if(sym == '3'){
				sendCharToUSART((unsigned char)(get_adc_val_3()/4));
			}
		}

		// debug data set 
		// if (sym == '0'){
		// 	sendCharToUSART((unsigned char)30);
		// } else if(sym == '1'){
		// 	sendCharToUSART((unsigned char)50);
		// } else if(sym == '2'){
		// 	sendCharToUSART((unsigned char)70);
		// } else if(sym == '3'){
		// 	sendCharToUSART((unsigned char)110);
		// }
	}
}

