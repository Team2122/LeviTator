package org.teamtators.common.tester.components;

import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class DigitalSensorTest extends ManualTest {

    private DigitalSensor digitalSensor;

    public DigitalSensorTest(String name, DigitalSensor digitalSensor) {
        super(name);
        this.digitalSensor = digitalSensor;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to get the value and type from the sensor");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if (button == LogitechF310.Button.A) {
            printTestInfo("Digital sensor value {} (type {})", digitalSensor.get(), digitalSensor.getType());
        }
    }
}
