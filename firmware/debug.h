#ifndef DEBUG_H
#define DEBUG_H 

#include <avr/io.h>
#include <util/delay.h>

#define		LED1	0b00000001
#define		LED2	0b00010000
#define		OFF_ALL_LEDS	0x00
#define		LED_PAUSE		_delay_ms(1000)

/*
* Настройка портов в/в
*/
void init_debug(void);

/*
* Отлавдочная ф-ия 1
* Включение светодиода
*/
void blik_led1(void);

/*
* Отлавдочная ф-ия 2
* Включение светодиода
*/
void blik_led2(void);

#endif

