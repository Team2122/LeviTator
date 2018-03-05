package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.SolenoidConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.control.MotorPowerUpdater;
import org.teamtators.common.control.Updater;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SolenoidTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

public class Picker extends Subsystem implements Configurable<Picker.Config> {
    private SpeedController leftMotor;
    private MotorPowerUpdater leftMotorUpdater;
    private Updater leftUpdater;
    private SpeedController rightMotor;
    private MotorPowerUpdater rightMotorUpdater;
    private Updater rightUpdater;
    private Solenoid extensionSolenoid;
    private DigitalSensor cubeDetectSensor;
    private DigitalSensor upperCubeSensor;
    private DigitalSensor lowerCubeSensor;

    public Picker() {
        super("Picker");
    }

    public void setRollerPowers(double left, double right) {
//        logger.trace("Setting roller powers to {}, {}", left, right);
        leftMotorUpdater.set(left);
        rightMotorUpdater.set(right);
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

        leftMotorUpdater = new MotorPowerUpdater(leftMotor);
        rightMotorUpdater = new MotorPowerUpdater(rightMotor);

        leftUpdater = new Updater(leftMotorUpdater);
        rightUpdater = new Updater(rightMotorUpdater);

        leftUpdater.start();
        rightUpdater.start();
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

        leftUpdater.stop();
        rightUpdater.stop();

        leftUpdater = null; //so the GC catches these bad boys
        rightUpdater = null; //so the GC catches these bad boys
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