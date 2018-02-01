package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.AnalogPotentiometer;
import edu.wpi.first.wpilibj.DigitalOutput;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.PWMSpeedController;
import org.teamtators.common.scheduler.Subsystem;

public class Lift extends Subsystem {

    private PWMSpeedController liftMotor;
    private DigitalOutput limitSensor;
    private PWMSpeedController pivotMotor;
    private AnalogPotentiometer pivotEncoder;
    private Encoder liftEncoder;

    private double desiredPickerAngle;
    private double desiredLiftHeight;

    public Lift() {
        super("lift");
    }

    public double getCurrentLiftPositionInInches() {
        return liftEncoder.getDistance();
    }

    public double getDesiredLiftPositionInInches() {
        return desiredLiftHeight;
    }

    public void setDesiredLiftPositionInInches(double desiredPosition) {
        this.desiredLiftHeight = desiredPosition;
    }

    public void setDesiredLiftKnownPosition(KnownLiftPosition desiredPosition) {

    }

    public double getCurrentPickerAngle() {
        return pivotEncoder.get();
    }

    public double getDesiredPickerAngler() {
        return desiredPickerAngle;
    }

    public void setDesiredPickerAngle(double desiredAngle) {
        this.desiredPickerAngle = desiredAngle;
    }

    public void setDesiredKnownPickerPosition(KnownPickerPosition desiredPosition) {

    }

    public enum KnownLiftPosition {
        PICK,
        SWITCH,
        SACLE_LOW,
        SCALE_HIGH;
    }

    public enum KnownPickerPosition {
        LEFT,
        CENTER,
        RIGHT;
    }
}
