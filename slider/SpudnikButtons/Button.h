/**********************************************

     Button library

     Author: Avery Bainbridge
     Date: 3/22/2018

***********************************************/
#include "Arduino.h"
#ifndef BUTTON_ON
#define BUTTON_ON HIGH
#endif
class Button {
public:
    Button(uint8_t pinIn, unsigned long debounceDelay) {
        pin = pinIn;
        delay = debounceDelay;
    }
    Button(){}
    void update() {
        uint8_t reading = digitalRead(pin);
        if(reading != lastButtonState) {
            lastToggle = micros();
        }
        if((micros() - lastToggle) > delay) {
            if (reading != buttonState) {
               buttonState = reading;
               if (buttonState == BUTTON_ON) {
                   value = !value;
               }
            }
        }
        lastButtonState = reading;
    }
    bool get() {
        return value;
    }
private:
	uint8_t       pin;
	unsigned long delay;
	unsigned long lastToggle;

  uint8_t       buttonState;
	uint8_t       value;
	uint8_t       lastButtonState;
};
