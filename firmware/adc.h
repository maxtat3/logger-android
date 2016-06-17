#ifndef ADC_H
#define ADC_H

#include <avr/io.h>
#include <avr/interrupt.h>

/*
* Настройка АЦП
*/
void init_adc(void);

/*
* Получение АЦП значения канала 1
*/
unsigned int get_adc_val_1(void);

#endif