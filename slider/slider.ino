/************************************************************
  ____        _   _              _                         _
 |  _ \      | | | |            | |                       | |
 | |_) |_   _| |_| |_ ___  _ __ | |__   ___   __ _ _ __ __| |
 |  _ <| | | | __| __/ _ \| '_ \| '_ \ / _ \ / _` | '__/ _` |
 | |_) | |_| | |_| || (_) | | | | |_) | (_) | (_| | | | (_| |
 |____/ \__,_|\__|\__\___/|_| |_|_.__/ \___/ \__,_|_|  \__,_|

AUTHOR: Avery Bainbridge
DATE: 3/17/2018

************************************************************/
#define VERSION 1.20

#define ENCODER_OPTIMIZE_INTERRUPTS
#include <Encoder.h>

#define AI1  21
#define AI2  22
#define PWMA 23

#define ENCODER_PIN_1 3
#define ENCODER_PIN_2 4
#define ENCODER_RESET_BUTTON 2

#define TOUCH_SENSOR 3
#define LIFT_SLIDER 5
#define POWER_SLIDER 18

#define R_PIN 5
#define G_PIN 7
#define B_PIN 6

#define  MIN_RGB_VALUE  10
#define  MAX_RGB_VALUE  255

#define  TRANSITION_DELAY  70
#define  WAIT_DELAY        500

//#define DEBUG
#ifdef DEBUG
#define ENABLE_LOGGING
#endif
#define READ_RESOLUTION 13
#define WRITE_RESOLUTION 12
#define ERROR_MARGIN 0.003
#define CPR 48.0

const int BUTTON_PORTS[] = {};
const int NUM_BUTTONS = 0;
const double DETENTS[] = {0, 0.145, 0.58, 0.699, 0.843};
const int DETENTS_LENGTH = 5;

//PID values
const double kP = 95;
const double kI = 0;
const double kD = -1;
const double maxOutput = 0.75;

const double PWM_MAX = ((1 << WRITE_RESOLUTION) - 1);

const double OUTPUT_CONVERSION_FACTOR = PWM_MAX;

const double INPUT_CONVERSION_FACTOR = ((1 << READ_RESOLUTION) - 1);

double setpoint;
double serialSetpoint;
double detentSetpoint;
double lastValue;
double totalError;
double lastError;
double lastPosition;

int TEST_ITER = 10;
int tests = 0;
boolean enabled = true;
boolean testing = false;

unsigned long lastTime;

unsigned long testStarted;

int ledIter;
int ledIterT;
double delayLed;

typedef struct
{
  byte  x, y, z;
} coord;

static coord  v;
int    v1, v2=0;

const coord vertex[] =
{
//x  y  z      name
  {0, 0, 0}, // A or 0
  {0, 1, 0}, // B or 1
  {0, 1, 1}, // C or 2
  {0, 0, 1}, // D or 3
  {1, 0, 0}, // E or 4
  {1, 1, 0}, // F or 5
  {1, 1, 1}, // G or 6
  {1, 0, 1}  // H or 7
};

const byte path[] =
{
  //0x01, 0x23, 0x76, 0x54, 0x03, 0x21, 0x56, 0x74,  // trace the edges
  0x13, 0x64, 0x16, 0x02, 0x75, 0x24, 0x35, 0x17, 0x25, 0x70,  // do the diagonals
};

#define  MAX_PATH_SIZE  (sizeof(path)/sizeof(path[0]))  // size of the array

boolean firstRun = true;

boolean traverse(int dx, int dy, int dz, double delta)
// Move along the colour line from where we are to the next vertex of the cube.
// The transition is achieved by applying the 'delta' value to the coordinate.
// By definition all the coordinates will complete the transition at the same
// time as we only have one loop index.
{
  if(ledIterT >= MAX_RGB_VALUE-MIN_RGB_VALUE) {
    delayLed = WAIT_DELAY;
    ledIterT = 0;
    return true;
  }
  delayLed-=delta*10000;
  Serial.printf("delayLed %.3f\n", delayLed);
  Serial.printf("dx %d dy %d dz %d\n", dx, dy, dz);
  Serial.printf("ledIter %d\n", ledIter);
  if ((dx == 0) && (dy == 0) && (dz == 0)) {   // no point looping if we are staying in the same spot!
    //return;
  }
  if(delayLed <= 0)
  {
    // set the colour in the LED
    analogWrite(R_PIN, (v.x / 255.0) * OUTPUT_CONVERSION_FACTOR);
    analogWrite(G_PIN, (v.y / 255.0) * OUTPUT_CONVERSION_FACTOR);
    analogWrite(B_PIN, (v.z / 255.0) * OUTPUT_CONVERSION_FACTOR);

      // wait fot the transition delay
    v.x += dx;
    v.y += dy;
    v.z += dz;
    
  delayLed = TRANSITION_DELAY;
  ledIterT++;
  }
  Serial.printf("v.x %d v.y %d v.z %d\n", v.x, v.y, v.z);
  return false;
}


