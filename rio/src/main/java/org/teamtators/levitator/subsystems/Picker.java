package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SolenoidTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

public class Picker extends Subsystem {

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

    }

    public void setDeathGrip(boolean isGrip) {

    }

    public void setPickerRetract(boolean isRetract) {

    }

    public boolean isCubeIn() {
        return false;
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

    public enum State {
        IN,
        PICKING,
        GRIP_IN,
        GRIP_OUT,
        RELEASING
    }
}
