package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.AnalogPotentiometer;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.PWMSpeedController;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.Subsystem;

public class Lift extends Subsystem {

    private PWMSpeedController liftMotor;
    private DigitalSensor limitSensorTop;
    private DigitalSensor limitSensorBottom;
    private PWMSpeedController pivotMotor;
    private AnalogPotentiometer pivotEncoder;
    private Encoder liftEncoder;

    private double desiredPivotAngle;
    private double desiredHeight;

    public Lift() {
        super("Lift");
    }

    /**
     * @return height in inches
     */
    public double getCurrentHeight() {
        return liftEncoder.getDistance();
    }

    /**
     * @return height in inches
     */
    public double getDesiredHeight() {
        return desiredHeight;
    }

    /**
     * @param desiredHeight height in inches
     */
    public void setDesiredHeight(double desiredHeight) {
        this.desiredHeight = desiredHeight;
    }

    public void setDesiredHeightPreset(HeightPreset desiredHeight) {

    }

    public double getCurrentPivotAngle() {
        return pivotEncoder.get();
    }

    public double getDesiredPivotAngle() {
        return desiredPivotAngle;
    }

    public void setDesiredPivotAngle(double desiredAngle) {
        this.desiredPivotAngle = desiredAngle;
    }

    public void setDesiredAnglePreset(AnglePreset desiredPivotAngle) {

    }

    public enum HeightPreset {
        PICK,
        SWITCH,
        SCALE_LOW,
        SCALE_HIGH;
    }

    public enum AnglePreset {
        LEFT,
        CENTER,
        RIGHT;
    }
}
