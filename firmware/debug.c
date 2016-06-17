#include "debug.h"

// настройка портов в/в
void init_debug(void){
	DDRB |= (_BV(3));
	DDRB |= (_BV(4));
}

// тестовая ф-ия 1
void blik_led1(void){
	PORTB = LED1;
	LED_PAUSE;
	PORTB = OFF_ALL_LEDS;
	LED_PAUSE;
}

// тестовая ф-ия 2
void blik_led2(void){
	PORTB = LED2;
	LED_PAUSE;
	PORTB = OFF_ALL_LEDS;
	LED_PAUSE;
}