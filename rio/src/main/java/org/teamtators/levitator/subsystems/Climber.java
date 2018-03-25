package org.teamtators.levitator.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.Solenoid;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.*;
import org.teamtators.common.control.MotorPowerUpdater;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SolenoidTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

import java.util.Arrays;
import java.util.List;

public class Climber extends Subsystem implements Configurable<Climber.Config> {
    private WPI_TalonSRX climberMotor;
    private WPI_VictorSPX slaveMotor;
//    private MotorPowerUpdater climberMotorUpdater;
    private DigitalSensor topLimit;
    private DigitalSensor bottomLimit;
    private Solenoid releaser;
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
        climberMotor.set(pow);
        slaveMotor.follow(climberMotor);
    }

    public double getPosition() {
        return climberMotor.getSensorCollection().getQuadraturePosition() / 1024.0 * config.distancePerPulse;
    }

    public boolean isAtBottomLimit() {
        return bottomLimit.get();
    }

    public boolean isAtTopLimit() {
        return !topLimit.get();
    }

    public void resetPosition() {
        climberMotor.getSensorCollection().setQuadraturePosition(0, 0);
    }

    public void release() {
        releaser.set(true);
    }

    public void retract() {
        releaser.set(false);
    }

    public List<MotorPowerUpdater> getMotorUpdatables() {
        return Arrays.asList(/*climberMotorUpdater*/);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        climberMotor = config.climberMotor.create();
        slaveMotor = config.slaveMotor.create();
        topLimit = config.topLimit.create();
        bottomLimit = config.bottomLimit.create();
        releaser = config.releaser.create();
//        climberMotorUpdater = new MotorPowerUpdater(climberMotor);

        climberMotor.setName("Climber", "climberMotor");
        slaveMotor.setName("Climber", "slaveMotor");
        topLimit.setName("Climber", "topLimit");
        bottomLimit.setName("Climber", "bottomLimit");
        releaser.setName("Climber", "releaser");
    }

    @Override
    public void deconfigure() {
        SpeedControllerConfig.free(climberMotor);
        SpeedControllerConfig.free(slaveMotor);
        topLimit.free();
        bottomLimit.free();
        releaser.free();
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup group = super.createManualTests();
        group.addTest(new ClimberEncoderTest());
        group.addTest(new SpeedControllerTest("climberMotor", climberMotor));
        group.addTest(new SpeedControllerTest("slaveMotor", slaveMotor));
        group.addTest(new DigitalSensorTest("topLimit", topLimit));
        group.addTest(new DigitalSensorTest("bottomLimit", bottomLimit));
        group.addTest(new SolenoidTest("releaser", releaser));
        return group;
    }

    public static class Config {
        public TalonSRXConfig climberMotor;
        public VictorSPXConfig slaveMotor;
        public double distancePerPulse;
        public DigitalSensorConfig topLimit;
        public DigitalSensorConfig bottomLimit;
        public SolenoidConfig releaser;
    }

    public class ClimberEncoderTest extends ManualTest {
        public ClimberEncoderTest() {
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
