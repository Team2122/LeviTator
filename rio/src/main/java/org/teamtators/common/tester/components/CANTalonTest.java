package org.teamtators.common.tester.components;

import com.ctre.CANTalon;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class CANTalonTest extends ManualTest {

    private final CANTalon controller;
    private final double maxSpeed;
    private CANTalon.TalonControlMode originalControlMode;
    private double axisValue;

    public CANTalonTest(String name, CANTalon controller, double maxSpeed) {
        super(name);
        this.controller = controller;
        originalControlMode = controller.getControlMode();
        this.maxSpeed = maxSpeed;
    }

    @Override
    public void start() {
        printTestInstructions("Push joystick in direction to move (forward +, backward -), A to print info, B to switch control modes");
        axisValue = 0;
        controller.enable();
    }

    private double getSpeed() {
        return axisValue;
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        CANTalon.TalonControlMode controlMode = controller.getControlMode();
        switch (button) {
            case A:
                double speed = controller.getSpeed();
                double setpoint = controller.getSetpoint();
                double output = controller.getOutputVoltage() / controller.getBusVoltage();
                printTestInfo("Speed=" + speed + ", Setpoint=" + setpoint + ", Output=" + output);
                break;
            case B:
                if (controlMode == originalControlMode) {
                    printTestInfo("Switching to voltage (% Vbus) mode");
                    controller.changeControlMode(CANTalon.TalonControlMode.PercentVbus);
                } else {
                    printTestInfo("Switching to original mode (" + originalControlMode + ")");
                    controller.changeControlMode(originalControlMode);
                }
                break;
        }
    }

    @Override
    public void update(double delta) {
        CANTalon.TalonControlMode controlMode = controller.getControlMode();
        if (controlMode.isPID()) {
            controller.set(axisValue * maxSpeed);
        } else {
            controller.set(axisValue);
        }
    }

    @Override
    public void stop() {
        controller.setSetpoint(0);
        controller.reset();
//        controller.disable();
    }

    @Override
    public void updateAxis(double value) {
        axisValue = value;
    }
}
