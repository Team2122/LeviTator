package org.teamtators.levitator.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.SolenoidConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.config.helpers.SpeedControllerGroupConfig;
import org.teamtators.common.control.MotorPowerUpdater;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.hw.SpeedControllerGroup;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SolenoidTest;
import org.teamtators.common.tester.components.SpeedControllerTest;
import org.teamtators.levitator.TatorRobot;

import java.util.Arrays;
import java.util.List;

public class Climber extends Subsystem implements Configurable<Climber.Config> {
    private final TatorRobot robot;
    private SpeedControllerGroup climberMotor;
    private WPI_TalonSRX masterMotor;
//    private MotorPowerUpdater climberMotorUpdater;
    private DigitalSensor topLimit;
    private DigitalSensor bottomLimit;
    private Solenoid releaser;
    private Config config;
    private boolean homed;

    public Climber(TatorRobot robot) {
        super("Climber");
        this.robot = robot;
    }

    public void setHomed(boolean homed) {
        this.homed = homed;
    }

    public boolean isHomed() {
        return homed;
    }

    public void setPower(double power) {
        double pow = power;
        if (isAtTopLimit() && power > 0) {
            logger.warn("Attempted to give climber power {} at top limit", power);
            pow = 0;
        }
        if (isAtBottomLimit() && power < 0) {
            logger.warn("Attempted to give climber power {} at bottom limit", power);
            pow = 0;
        }
        climberMotor.set(pow);
    }

    public double getPosition() {
        return masterMotor.getSelectedSensorPosition(0) / 1024.0 * config.distancePerPulse;
    }

    public boolean isAtBottomLimit() {
        return bottomLimit.get();
    }

    public boolean isAtTopLimit() {
        return !topLimit.get();
    }

    public void resetPosition() {
        masterMotor.setSelectedSensorPosition(0, 0, 0);
    }

    public void release() {
        releaser.set(true);
    }

    public void unrelease() {
        releaser.set(false);
    }

    public List<MotorPowerUpdater> getMotorUpdatables() {
        return Arrays.asList(/*climberMotorUpdater*/);
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        if (!homed) {
            Command homeCommand = robot.getCommandStore().getCommand("ClimberHome");
            if (homeCommand != null && !homeCommand.isRunning()) {
                robot.getScheduler().startCommand(homeCommand);
            }
        }
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        climberMotor = config.climberMotor.create();
        masterMotor = (WPI_TalonSRX) climberMotor.getSpeedControllers()[0];
        topLimit = config.topLimit.create();
        bottomLimit = config.bottomLimit.create();
        releaser = config.releaser.create();
//        climberMotorUpdater = new MotorPowerUpdater(climberMotor);

        climberMotor.setName("Climber", "climberMotor");
        for (int i = 0; i < climberMotor.getSpeedControllers().length; i++) {
            SpeedController speedController = climberMotor.getSpeedControllers()[i];
            ((Sendable) speedController).setName("Climber", ("climberMotor(" + i + ")"));
        }
        topLimit.setName("Climber", "topLimit");
        bottomLimit.setName("Climber", "bottomLimit");
        releaser.setName("Climber", "releaser");

        homed = false;
    }

    @Override
    public void deconfigure() {
        SpeedControllerConfig.free(climberMotor);
        topLimit.free();
        bottomLimit.free();
        releaser.free();
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup group = super.createManualTests();
        group.addTest(new ClimberEncoderTest());
        group.addTest(new SpeedControllerTest("climberMotor", climberMotor));
        for (int i = 0; i < climberMotor.getSpeedControllers().length; i++) {
            SpeedController speedController = climberMotor.getSpeedControllers()[i];
            group.addTest(new SpeedControllerTest("climberMotor(" + i + ")", speedController));
        }
        group.addTest(new DigitalSensorTest("topLimit", topLimit));
        group.addTest(new DigitalSensorTest("bottomLimit", bottomLimit));
        group.addTest(new SolenoidTest("releaser", releaser));
        return group;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Config {
        public SpeedControllerGroupConfig climberMotor;
        public double distancePerPulse;
        public DigitalSensorConfig topLimit;
        public DigitalSensorConfig bottomLimit;
        public SolenoidConfig releaser;
    }

    public class ClimberEncoderTest extends ManualTest {
        ClimberEncoderTest() {
            super("ClimberEncoderTest");
        }

        @Override
        public void start() {
            super.start();
            printTestInstructions("Press A to read the encoder value. Press B to reset the encoder value.");
            setPower(0.0);
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    printTestInfo("Encoder value: {}", getPosition());
                    break;
                case B:
                    printTestInfo("Reset encoder.");
                    resetPosition();
                    break;
                case X:
                    setPower(1.0);
                    break;
                case Y:
                    setPower(-1.0);
                    break;
            }
        }

        @Override
        public void onButtonUp(LogitechF310.Button button) {

            switch (button) {
                case X:
                case Y:
                    setPower(0.0);
                    break;
            }
        }

        @Override
        public void stop() {
            setPower(0.0);
        }
    }
}
