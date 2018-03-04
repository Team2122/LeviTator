package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.DoubleSolenoidConfig;
import org.teamtators.common.config.helpers.SolenoidConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.DoubleSolenoidTest;
import org.teamtators.common.tester.components.SolenoidTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

public class Elevators extends Subsystem implements Configurable<Elevators.Config> {
    private Solenoid rightElevatorSolenoid;
    private Solenoid releaser;
    private DoubleSolenoid elevatorSlideSolenoid;
    private SpeedController leftFlap;
    private DigitalSensor leftFlapSensor;

    private boolean isDeployed;

    private Config config;

    public Elevators() {
        super("Elevators");
    }

    public void reset() {
        rightElevatorSolenoid.set(false);
        elevatorSlideSolenoid.set(DoubleSolenoid.Value.kOff);
        releaser.set(false);
        leftFlap.set(0.0);
        isDeployed = false;
    }

    public void liftRightElevator() {
        if (isSafeToLiftElevators()) {
            rightElevatorSolenoid.set(true);
        }
    }

    public void deployElevators() {
        releaser.set(true);
        elevatorSlideSolenoid.set(DoubleSolenoid.Value.kForward);
        isDeployed = true;
    }

    public boolean isSafeToLiftElevators() {
        return isDeployed;
    }

    public void slide(DoubleSolenoid.Value value) {
        elevatorSlideSolenoid.set(value);
    }

    public void setFlapPower(double power) {
        leftFlap.set(power);
    }

    public boolean isFlapDetected() {
        return leftFlapSensor.get();
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        switch (state) {
            case TELEOP:
            case AUTONOMOUS:
                elevatorSlideSolenoid.set(DoubleSolenoid.Value.kForward);
                break;
            case DISABLED:
            case TEST:
                elevatorSlideSolenoid.set(DoubleSolenoid.Value.kOff);
                break;
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SolenoidTest("rightElevatorSolenoid", rightElevatorSolenoid));
        tests.addTest(new SolenoidTest("releaser", releaser));
        tests.addTest(new SpeedControllerTest("leftFlap", leftFlap));
        tests.addTest(new DigitalSensorTest("leftFlapSensor", leftFlapSensor));
        tests.addTest(new DoubleSolenoidTest("elevatorSlideSolenoid", elevatorSlideSolenoid));
        return tests;
    }

    public void configure(Config config) {
        this.config = config;
        this.rightElevatorSolenoid = config.rightElevatorSolenoid.create();
        this.releaser = config.releaser.create();
        this.elevatorSlideSolenoid = config.elevatorSlideSolenoid.create();
        this.leftFlap = config.leftFlap.create();
        this.leftFlapSensor = config.leftFlapSensor.create();
        reset();
    }

    public void deconfigure() {
        elevatorSlideSolenoid.free();
        rightElevatorSolenoid.free();
        SpeedControllerConfig.free(leftFlap);
        leftFlapSensor.free();
        releaser.free();
    }

    public static class Config {
        public SolenoidConfig rightElevatorSolenoid;
        public SolenoidConfig releaser;
        public DoubleSolenoidConfig elevatorSlideSolenoid;
        public SpeedControllerConfig leftFlap;
        public DigitalSensorConfig leftFlapSensor;
    }
}