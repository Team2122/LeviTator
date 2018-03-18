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

#define ENABLE_LOGGING
//#define SELF_TEST
#define READ_RESOLUTION 13
#define WRITE_RESOLUTION 12
#define ERROR_MARGIN 0.003

const int NUM_BUTTONS = 0;
const double HEIGHT_PRESETS[] = {0, .5, 1};
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

void setup() {
  // put your setup code here, to run once:
  Joystick.useManualSend(false);
  for(int i = 0; i < NUM_BUTTONS; i++) {
    pinMode(i, INPUT_PULLUP);
  }
  pinMode(AI1, OUTPUT);
  pinMode(AI2, OUTPUT);
  pinMode(PWMA, OUTPUT);
  Serial.begin(9600);
  analogReadResolution(READ_RESOLUTION);
  analogWriteResolution(WRITE_RESOLUTION);
}

void loop() {

  if(isnan(setpoint)) {
    setpoint = 1;
  }

  int serialValue = Serial.read();

  if(serialValue != -1) {
    serialSetpoint = HEIGHT_PRESETS[serialValue - 48];
    enabled = true;
  }
  double delta = .005; 
  //Buttons
  for(int i = 0; i < NUM_BUTTONS; i++) {
    Joystick.button(i, digitalRead(i));
  }
  double sliderValRaw = analogRead(LIFT_SLIDER);
  double sliderVal = sliderValRaw / ((1 << READ_RESOLUTION) - 1);
  //Serial.println(sliderVal);

  if(true /*and more future conditions*/) {
      //Serial.println("Possible move to detent!");
      int nearestDetent = 0;
      double bestDistance = 1;
      boolean setDetent = false;
      for(int i = 0; i < DETENTS_LENGTH; i++) {
        double distance = DETENTS[i] - sliderVal;
        if(abs(distance) < bestDistance /*&& signOf(distance) == signOf(errorVal - lastError)*/){
          nearestDetent = i;
          bestDistance = abs(distance);
        }
        if(abs(distance) < 0.02) {
          setDetent = true;
        }
      }
      if(setDetent) {
        detentSetpoint = DETENTS[nearestDetent];
        Serial.println("Found close detent!");
      } else {
        Serial.println("No close detents!");
        detentSetpoint = 1.0 / 0.0;
      }
    }
  double errorVal;

    if(abs(serialSetpoint - sliderVal) <= ERROR_MARGIN) {
      Serial.println("Resetting serial setpoint");
      serialSetpoint = 1.0 / 0.0;
    }
  
  if(!isinf(serialSetpoint) && abs(serialSetpoint - sliderVal) > ERROR_MARGIN) {
    Serial.println("Using serial setpoint");
    errorVal = serialSetpoint - sliderVal;

  } else if(!isinf(detentSetpoint) && abs(detentSetpoint - sliderVal) > ERROR_MARGIN) {
    Serial.println("Using detent setpoint");
    errorVal = detentSetpoint - sliderVal;
    serialSetpoint = 1.0 / 0.0;
  } else {
    errorVal = 0;
  }

  Serial.println(serialSetpoint);
  
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
      Serial.println("On target!");
      output = 0;
    } else {
   }
    
#ifdef ENABLE_LOGGING
    //Serial.print("Writing value: ");
    Serial.print(output);
    //Serial.print(" Actual slider value: ");
    Serial.print(",");
    Serial.print(sliderVal);
    Serial.print(",");
    //Serial.print(" Target slider value: ");
    Serial.println(setpoint);
#endif
    driveMotor(output);
  } else {
    totalError = 0;
    driveMotor(0);
#ifdef SELF_TEST
    //setpoint = random(0, 8191);
    enabled = true;
    tests++;
#endif
    lastError = errorVal;
  }

  
  lastValue = sliderVal;
  lastPosition = sliderVal;

  Joystick.X(sliderVal * 1023);
  Joystick.send_now();
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

