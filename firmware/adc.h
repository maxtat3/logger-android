#ifndef ADC_H
#define ADC_H

#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include <util/atomic.h>
#include <stdbool.h>

/*
* Настройка АЦП
*/
void init_adc(void);

/*
* Получение АЦП значения канала 1
*/
unsigned int get_adc_val_1(void);

#endif