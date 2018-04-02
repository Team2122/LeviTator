package org.teamtators.common.tester.components;

import edu.wpi.first.wpilibj.VictorSP;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class VictorSPTest extends ManualTest {

    private VictorSP motor;
    private int fullSpeed;
    private double axisValue;

    public VictorSPTest(String name, VictorSP motor) {
        super(name);
        this.motor = motor;
    }

    @Override
    public void start() {
        printTestInstructions("Push joystick in direction to move, where forward is positive");
        printTestInstructions("Press back/add to drive backward/forward at full speed");
        fullSpeed = 0;
        axisValue = 0;
    }

    private double getSpeed() {
        if (fullSpeed != 0) {
            return fullSpeed;
        } else {
            return axisValue;
        }
    }

    @Override
    public void update(double delta) {
        motor.set(getSpeed());
    }

    @Override
    public void stop() {
        motor.set(0);
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if (button == LogitechF310.Button.BACK) fullSpeed--;
        else if (button == LogitechF310.Button.START) fullSpeed++;
    }

    @Override
    public void onButtonUp(LogitechF310.Button button) {
        if (button == LogitechF310.Button.BACK) fullSpeed++;
        else if (button == LogitechF310.Button.START) fullSpeed--;
    }

    @Override
    public void updateAxis(double value) {
        axisValue = value;
    }
}