Encoder knob(ENCODER_PIN_1, ENCODER_PIN_2);

enum PACKET_TYPE {
    Ping = 'p',
    UpdateSlider = 's',
    RunSelfTest = 't',
    SelectLed = 'l'
};

enum LED_MODE {
    Impress,
    Utility
};

LED_MODE SELECTED_LED_MODE = Impress;

void setup() {
  Joystick.useManualSend(false);
  for(int i = 0; i < NUM_BUTTONS; i++) {
    pinMode(BUTTON_PORTS[i], INPUT_PULLUP);
  }
  pinMode(AI1, OUTPUT);
  pinMode(AI2, OUTPUT);
  pinMode(PWMA, OUTPUT);
  pinMode(ENCODER_RESET_BUTTON, INPUT);
  pinMode(R_PIN, OUTPUT);
  pinMode(G_PIN, OUTPUT);
  pinMode(B_PIN, OUTPUT);
  Serial.begin(115200);
  analogReadResolution(READ_RESOLUTION);
  analogWriteResolution(WRITE_RESOLUTION);

  lastTime = micros();
  serialSetpoint = 1.0 / 0.0;
  knob.write(0);
}

void loop() {

  if(isnan(setpoint)) {
    setpoint = 1;
  }

  int serialValue = Serial.read();

  if(serialValue != -1) {
  Serial.printf("Recieved packet: %c\n", serialValue);
  PACKET_TYPE packet = (PACKET_TYPE)serialValue;
  switch(packet) {
        case Ping: {
            Serial.printf("~2122~^%.2f\n", VERSION);  
        }
        break;
        case UpdateSlider: {
            double val = Serial.parseFloat();
            serialSetpoint = val;
            Serial.printf("setting setpoint to %f\n", val);
            enabled = true;
        }
        break;
        case RunSelfTest: {
            int in = Serial.parseInt();
            TEST_ITER = in == 0 ? 10 : in;
            Serial.printf("Running self-test mode, %d iterations\n", TEST_ITER);
            testing = !testing;
        }
        /*case SelectLedMode: {
            char
        }*/
        break;
        
  }
}
  double delta = (micros() - lastTime) / 1000000.0;
  #ifdef ENABLE_LOGGING
  Serial.printf("Delta: %.4f\n", delta);
  #endif
  //Buttons
  for(int i = 0; i < NUM_BUTTONS; i++) {
    Joystick.button(i, digitalRead(BUTTON_PORTS[i]));
  }
  double sliderValRaw = analogRead(LIFT_SLIDER);
  double sliderVal = sliderValRaw / INPUT_CONVERSION_FACTOR;

  if(true /*and more future conditions*/) {
      //Serial.println("Possible move to detent!");
      int nearestDetent = 0;
      double bestDistance = 1;
      boolean setDetent = false;
      for(int i = 0; i < DETENTS_LENGTH; i++) {
        double distance = DETENTS[i] - sliderVal;
        if(abs(distance) < bestDistance){
          nearestDetent = i;
          bestDistance = abs(distance);
        }
        if(abs(distance) < 0.015) {
          setDetent = true;
        }
      }
      if(setDetent) {
        detentSetpoint = DETENTS[nearestDetent];
        #ifdef ENABLE_LOGGING
        Serial.println("Found close detent!");
        #endif
      } else {
        #ifdef ENABLE_LOGGING
        Serial.println("No close detents!");
        #endif
        detentSetpoint = 1.0 / 0.0;
      }
  }
  double errorVal;

  if(abs(serialSetpoint - sliderVal) <= ERROR_MARGIN) {
     #ifdef ENABLE_LOGGING
     Serial.println("Resetting serial setpoint");
     #endif
     serialSetpoint = 1.0 / 0.0;
  }
  
  if(!isinf(serialSetpoint) && abs(serialSetpoint - sliderVal) > ERROR_MARGIN) {
    #ifdef ENABLE_LOGGING
    Serial.println("Using serial setpoint");
    #endif
    errorVal = serialSetpoint - sliderVal;

  } else if(!isinf(detentSetpoint) && abs(detentSetpoint - sliderVal) > ERROR_MARGIN) {
    #ifdef ENABLE_LOGGING
    Serial.println("Using detent setpoint");
    #endif
    errorVal = detentSetpoint - sliderVal;
    serialSetpoint = 1.0 / 0.0;
  } else {
    errorVal = 0;
  }
  
  if(enabled) {
    double output = errorVal * kP;

    totalError += errorVal * delta;  

    output += kI * totalError;

    output += kD * (sliderVal - lastValue) / delta;

    if(isnan(output)) {
      output = 0;
      totalError = 0;
    }

    if (output > maxOutput) {
      output = maxOutput;
    }
    if (output < -maxOutput) {
      output = -maxOutput;
    }

    if(abs(errorVal) <= 0.003) {
      output = 0;
      if(testing) {
      if(tests == TEST_ITER) {
      Serial.printf("Test took %d micros\n",micros() - testStarted); 
      testing = false;
      tests = 0;
      testStarted = -1;
      }
      if(testStarted != -1 && tests != 0) {
        Serial.printf("Test took %d micros\n",micros() - testStarted); 
      }
      if(testing)
      Serial.printf("Running test #%d\n", ++tests);
      serialSetpoint = random(0, OUTPUT_CONVERSION_FACTOR) / OUTPUT_CONVERSION_FACTOR;
      enabled = true;
      testStarted = micros();
      }
    }
    #ifdef ENABLE_LOGGING
    Serial.printf("%.3f,%.3f,%.3f\n", output, sliderVal, setpoint);
    #endif
    driveMotor(output);
  } else {
    totalError = 0;
    driveMotor(0);
  }
  lastError = errorVal;
  lastValue = sliderVal;
  lastPosition = sliderVal;

  Joystick.X(sliderVal * 1023);

  if(digitalRead(ENCODER_RESET_BUTTON) > 0) {
    knob.write(0);
    //Serial.println("reset");
  }
  /*if(knob.read() > 24) {
    knob.write(-23);
  }
  if(knob.read() < -24) {
    knob.write(23);
  }
  */
  int knobV = knob.read();
  Joystick.Y((knobV / CPR + 0.5) * 1023);
  int powerSlider = analogRead(POWER_SLIDER);
  //Serial.println(powerSlider);
  Joystick.Z(((INPUT_CONVERSION_FACTOR - powerSlider) / INPUT_CONVERSION_FACTOR) * 1023);
  Joystick.send_now();
  lastTime = micros();

  //switch(SELECTED_LED_MODE) {
    //case Impress: {
        // the new vertex and the previous one

  // initialise the place we start from as the first vertex in the array
  if(ledIter >= 2*MAX_PATH_SIZE) {
    Serial.println("REINIT");
    v1, v2 = 0;
    v.x = (vertex[v2].x ? MAX_RGB_VALUE : MIN_RGB_VALUE);
    v.y = (vertex[v2].y ? MAX_RGB_VALUE : MIN_RGB_VALUE);
    v.z = (vertex[v2].z ? MAX_RGB_VALUE : MIN_RGB_VALUE);
    firstRun = true;
    ledIter = 0;
  }

  if(ledIterT >= MAX_RGB_VALUE-MIN_RGB_VALUE || firstRun) {
    v1 = v2;
    if (ledIter&1)  // odd number is the second element and ...
      v2 = path[ledIter>>1] & 0xf;  // ... the bottom nybble (index /2) or ...
    else      // ... even number is the first element and ...
      v2 = path[ledIter>>1] >> 4;  // ... the top nybble
    firstRun = false;
  }
  
  if(traverse(vertex[v2].x-vertex[v1].x,
             vertex[v2].y-vertex[v1].y,
             vertex[v2].z-vertex[v1].z,
             delta))
    ledIter++;
    //};
    //break;

  //}


  delay(5);
}


void driveMotor(double speed) {
  if (speed >= 0) {
    digitalWrite(AI1, LOW);
    digitalWrite(AI2, HIGH);
    analogWrite(PWMA, speed * OUTPUT_CONVERSION_FACTOR);
  } else {
    digitalWrite(AI1, HIGH);
    digitalWrite(AI2, LOW);
    analogWrite(PWMA, -speed * OUTPUT_CONVERSION_FACTOR);
  }
}

bool signOf(double value) {
  return value > 0 ? 1 : value < 0 ? -1 : 0;
}


