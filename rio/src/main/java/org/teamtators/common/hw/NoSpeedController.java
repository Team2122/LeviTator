package org.teamtators.common.hw;

public class NoSpeedController implements edu.wpi.first.wpilibj.SpeedController {
    private double speed;
    private boolean inverted;

    @Override
    public void set(double speed) {

    }

    @Override
    public double get() {
        return speed;
    }

    @Override
    public void setInverted(boolean isInverted) {
        this.inverted = isInverted;
    }

    @Override
    public boolean getInverted() {
        return inverted;
    }

    @Override
    public void disable() {

    }

    @Override
    public void stopMotor() {
        set(0.0);
    }

    @Override
    public void pidWrite(double output) {
        set(output);
    }
}
