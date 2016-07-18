# Logger Mobile
This project that allows to visualize in dynamic chart the analog signals on the screen android device. In addition there is a record of the results to csv file for further analysis in the any programs. Only for research slowly changing analog signals. This project may be using in research sphere. He is a basic model for further creations target device for particular task.

![Schematic](https://cloud.githubusercontent.com/assets/12572241/16412880/3fc18978-3d37-11e6-909d-efeeebe914a6.png)

## Description

It includes the following block:

 * Electronic board with microcontroller ATmega8 (MCU) for get analog data and directions
 * Bluetooth module HC-05 for data exchaing with MCU and android device
 * Any analogue sensors (temperature, preasure, etc)
 * Any android device (smartphone or tablet)

Basic useful feature list:

 * Four channels to measure 
 * No wires for connecting electronic block and target displaying device (android device). So NO electric galvanic connection
 * Displaying dynamic chart in full screen
 * After stopping measure process, moving and zooming chart
 * Saving measure data to csv file 

## Pre requirements in making 
Electronic (hardware) part: 

* ATmega 8 or other Atmel MCU with USART and ADC module
* HC-05 bluetooth module 
* USBASP, AVR910 or other Atmel comparable programmer for write firmware to MCU

Software part:
* tablet or smartphone android device
* Intellij IDEA or Android Studio
* gradle 2.4 or higher
* android SDK (api 14 level minimum)
* java 7 or higher

## Instalation
1. To begin we must assemble electronic part. See *./logger-android/circuit* directory. 
2. Connect atmel comparable programmer to PC (COM, USB, LPT) and MCU (ISP). May be neccessary set permissions for work with it in linux systems.
3. Write firmware to MCU. Now we compile of sources. Go to *./logger-android/firmware* catalog. 
> **Note:** Must be installed **avr gcc** packages for compile avr projects. For Windows platforms see WinAVR project. For linux you can install from terminal next packages:    
```sudo apt-get install avrdude gcc-avr binutils-avr gdb-avr avr-libc```    
  > **Note:** When crystal frequency selected different of in this project, open MakeFile , find F_CPU constant and select for your case. In additions necessary recalculate constants for USART module. As example constants collected [see in Atmega8 documentation](http://www.atmel.com/images/atmel-2486-8-bit-avr-microcontroller-atmega8_l_datasheet.pdf) in tables 60 - 63 in page 153 . Open usart.h file and changed USART_UBRR_HIGH_PART and USART_UBRR_LOW_PART constants for new MCU frequency rate.

  Execute next command for compile and burn firmware in linux systems:     
  ```make clean && make build && sudo make program```    
  and similar command placed in WinAVR Studio for Windows. 
4. Writting fuse bits. To see alredy burned fusebits in MCU execute command    
```sudo avrdude -P pPort -c pName -p mcuName -v```    
Where:   
  pPort - programmer port. For example: /dev/ttyACM0, usb   
  pName - programmer name. For example: avr910, usbasp   
  mcuName - MCU name. For example: ATmega8, attiny2313   

  Now we can burning fusebits for this project. High Byte = D9, Low Byte = EF.    
  Execute next command:  
  ```avrdude -c porgrammerName -p mcuName -U lfuse:w:0xEF:m -U hfuse:w:0xD9:m```     
5. Next step set settings in HC-05 bluetooth module. With help it AT commands configure HC-05 module. You need download proprietary software. Now should set parametrs: 38400 baude rate, id (enterred later in android application),password, other settings. For more see documentation in this bluetooth module.
6. Enable assembled electronic device when it is turn off.
7. Connect android device to PC via usb. Only for install logger application.
8. In IDE Intellij IDEA open this project in *./logger-android/mobile* catalog as gradle project selected buid.gradle 
9. Run **clean** and **build** gradle tasks in *app* module.
10. After that chould be created apk file for transfer to android device in directory *./logger-android/mobile/app/build/outputs/apk/*

  > **Note:** You can build and run android project another way. Directly from Intellij IDEA. In *Run/Debug configuration* (top, right display part in IDE) select *app* module and press *run* button or press shortkey Shift+F10 and select your connected device in list. Apk file creating automatly in same directory.
11. Run logger application in android device.
12. Application make request to turn on bluetooth module in android device. Aggry and press *Ok* button

## Getting started
Turn on electronic device. Connect sensors. Launch logger application in android device. In application we can entered bluetooth identifier for our electronic device (in which embedded HC-05). Entered her instead 00:00:00:00:00:00 of the default set. If we were correct id, module starts to scan and must be connection sucessfull. In android application press **start** button in action bar - measure process started. If need record data to file before start measure process, press **record** button. To stop process press **stop** button. Saving files in csv format placed in internal storage in *Logger saved files* directory. To reconnect device (if connection failed or interference gap) press **refresh** button in action bar the application.

## License 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
