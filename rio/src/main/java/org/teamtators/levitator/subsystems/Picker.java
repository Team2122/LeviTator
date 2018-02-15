package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.SolenoidConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.control.MotorPowerUpdater;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.automated.MotorCurrentTest;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SolenoidTest;
import org.teamtators.common.tester.components.SpeedControllerTest;
import org.teamtators.levitator.TatorRobot;

import java.util.Arrays;
import java.util.List;

import java.util.Arrays;
import java.util.List;

public class Picker extends Subsystem implements Configurable<Picker.Config> {
    private SpeedController leftMotor;
    private MotorPowerUpdater leftMotorUpdater;
    private SpeedController rightMotor;
    private MotorPowerUpdater rightMotorUpdater;
    private Solenoid extensionSolenoid;
    private DigitalSensor cubeDetectSensor;
    private DigitalSensor upperCubeSensor;
    private DigitalSensor lowerCubeSensor;
    private Solenoid armLock;

    private boolean defaultExtended = false;

    private TatorRobot robot;
    private Config config;

    public Picker(TatorRobot robot) {
        super("Picker");
        this.robot = robot;
    }

    public double getLeftCurrent() {
        return robot.getPDP().getCurrent(config.leftMotor.getPowerChannel());
    }

    public double getRightCurrent() {
        return robot.getPDP().getCurrent(config.rightMotor.getPowerChannel());
    }

    public void setRollerPowers(double left, double right) {
//        logger.trace("Setting roller powers to {}, {}", left, right);
        leftMotorUpdater.set(left);
        rightMotorUpdater.set(right);
    }

    public void setLeftMotorPower(double power) {
        setRollerPowers(power, 0.0);
    }

    public void setRightMotorPower(double power) {
        setRollerPowers(0.0, power);
    }

    public void setRollerPowers(RollerPowers rollerPowers) {
        setRollerPowers(rollerPowers.left, rollerPowers.right);
    }

    public void setRollerPower(double power) {
        setRollerPowers(power, power);
    }

    public void stopRollers() {
        setRollerPowers(0.0, 0.0);
    }

    public boolean isCubeDetected() {
        return cubeDetectSensor.get();
    }

    public boolean isCubeDetectedLeft() {
        return upperCubeSensor.get();
    }

    public boolean isCubeDetectedRight() {
        return lowerCubeSensor.get();
    }

    public boolean isCubeInPicker() {
        return upperCubeSensor.get();
    }

    public boolean isCubeDetectedAny() {
        return cubeDetectSensor.get() || upperCubeSensor.get() || lowerCubeSensor.get();
    }

    public void setPickerExtended(boolean isExtended) {
        extensionSolenoid.set(isExtended);
    }

    public boolean isExtended() {
        return extensionSolenoid.get();
    }


    public void extend(){
        setPickerExtended(true);
    }

    public void retract(){
        setPickerExtended(false);
    }

    public void toggleExtension() {
        if (isExtended()) {
            retract();
        } else {
            extend();
        }
    }

    public void extendDefault() {
        setPickerExtended(defaultExtended);
    }

    public boolean getDefaultExtended() {
        return defaultExtended;
    }

    public void setDefaultExtended(boolean defaultExtended) {
        this.defaultExtended = defaultExtended;
        if (isExtended() != defaultExtended) {
            extendDefault();
        }
    }

    public void setDefaultExtend() {
        setDefaultExtended(true);
    }

    public void setDefaultRetract() {
        setDefaultExtended(false);
    }

    public void setArmLocked(boolean locked) {
        armLock.set(locked);
    }

    public void lockArms() {
        setArmLocked(true);
    }

    public void unlockArms() {
        setArmLocked(false);
    }

    public boolean isArmLocked() {
        return armLock.get();
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SpeedControllerTest("leftMotor", leftMotor));
        tests.addTest(new SpeedControllerTest("rightMotor", rightMotor));
        tests.addTest(new SolenoidTest("extensionSolenoid", extensionSolenoid));
        tests.addTest(new DigitalSensorTest("cubeDetectSensor", cubeDetectSensor));
        tests.addTest(new DigitalSensorTest("upperCubeSensor", upperCubeSensor));
        tests.addTest(new DigitalSensorTest("lowerCubeSensor", lowerCubeSensor));
        tests.addTest(new SolenoidTest("armLock", armLock));
        return tests;
    }

    @Override
    public List<AutomatedTest> createAutomatedTests() {
        return Arrays.asList(
                new MotorCurrentTest("PickerLeftMotorCurrentTest", this::setLeftMotorPower, this::getLeftCurrent),
                new MotorCurrentTest("PickerRightMotorCurrentTest", this::setRightMotorPower, this::getRightCurrent)
                //TODO: Hybrid tests
        );
    }

    @Override
    public void configure(Config config) {
        super.configure();
        this.config = config;

        this.leftMotor = config.leftMotor.create();
        this.rightMotor = config.rightMotor.create();
        this.extensionSolenoid = config.extensionSolenoid.create();
        this.cubeDetectSensor = config.cubeDetectSensor.create();
        this.upperCubeSensor = config.upperCubeSensor.create();
        this.lowerCubeSensor = config.lowerCubeSensor.create();
        this.armLock = config.armLock.create();

        ((Sendable) leftMotor).setName("Picker", "leftMotor");
        ((Sendable) rightMotor).setName("Picker", "rightMotor");
        extensionSolenoid.setName("Picker", "extensionSolenoid");
        cubeDetectSensor.setName("Picker", "cubeDetectSensor");
        upperCubeSensor.setName("Picker", "upperCubeSensor");
        lowerCubeSensor.setName("Picker", "lowerCubeSensor");
        armLock.setName("Picker", "armLock");

        leftMotorUpdater = new MotorPowerUpdater(leftMotor);
        rightMotorUpdater = new MotorPowerUpdater(rightMotor);
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        if (state == RobotState.AUTONOMOUS) {
            setDefaultRetract();
        }
    }

    @Override
    public void deconfigure() {
        super.deconfigure();
        SpeedControllerConfig.free(leftMotor);
        SpeedControllerConfig.free(rightMotor);
        extensionSolenoid.free();
        cubeDetectSensor.free();
        upperCubeSensor.free();
        lowerCubeSensor.free();
        armLock.free();
    }

    public List<Updatable> getMotorUpdatables() {
        return Arrays.asList(leftMotorUpdater, rightMotorUpdater);
    }

    @SuppressWarnings("WeakerAccess")
    public static class Config {
        public SpeedControllerConfig leftMotor;
        public SpeedControllerConfig rightMotor;
        public SolenoidConfig extensionSolenoid;
        public DigitalSensorConfig cubeDetectSensor;
        public DigitalSensorConfig upperCubeSensor;
        public DigitalSensorConfig lowerCubeSensor;
        public SolenoidConfig armLock;
    }

    public static class RollerPowers {
        public double left;
        public double right;
    }
}