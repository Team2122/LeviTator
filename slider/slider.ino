/*********************************************************

Buttonboard Slider

AUTHOR: Avery Bainbridge
DATE: 3/17/2018

**********************************************************/

#define AI1  21
#define AI2  22
#define PWMA 23

#define TOUCH_SENSOR 3
#define LIFT_SLIDER 5
#define DEBUG
#ifdef DEBUG
#define ENABLE_LOGGING
//#define SELF_TEST
#endif
#define READ_RESOLUTION 13
#define WRITE_RESOLUTION 12
#define ERROR_MARGIN 0.003

const int BUTTON_PORTS[] = {};
const int NUM_BUTTONS = 0;
const double DETENTS[] = {0, .125, .25, .375, .5, .625, .75, .875, 1};
const int DETENTS_LENGTH = 9;

//PID values
const double kP = 95;
const double kI = 0;
const double kD = 0;

const double maxOutput = ((1 << WRITE_RESOLUTION) - 1);

const double OUTPUT_CONVERSION_FACTOR = maxOutput;

double setpoint;
double serialSetpoint;
double detentSetpoint;
double lastValue;
double totalError;
double lastError;
double lastPosition;

const int TEST_ITER = 10;
int tests = 0;
boolean enabled = true;
boolean finished;

long lastTime;

void setup() {
  // put your setup code here, to run once:
  Joystick.useManualSend(false);
  for(int i = 0; i < NUM_BUTTONS; i++) {
    pinMode(BUTTON_PORTS[i], INPUT_PULLUP);
  }
  pinMode(AI1, OUTPUT);
  pinMode(AI2, OUTPUT);
  pinMode(PWMA, OUTPUT);
  Serial.begin(115200);
  analogReadResolution(READ_RESOLUTION);
  analogWriteResolution(WRITE_RESOLUTION);

  lastTime = millis();
}

void loop() {

  if(isnan(setpoint)) {
    setpoint = 1;
  }

  int serialValue = Serial.read();

  if(serialValue != -1) {
    //serialSetpoint = HEIGHT_PRESETS[serialValue - 48];
    union { char serial[8]; double val; };
    serial[7] = serialValue;


    int serialBytesRecieved = 1;

    while(serialBytesRecieved < 8) {
      while(Serial.available() > 0) {
        serial[++serialBytesRecieved] = Serial.read();
      }
    }
    serialSetpoint = val;
    enabled = true;
  }
  double delta = (millis() - lastTime) / 1000.0; 
  #ifdef ENABLE_LOGGING
  Serial.printf("Delta: %.4f\n", delta);
  #endif
  //Buttons
  for(int i = 0; i < NUM_BUTTONS; i++) {
    Joystick.button(i, digitalRead(BUTTON_PORTS[i]));
  }
  double sliderValRaw = analogRead(LIFT_SLIDER);
  double sliderVal = sliderValRaw / ((1 << READ_RESOLUTION) - 1);

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
        if(abs(distance) < 0.02) {
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
  
  if(enabled && tests <= TEST_ITER) {
    double output = errorVal * kP;

    totalError += errorVal * delta;  

    output += kI * totalError;

    output += kD * (sliderVal - lastValue) / delta;

    if(isnan(output)) {
      output = 0;
      totalError = 0;
    }

    if(abs(errorVal) <= 0.003) {
      output = 0;
    }
    
#ifdef ENABLE_LOGGING
    Serial.printf("%.3f,%.3f,%.3f\n", output, sliderVal, setpoint);
#endif
    driveMotor(output);
  } else {
    totalError = 0;
    driveMotor(0);
#ifdef SELF_TEST
    setpoint = random(0, OUTPUT_CONVERSION_FACTOR) / OUTPUT_CONVERSION_FACTOR;
    enabled = true;
    tests++;
#endif
    lastError = errorVal;
  }

  
  lastValue = sliderVal;
  lastPosition = sliderVal;

  Joystick.X(sliderVal * 1023);
  Joystick.send_now();
  lastTime = millis();
  delay(5);
}


void driveMotor(double speed) {
  if(speed >= 0) {
    digitalWrite(AI1, HIGH);
    digitalWrite(AI2, LOW);
    analogWrite(PWMA, speed * OUTPUT_CONVERSION_FACTOR);
  } else {
    digitalWrite(AI1, LOW);
    digitalWrite(AI2, HIGH);
    analogWrite(PWMA, -speed * OUTPUT_CONVERSION_FACTOR);
  }
}

bool signOf(double value) {
  return value > 0 ? 1 : value < 0 ? -1 : 0;
}

