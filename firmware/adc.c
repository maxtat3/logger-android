#include "adc.h"

#define		HIGH_7	(_BV(7))
#define		HIGH_6	(_BV(6))
#define		HIGH_5	(_BV(5))
#define		HIGH_4	(_BV(4))
#define		HIGH_3	(_BV(3))
#define		HIGH_2	(_BV(2))
#define		HIGH_1	(_BV(1))
#define		HIGH_0	(_BV(0))
#define		LOW_7	(~(_BV(7)))
#define		LOW_6	(~(_BV(6)))
#define		LOW_5	(~(_BV(5)))
#define		LOW_4	(~(_BV(4)))
#define		LOW_3	(~(_BV(3)))
#define		LOW_2	(~(_BV(2)))
#define		LOW_1	(~(_BV(1)))

#define 	ADC1_REFINT		HIGH_7 | HIGH_6 | HIGH_0
#define 	ADC2_REFINT		HIGH_7 | HIGH_6 | HIGH_1
#define 	ADC3_REFINT		HIGH_7 | HIGH_6 | HIGH_1 | HIGH_0
#define 	ADC4_REFINT		HIGH_7 | HIGH_6 | HIGH_2	

volatile unsigned int val1, val2, val3, val4;
volatile unsigned int adcResult;
volatile unsigned char lowByte;

/*
* Обработка прерывания от ацп
*/
ISR(ADC_vect){
	// 1. считываем младший и старший байты результата АЦ-преобразования и образуем из них 10-битовый результат
	lowByte = ADCL;
	adcResult = (ADCH<<8)|lowByte;

	// 2. В зависимости от номера канала ADC сохраняем результат в ячейке памяти и настраиваем номер канала для следующего преобразования
	switch (ADMUX) {
		case ADC1_REFINT:
			val1 = adcResult;
			ADMUX = ADC2_REFINT;
			break;
		case ADC2_REFINT:
			val2 = adcResult;
			ADMUX = ADC3_REFINT;
			break;
		case ADC3_REFINT:
			val3 = adcResult;
			ADMUX = ADC4_REFINT;
			break;
		case ADC4_REFINT:
			val4 = adcResult;
			ADMUX = ADC1_REFINT;
			break;
		default:
		//...
		break;
	}
	// 3. запускаем новое АЦ-преобразование
	ADCSRA |= (1<<ADSC);
}

void init_adc(void){
	ADCSRA |= (1<<ADPS2)|(1<<ADPS1)|(1<<ADPS0); // предделитель на 128
	ADCSRA |= (1<<ADIE);                        // разрешаем прерывание от ацп
	ADCSRA |= (1<<ADEN);                        // разрешаем работу АЦП

	ADMUX |= (1<<REFS0)|(1<<REFS1);             // работа от внутр. ИОН 2,56 В
	ADMUX|=(0<<MUX3)|(0<<MUX2)|(0<<MUX1)|(1<<MUX0);
	//ADMUX|=0b11000001;
}

unsigned int get_adc_val_1(void){
	return val1;
}

unsigned int get_adc_val_2(void){
	return val2;
}

unsigned int get_adc_val_3(void){
	return val3;
}

unsigned int get_adc_val_4(void){
	return val4;
}