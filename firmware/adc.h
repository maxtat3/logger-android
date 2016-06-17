#ifndef ADC_H
#define ADC_H

#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include <util/atomic.h>
#include <stdbool.h>

void init_adc(void);
unsigned int get_adc_val_1(void);

#endif