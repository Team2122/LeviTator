package org.teamtators.common.hw;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.ControllerPower;
import edu.wpi.first.wpilibj.PIDSourceType;
import edu.wpi.first.wpilibj.interfaces.Potentiometer;

public class AnalogPotentiometer implements Potentiometer {
    private AnalogInput analogInput;
    private double fullRange;
    private double offset;
    private boolean continuous;

    public AnalogPotentiometer(int channel, double fullRange, double offset, boolean continuous) {
        analogInput = new AnalogInput(channel);
        this.fullRange = fullRange;
        this.offset = offset;
        this.continuous = continuous;
    }

    public AnalogPotentiometer(int channel, double fullRange, double offset) {
        this(channel, fullRange, offset, true);
    }

    public double getRawVoltage() {
        return analogInput.getVoltage();
    }

    public void free() {
        analogInput.free();
    }

    @Override
    public double get() {
        double p = analogInput.getVoltage() / ControllerPower.getVoltage5V();
        double value = p * fullRange + offset;
        if (value < 0) {
            value += fullRange;
        }
        if (value > fullRange) {
            value -= fullRange;
        }
        return value;
    }

    public double getFullRange() {
        return fullRange;
    }

    public void setFullRange(double fullRange) {
        this.fullRange = fullRange;
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset = offset;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    @Override
    public PIDSourceType getPIDSourceType() {
        return PIDSourceType.kDisplacement;
    }

    @Override
    public void setPIDSourceType(PIDSourceType pidSource) {
        if (pidSource != PIDSourceType.kDisplacement) {
            throw new IllegalArgumentException("Only supports displacement");
        }
    }

    @Override
    public double pidGet() {
        return this.get();
    }
}
