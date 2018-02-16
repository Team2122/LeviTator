package org.teamtators.levitator.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.SolenoidConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.automated.MotorCurrentTest;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SolenoidTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

import java.util.Arrays;
import java.util.List;

public class Picker extends Subsystem implements Configurable<Picker.Config> {
    private SpeedController leftMotor;
    private SpeedController rightMotor;
    private Solenoid extensionSolenoid;
    private DigitalSensor cubeDetectSensor;
    private DigitalSensor upperCubeSensor;
    private DigitalSensor lowerCubeSensor;

    public Picker() {
        super("Picker");
    }

    public double getLeftCurrent() {
        return ((WPI_TalonSRX)leftMotor).getOutputCurrent();
    }

    public double getRightCurrent() {
        return ((WPI_TalonSRX)rightMotor).getOutputCurrent();
    }

    public void setRollerPowers(double left, double right) {
//        logger.trace("Setting roller powers to {}, {}", left, right);
        leftMotor.set(left);
        rightMotor.set(right);
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

    public void setPickerExtended(boolean isExtended) {
        extensionSolenoid.set(isExtended);
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

    public void extend(){
        setPickerExtended(true);
    }

    public void retract(){
        setPickerExtended(false);
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
        return tests;
    }

    @Override
    public List<AutomatedTest> createAutomatedTests() {
        return Arrays.asList(
                new MotorCurrentTest("PickerLeftMotorCurrentTest", this::setLeftMotorPower, this::getLeftCurrent),
                new MotorCurrentTest("PickerRightMotorCurrentTest", this::setRightMotorPower, this::getRightCurrent),
                new org.teamtators.common.tester.automated.DigitalSensorTest("CubeDetectSensorTest", this::isCubeDetected),
                new org.teamtators.common.tester.automated.DigitalSensorTest("CubeDetectLeftSensorTest", this::isCubeDetectedLeft),
                new org.teamtators.common.tester.automated.DigitalSensorTest("CubeDetectRightSensorTest", this::isCubeDetectedRight)
        );
    }

    @Override
    public void configure(Config config) {
        super.configure();

        this.leftMotor = config.leftMotor.create();
        this.rightMotor = config.rightMotor.create();
        this.extensionSolenoid = config.extensionSolenoid.create();
        this.cubeDetectSensor = config.cubeDetectSensor.create();
        this.upperCubeSensor = config.upperCubeSensor.create();
        this.lowerCubeSensor = config.lowerCubeSensor.create();

        ((Sendable) leftMotor).setName("Picker", "leftMotor");
        ((Sendable) rightMotor).setName("Picker", "rightMotor");
        extensionSolenoid.setName("Picker", "extensionSolenoid");
        cubeDetectSensor.setName("Picker", "cubeDetectSensor");
        upperCubeSensor.setName("Picker", "upperCubeSensor");
        lowerCubeSensor.setName("Picker", "lowerCubeSensor");
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
    }

    public static class Config {
        public SpeedControllerConfig leftMotor;
        public SpeedControllerConfig rightMotor;
        public SolenoidConfig extensionSolenoid;
        public DigitalSensorConfig cubeDetectSensor;
        public DigitalSensorConfig upperCubeSensor;
        public DigitalSensorConfig lowerCubeSensor;
    }

    public static class RollerPowers {
        public double left;
        public double right;
    }
}