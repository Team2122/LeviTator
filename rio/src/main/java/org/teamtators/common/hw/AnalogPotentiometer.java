package org.teamtators.common.hw;

import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.interfaces.Potentiometer;
import edu.wpi.first.wpilibj.smartdashboard.SendableBuilder;

public class AnalogPotentiometer extends SensorBase implements Potentiometer, Sendable {
    public static final double DEFAULT_FULL_RANGE = 360.0;
    public static final double DEFAULT_OFFSET = 0.0;
    public static final boolean DEFAULT_CONTINUOUS = false;
    private AnalogInput analogInput;
    private double fullRange;
    private double offset;
    private boolean continuous;

    public AnalogPotentiometer(int channel, double fullRange, double offset, boolean continuous) {
        analogInput = new AnalogInput(channel);
        this.fullRange = fullRange;
        this.offset = offset;
        this.continuous = continuous;

        analogInput.setAverageBits(20);

        addChild(analogInput);
    }

    public AnalogPotentiometer(int channel, double fullRange, double offset) {
        this(channel, fullRange, offset, DEFAULT_CONTINUOUS);
    }

    public AnalogPotentiometer(int channel, boolean continuous) {
        this(channel, DEFAULT_FULL_RANGE, DEFAULT_OFFSET, continuous);
    }

    public AnalogPotentiometer(int channel) {
        this(channel, DEFAULT_CONTINUOUS);
    }

    public double getRawVoltage() {
        return analogInput.getVoltage();
    }

    public void free() {
        super.free();
        analogInput.free();
    }

    @Override
    public double get() {
        double p = analogInput.getAverageVoltage() / ControllerPower.getVoltage5V();
        double value = p * fullRange + offset;
        if (continuous) {
            if (value < 0) {
                value += fullRange;
            }
            if (value > fullRange) {
                value -= fullRange;
            }
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

    public AnalogInput getAnalogInput() {
        return analogInput;
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("Analog Input");
        builder.addDoubleProperty("Value", this::get, null);
        builder.addDoubleProperty("Offset", this::getOffset, this::setOffset);
    }
}
