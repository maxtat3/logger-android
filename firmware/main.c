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


bool isTranslateData = false;


int main(void){
	unsigned char sym;
	
	cli();
	init_debug();
	init_adc();
	init_usart();
	sei();
	
	ADCSRA |= (1<<ADSC); // запускаем первое АЦП преобразование

	blik_led1();
	
	while(1){
		sym = getCharOfUSART();

		if(sym == 'b'){
			isTranslateData = !isTranslateData;
		} 

		//----

		if (isTranslateData){
			sendCharToUSART((unsigned char)(get_adc_val_1()/4));
			_delay_ms(100);
		}

	}
}

