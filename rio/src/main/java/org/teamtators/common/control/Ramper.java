package org.teamtators.common.control;

import org.teamtators.common.config.Configurable;

public class Ramper implements Updatable, Configurable<Ramper.Config> {
    private double maxAcceleration;
    private boolean onlyUp = true;

    private double lastOutput;
    private double value;
    private double output;

    public Ramper(double maxAcceleration) {
        this.maxAcceleration = maxAcceleration;
    }

    public Ramper() {
        this(0);
    }

    public void reset() {
        lastOutput = Double.NaN;
        value = 0.0;
    }

    public void update(double deltaT) {
        if (Double.isNaN(lastOutput)) {
            output = lastOutput = value;
            return;
        }
        double deltaV = value - lastOutput;
        double maxDeltaV = maxAcceleration * deltaT;
        if (deltaV >= maxDeltaV && !(output < 0 && onlyUp)) {
            deltaV = maxDeltaV;
        } else if (deltaV < -maxDeltaV && !(output > 0 && onlyUp)) {
            deltaV = -maxDeltaV;
        }
        output = lastOutput + deltaV;
        lastOutput = output;
    }

    public double update(double deltaT, double value) {
        setValue(value);
        update(deltaT);
        return getOutput();
    }

    @Override
    public String getName() {
        return "Ramper";
    }

    public void configure(Config config) {
        this.maxAcceleration = config.maxAcceleration;
        this.onlyUp = config.onlyUp;
    }

    public double getMaxAcceleration() {
        return maxAcceleration;
    }

    public void setMaxAcceleration(double maxAcceleration) {
        this.maxAcceleration = maxAcceleration;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getOutput() {
        return output;
    }

    public boolean isOnlyUp() {
        return onlyUp;
    }

    public void setOnlyUp(boolean onlyUp) {
        this.onlyUp = onlyUp;
    }

    public static class Config {
        public double maxAcceleration;
        public boolean onlyUp = true;
    }
}
