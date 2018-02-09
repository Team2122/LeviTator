package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.DigitalSensorConfig;
import org.teamtators.common.config.SolenoidConfig;
import org.teamtators.common.config.SpeedControllerConfig;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SolenoidTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

public class Picker extends Subsystem implements Configurable<Picker.Config> {

    private SpeedController leftPickerMotor;
    private SpeedController rightPickerMotor;
    private Solenoid deathGripSolenoid;
    private Solenoid pickerRetractSolenoid;
    private DigitalSensor cubeStatusSensor;

    private State state;

    public Picker() {
        super("Picker");
    }

    public void setRollerPower(double rollerPower) {
        leftPickerMotor.set(rollerPower);
        rightPickerMotor.set(rollerPower);
    }

    public void setDeathGrip(boolean isGrip) {
        deathGripSolenoid.set(isGrip);
    }

    public void setPickerRetract(boolean isRetract) {
        pickerRetractSolenoid.set(isRetract);
    }

    public boolean isCubeIn() {
        return cubeStatusSensor.get();
    }

    public void setDesiredState(State desiredState) {
        state = desiredState;
    }

    public State getState(){
        return state;
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SpeedControllerTest("leftPickerMotor", leftPickerMotor));
        tests.addTest(new SpeedControllerTest("rightPickerMotor", rightPickerMotor));
        tests.addTest(new SolenoidTest("deathGripSolenoid", deathGripSolenoid));
        tests.addTest(new SolenoidTest("pickerRetractSolenoid", pickerRetractSolenoid));
        tests.addTest(new DigitalSensorTest("cubeStatusSensor", cubeStatusSensor));
        return tests;
    }

    @Override
    public void configure(Config config) {
        this.leftPickerMotor = config.leftPickerMotor.create();
        this.rightPickerMotor = config.rightPickerMotor.create();
        this.deathGripSolenoid = config.deathGripSolenoid.create();
        this.pickerRetractSolenoid = config.pickerRetractSolenoid.create();
        this.cubeStatusSensor = config.cubeStatusSensor.create();
    }

    public enum State {
        RETRACTED_NO_CUBE,
        PICKING,
        RETRACTED_WITH_CUBE,
        EXTENDED_WITH_CUBE,
        RELEASING
    }

    public static class Config {
        public SpeedControllerConfig leftPickerMotor;
        public SpeedControllerConfig rightPickerMotor;
        public SolenoidConfig deathGripSolenoid;
        public SolenoidConfig pickerRetractSolenoid;
        public DigitalSensorConfig cubeStatusSensor;
    }
}