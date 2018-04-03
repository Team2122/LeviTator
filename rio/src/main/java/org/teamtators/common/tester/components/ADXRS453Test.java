package org.teamtators.common.tester.components;

import org.teamtators.common.hw.ADXRS453;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class ADXRS453Test extends ManualTest {
    private ADXRS453 gyro;

    public ADXRS453Test(String name, ADXRS453 gyro) {
        super(name);
        this.gyro = gyro;
    }

    @Override
    public void start() {
        printTestInstructions("Press A to get the rate, the angle, and the calibration offset of the gyro");
        printTestInstructions("B to start calibration, and Y to end calibration");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case A:
                double angle = gyro.getAngle();
                double rate = gyro.getRate();
                double calibrationOffset = gyro.getCalibrationOffset();
                printTestInfo("Gyro Angle: {}, Rate: {} (Offset: {})", angle, rate, calibrationOffset);
                break;
            case B:
                printTestInfo("Starting gyro calibration");
                gyro.startCalibration();
                break;
            case Y:
                printTestInfo("Finishing gyro calibration");
                gyro.finishCalibration();
                break;
        }
    }

    @Override
    public void update(double delta) {
    }
}
