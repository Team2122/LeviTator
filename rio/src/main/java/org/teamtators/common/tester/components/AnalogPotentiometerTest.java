package org.teamtators.common.tester.components;

import org.teamtators.common.hw.AnalogPotentiometer;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class AnalogPotentiometerTest extends ManualTest {
    private AnalogPotentiometer analogPotentiometer;
    private double originalOffset;

    public AnalogPotentiometerTest(String name, AnalogPotentiometer analogPotentiometer) {
        super(name);
        this.analogPotentiometer = analogPotentiometer;
        this.originalOffset = analogPotentiometer.getOffset();
    }

    @Override
    public void start() {
        printTestInstructions("Press A to get the potentiometer value. B to get scale and offset");
        printTestInstructions("X to reset offset and Y to apply original offset.");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case A:
                double value = analogPotentiometer.get();
                double voltage = analogPotentiometer.getRawVoltage();
                printTestInfo("Value: {} (voltage: {}V)", value, voltage);
                break;
            case X:
                analogPotentiometer.setOffset(0);
                printTestInfo("Reset offset to 0");
                break;
            case Y:
                analogPotentiometer.setOffset(originalOffset);
                printTestInfo("Set offset to original value of {}", originalOffset);
                break;
            case B:
                double offset = analogPotentiometer.getOffset();
                double scale = analogPotentiometer.getFullRange();
                printTestInfo("Offset: {}, Full Range: {}", offset, scale);
                break;
        }
    }
}
