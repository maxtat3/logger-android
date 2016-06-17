#include "usart.h"

/* Однобайтный буфер */
volatile unsigned char usartRxBuf = 0;

/*
* Прием символа по usart`у в буфер
*/
ISR(USART_RXC_vect){ 
   usartRxBuf = UDR;  
} 

void init_usart(void){
	UBRRH = USART_UBRR_HIGH_PART;
	UBRRL = USART_UBRR_LOW_PART; 
	
	// UCSRA=(1<<U2X);
	UCSRB=(1<<RXCIE)|(1<<RXEN)|(1<<TXEN); //разр. прерыв при приеме, разр приема, разр передачи.
	UCSRC=(1<<URSEL)|(1<<UCSZ1)|(1<<UCSZ0);  //размер слова 8 разрядов
}

void sendCharToUSART(unsigned char sym){
	while(!(UCSRA & (1<<UDRE)));
	UDR = sym;  
}

unsigned char getCharOfUSART(void){
	unsigned char tmp;
	ATOMIC_BLOCK(ATOMIC_FORCEON){
		tmp = usartRxBuf;
		usartRxBuf = 0;
	}
	return tmp;  
}