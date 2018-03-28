/**
 * Code for the driver station button interface for Spudnik
 * Authors: Alex Mikhalev & Lee Bousfield
 * Date: March 7, 2014
 * Debouncer code, joystick library update by Avery Bainbridge
 * Date: March 22, 2018
 */

// Whether debug mode is on or not
#define DEBUG true

// The state of a pin when its button is pressed
#define BUTTON_ON HIGH

#include "Button.h"

#include <Joystick.h>

Joystick_ joySt;

// The number of buttons we have
const uint8_t NUM_BUTTONS = 12;

// The pins to read for button input values
const uint8_t buttonPins[] = {
  2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};

// The pins values that have been read with digitalRead()
boolean ioValues[NUM_BUTTONS];

// The state of the joystick
//JoyState_t joySt;

Button buttons[NUM_BUTTONS];

/**
 * Called once at Arduino startup
 */
void setup() {
  setupIO();
  setupJoy();

#if DEBUG
  setupSerial();
#endif
}

/**
 * Called periodically forever after setup()
 */
void loop() {

  updateJoy();

#if DEBUG
  outputDebug();
#endif
}

/**
 * Initialize serial port debugging
 */
#if DEBUG
void setupSerial() {
  // Initialize serial at 9600 baud
  Serial.begin(9600);
  // Display initialization message
  Serial.println("SpudnikButtons - Driver Station button interface for Spudnik");
  Serial.println("Written by Alex Mikhalev & Lee Bousfield");
  Serial.println("Using buttons: ");
  for (uint8_t i = 0; i < NUM_BUTTONS; i++) {
    Serial.print(String(i) + ": " + String(buttonPins[i]) + ", ");
  }
}

/**
 * Output debug messages to Serial
 */
void outputDebug() {
  for (uint8_t i = 0; i < NUM_BUTTONS; i++) {
    if (ioValues[i] == BUTTON_ON) {
      Serial.println("Button " + String(i) + " is pressed");
    }
  }
}
#endif

/**
 * Set up io pin states
 */
void setupIO() {
  // Set each pin we are using to input
  for(uint8_t i = 0; i < NUM_BUTTONS; i++) {
    pinMode(buttonPins[i], INPUT);
    buttons[i] = Button(buttonPins[i], 50);
  }
}

/**
 * Setup the joystick with default values
 */
void setupJoy() {
  joySt.setXAxis(0);
  joySt.setYAxis(0);
  joySt.setZAxis(0);
  joySt.setRxAxis(0);
  joySt.setRyAxis(0);
  joySt.setRzAxis(0);
  joySt.setThrottle(0);
  joySt.setRudder(0);
  joySt.setHatSwitch(0, 0);
  joySt.setHatSwitch(1, 0);
}

/**
 * Update joystick buttons from pins
 */
void updateJoy() {
  // Read all pin values into ioValues
  for(uint8_t i = 0; i < NUM_BUTTONS; i++) {
    bool rawValue = (digitalRead(buttonPins[i]) == BUTTON_ON);
    buttons[i].update();
    bool value = (buttons[i].get() == BUTTON_ON);
    joySt.setButton(i, value ? 1 : 0);
  }
  joySt.sendState();
}


