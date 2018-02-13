package org.teamtators.common.tester.components;

import org.teamtators.common.hw.DistanceLaser;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class DistanceLaserTest extends ManualTest {
    private DistanceLaser distanceLaser;

    public DistanceLaserTest(String name, DistanceLaser distanceLaser) {
        super(name);
        this.distanceLaser = distanceLaser;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to get the Distance and Voltage of the DistanceLaser");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if (button == LogitechF310.Button.A) {
            printTestInfo("Distance Laser ballDistance: {}; voltage: {}", distanceLaser.getDistance(), distanceLaser.getVoltage());
        }
    }
}
