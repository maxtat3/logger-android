#ifndef ADC_H
#define ADC_H

#include <avr/io.h>
#include <avr/interrupt.h>

/*
* Настройка АЦП
*/
void init_adc(void);

/*
* Получение АЦП значения канала 0
*/
unsigned int get_adc_val_0(void);

/*
* Получение АЦП значения канала 1
*/
unsigned int get_adc_val_1(void);

/*
* Получение АЦП значения канала 2
*/
unsigned int get_adc_val_2(void);

/*
* Получение АЦП значения канала 3
*/
unsigned int get_adc_val_3(void);

#endif