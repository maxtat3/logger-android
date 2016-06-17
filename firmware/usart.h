#ifndef USART_H
#define USART_H

#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/atomic.h>

// Значения регистров UBRRH и UBRRL в зависимости от
// частоты контроллера (Fcpu) и скорости передачи данных .
// Во всех случаях U2X = 0 .
// U2X - удвоение скорости при работе в ассинхронном режиме.
// !!! Нужно расскомментировать нжную пары констант !!!
// !!! Также нужно не забывать изменить чатоту контроллера в Makefile !!!

// 19200 бод @ 14,7456 MHz 
// #define USART_UBRR_HIGH_PART 0
// #define USART_UBRR_LOW_PART 47

// 9600 бод @ 14,7456 MHz 
// #define USART_UBRR_HIGH_PART 0
// #define USART_UBRR_LOW_PART 95

// 38400 бод @ 14,7456 MHz 
// #define USART_UBRR_HIGH_PART 0
// #define USART_UBRR_LOW_PART 23

// 38400 бод @ 8 MHz 
#define USART_UBRR_HIGH_PART 0
#define USART_UBRR_LOW_PART 12

/*
* Настройка USART
*/
void init_usart(void);

/*
* Отправка символа по usart`у
*/
void sendCharToUSART(unsigned char sym);

/*
* Чтение симвлоа из буфера
*/
unsigned char getCharOfUSART(void);

#endif