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
	// UBRR=47 @ 19200 бод при 14,7456 MHz (U2X = 0)
	// самый оптимальный вариант (16 выб/с для 4 канала)
	// UBRRH = 0;
	// UBRRL = 47; 
	
	//UBRR=95 @ 9600 бод при 14,7456 MHz (U2X = 0)
	// примерно 15 выб/с для 4 канала
	// UBRRH = 0;
	// UBRRL = 95; 

	//UBRR=... @ 38400 бод при 14,7456 MHz (U2X = 0)
	// UBRRH = 0;
	// UBRRL = 23; 

	//UBRR=... @ 38400 бод при 8 MHz (U2X = 0)
	UBRRH = 0;
	UBRRL = 12; 
	
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