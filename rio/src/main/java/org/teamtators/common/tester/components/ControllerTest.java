package org.teamtators.common.tester.components;

import org.teamtators.common.control.AbstractController;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class ControllerTest extends ManualTest {
    private double minSetpoint;
    private AbstractController controller;
    private double maxSetpoint;

    public ControllerTest(AbstractController controller, double maxSetpoint, double minSetpoint) {
        super(controller.getName());
        this.controller = controller;
        this.maxSetpoint = maxSetpoint;
        this.minSetpoint = minSetpoint;
    }

    public ControllerTest(AbstractController controller, double maxSetpoint) {
        this(controller, maxSetpoint, -maxSetpoint);
    }

    public ControllerTest(AbstractController controller) {
        this(controller, controller.getMaxSetpoint(), controller.getMinSetpoint());
    }


    @Override
    public void start() {
        printTestInstructions("Testing {} {} (setpoint range = [{}, {}])",
                controller.getClass().getSimpleName(), controller.getName(), minSetpoint, maxSetpoint);
        printTestInstructions("Press A to disable, B to enable, X to get information and joystick to set setpoint");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case A:
                disableController();
                printTestInfo("Disabled controller");
                break;
            case B:
                enableController();
                printTestInfo("Enabled controller");
                break;
            case X:
                printTestInfo("Input = {}, Setpoint = {}, Output = {}, On Target = {}", controller.getInput(),
                        controller.getSetpoint(), controller.getOutput(), controller.isOnTarget());
                break;
        }
    }

    protected void enableController() {
        controller.start();
    }

    protected void disableController() {
        controller.stop();
    }

    @Override
    public void updateAxis(double value) {
        double x = (1 - value) / 2.0;
        double output = minSetpoint + x * (maxSetpoint - minSetpoint);
        controller.setSetpoint(output);
    }

    @Override
    public void stop() {
        disableController();
    }
}
