package org.teamtators.levitator.subsystems;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.controllers.ButtonBoardFingers;
import org.teamtators.common.controllers.Controller;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.controllers.RawController;
import org.teamtators.common.math.LinearInterpolationFunction;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;

import java.util.Arrays;
import java.util.List;

public class OperatorInterface extends Subsystem implements Configurable<OperatorInterface.Config> {
    private LogitechF310 driverJoystick = new LogitechF310("driver");
    private ButtonBoardFingers gunnerJoystick = new ButtonBoardFingers("gunner");
    private RawController gunnerSecondary = new RawController("gunnerSecondary");
    private RawController slider = new RawController("slider");
    private List<Controller<?, ?>> controllers;
    private Config config;

    OperatorInterface() {
        super("Operator Interface");
    }

    // For tank drive
    public double getDriveLeft() {
        return -driverJoystick.getAxisValue(LogitechF310.Axis.LEFT_STICK_Y);
    }

    public double getDriveRight() {
        return -driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_Y);
    }

    private double getSliderValueRaw() {
        return slider.getRawAxisValue(0);
    }

    public double getSliderHeight() {
        return config.sliderFunction.calculate(getSliderValueRaw());
    }

    private double getPivotKnobRaw() {
        return slider.getRawAxisValue(1);
    }

    public double getPivotKnob() {
        double knobAngle = getPivotKnobRaw() * config.knobRange;
        if (Math.abs(knobAngle) < config.knobDeadZone) {
            return 0;
        }
        return knobAngle;
    }

    public double getReleasePower() {
        return slider.getRawAxisValue(2);
    }

    @Override
    public void configure(Config config) {
        super.configure();
        this.config = config;
        driverJoystick.configure(config.driverJoystick);
        gunnerJoystick.configure(config.gunnerJoystick);
        gunnerSecondary.configure(config.gunnerSecondary);
        slider.configure(config.slider);

        controllers = Arrays.asList(
                driverJoystick,
                gunnerJoystick,
                gunnerSecondary,
                slider
        );
    }

    public LogitechF310 getDriverJoystick() {
        return driverJoystick;
    }

    public List<Controller<?, ?>> getAllControllers() {
        return controllers;
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup group = super.createManualTests();
        group.addTest(new OITest());
        return group;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Config {
        public LogitechF310.Config driverJoystick;
        public ButtonBoardFingers.Config gunnerJoystick;
        public RawController.Config gunnerSecondary;
        public RawController.Config slider;
        public double knobRange;
        public double knobDeadZone;
        public LinearInterpolationFunction sliderFunction;
    }

    private class OITest extends ManualTest {
        OITest() {
            super("OITest");
        }

        @Override
        public void start() {
            printTestInstructions("Press A to get statuses");
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            printTestInfo("Tank: Left = {}, Right = {}", getDriveLeft(), getDriveRight());
        }
    }
}
