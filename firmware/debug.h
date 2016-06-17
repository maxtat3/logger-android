#ifndef DEBUG_H
#define DEBUG_H 

#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include <util/atomic.h>
#include <stdbool.h>

#define		LED1	0b00000001
#define		LED2	0b00010000
#define		OFF_ALL_LEDS	0x00
#define		LED_PAUSE		_delay_ms(1000)

void init_debug(void);
void blik_led1(void);
void blik_led2(void);

#endif

