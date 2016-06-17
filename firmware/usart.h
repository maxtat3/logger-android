#ifndef USART_H
#define USART_H

#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include <util/atomic.h>
#include <stdbool.h>

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