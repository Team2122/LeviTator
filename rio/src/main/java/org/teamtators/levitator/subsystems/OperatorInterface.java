package org.teamtators.levitator.subsystems;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;

public class OperatorInterface extends Subsystem implements Configurable<OperatorInterface.Config> {
    private LogitechF310 driverJoystick = new LogitechF310();
    private LogitechF310 gunnerJoystick = new LogitechF310();
    private Config config;

    public OperatorInterface() {
        super("Operator Interface");
    }

    // For tank drive
    public double getDriveLeft() {
        return -driverJoystick.getAxisValue(LogitechF310.Axis.LEFT_STICK_Y);
    }

    public double getDriveRight() {
        return -driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_Y);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        driverJoystick.configure(config.driverJoystick);
        gunnerJoystick.configure(config.gunnerJoystick);
    }

    public LogitechF310 getDriverJoystick() {
        return driverJoystick;
    }

    public LogitechF310 getGunnerJoystick() {
        return gunnerJoystick;
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup group = super.createManualTests();
        group.addTest(new OITest());
        return group;
    }

    public static class Config {
        public LogitechF310.Config driverJoystick;
        public LogitechF310.Config gunnerJoystick;
    }

    private class OITest extends ManualTest {
        public OITest() {
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
