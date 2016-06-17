#ifndef USART_H
#define USART_H

#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include <util/atomic.h>
#include <stdbool.h>

void init_usart(void);
void sendCharToUSART(unsigned char sym);
unsigned char getCharOfUSART(void);

#endif