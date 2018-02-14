package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.SolenoidConfig;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SolenoidTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

public class Picker extends Subsystem implements Configurable<Picker.Config> {
    private SpeedController leftMotor;
    private SpeedController rightMotor;
    private Solenoid extensionSolenoid;
    private DigitalSensor cubeDetectSensor;
    private DigitalSensor cubeDetectLeftSensor;
    private DigitalSensor cubeDetectRightSensor;

    public Picker() {
        super("Picker");
    }

    public void setRollerPowers(double left, double right) {
        logger.trace("Setting roller powers to {}, {}", left, right);
        leftMotor.set(left);
        rightMotor.set(right);
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
        return cubeDetectLeftSensor.get();
    }

    public boolean isCubeDetectedRight() {
        return cubeDetectRightSensor.get();
    }

    public boolean isCubeInPicker() {
        return cubeDetectRightSensor.get();
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
        tests.addTest(new DigitalSensorTest("cubeDetectLeftSensor", cubeDetectLeftSensor));
        tests.addTest(new DigitalSensorTest("cubeDetectRightSensor", cubeDetectRightSensor));
        return tests;
    }

    @Override
    public void configure(Config config) {
        this.leftMotor = config.leftMotor.create();
        this.rightMotor = config.rightMotor.create();
        this.extensionSolenoid = config.extensionSolenoid.create();
        this.cubeDetectSensor = config.cubeDetectSensor.create();
        this.cubeDetectLeftSensor = config.cubeDetectLeftSensor.create();
        this.cubeDetectRightSensor = config.cubeDetectRightSensor.create();

        ((Sendable) leftMotor).setName("Picker", "leftMotor");
        ((Sendable) rightMotor).setName("Picker", "rightMotor");
        extensionSolenoid.setName("Picker", "extensionSolenoid");
        cubeDetectSensor.setName("Picker", "cubeDetectSensor");
        cubeDetectLeftSensor.setName("Picker", "cubeDetectLeftSensor");
        cubeDetectRightSensor.setName("Picker", "cubeDetectRightSensor");
    }

    public static class Config {
        public SpeedControllerConfig leftMotor;
        public SpeedControllerConfig rightMotor;
        public SolenoidConfig extensionSolenoid;
        public DigitalSensorConfig cubeDetectSensor;
        public DigitalSensorConfig cubeDetectLeftSensor;
        public DigitalSensorConfig cubeDetectRightSensor;
    }

    public static class RollerPowers {
        public double left;
        public double right;
    }
}