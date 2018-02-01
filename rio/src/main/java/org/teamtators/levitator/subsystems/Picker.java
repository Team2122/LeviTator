package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;

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

    public void setDeathGrip(boolean isGrip){

    }

    public void setPickerRetract(boolean isRetract){

    }

    public boolean isCubeIn(){
        return false;
    }

    public enum State{
        IN,
        PICKING,
        GRIP_IN,
        GRIP_OUT,
        RELEASING
    }

}


