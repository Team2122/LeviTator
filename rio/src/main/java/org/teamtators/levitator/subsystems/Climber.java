package org.teamtators.levitator.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.TalonSRXConfig;
import org.teamtators.common.config.helpers.VictorSPXConfig;
import org.teamtators.common.control.MotorPowerUpdater;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

import java.util.Arrays;
import java.util.List;

public class Climber extends Subsystem implements Configurable<Climber.Config> {
    private WPI_TalonSRX climberMotor;
    private WPI_VictorSPX slaveMotor;
    private MotorPowerUpdater climberMotorUpdater;
    private DigitalSensor topLimit;
    private DigitalSensor bottomLimit;
    private Config config;

    public Climber() {
        super("Climber");
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
        climberMotorUpdater.set(pow);
    }

    public double getPosition() {
        return climberMotor.getSelectedSensorPosition(0) * config.distancePerPulse;
    }

    public boolean isAtBottomLimit() {
        return bottomLimit.get();
    }

    public boolean isAtTopLimit() {
        return topLimit.get();
    }

    public void resetPosition() {
        climberMotor.setSelectedSensorPosition(0, 0, 0);
    }

    public List<MotorPowerUpdater> getMotorUpdatables() {
        return Arrays.asList(climberMotorUpdater);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        climberMotor = config.climberMotor.create();
        slaveMotor = config.slaveMotor.create();
        topLimit = config.topLimit.create();
        bottomLimit = config.bottomLimit.create();
        slaveMotor.set(ControlMode.Follower, config.climberMotor.id);

        climberMotorUpdater = new MotorPowerUpdater(climberMotor);

    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup group = super.createManualTests();
        group.addTest(new ClimberEncoderTest());
        group.addTest(new SpeedControllerTest("climberMotor", climberMotor));
        group.addTest(new DigitalSensorTest("topLimit", topLimit));
        group.addTest(new DigitalSensorTest("bottomLimit", bottomLimit));
        return group;
    }

    public static class Config {
        public TalonSRXConfig climberMotor;
        public VictorSPXConfig slaveMotor;
        public double distancePerPulse;
        public DigitalSensorConfig topLimit;
        public DigitalSensorConfig bottomLimit;

    }

    public class ClimberEncoderTest extends ManualTest {
        public ClimberEncoderTest() {
            super("ClimberEncoderTest");
        }

        @Override
        public void start() {
            super.start();
            printTestInstructions("Press A to read the encoder value. Press B to reset the encoder value.");
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
            }
        }
    }
}
