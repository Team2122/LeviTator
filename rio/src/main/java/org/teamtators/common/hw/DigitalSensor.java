package org.teamtators.common.hw;

import edu.wpi.first.wpilibj.DigitalInput;

/**
 * Reads digital inputs and returns the current value on the channel
 */
public class DigitalSensor {
    private DigitalInput digitalSensor;
    private Type type;

    public DigitalSensor(int channel, Type type) {
        digitalSensor = new DigitalInput(channel);
        this.type = type;
    }

    /**
     * @return the value from a digital input channel, taking into account the type
     */
    public boolean get() {
        boolean value = getRaw();
        switch (type) {
            case NPN:
                return !value;
            default:
                return value;
        }
    }

    /**
     * @return the value from a digital input channel
     */
    public boolean getRaw() {
        return digitalSensor.get();
    }

    /**
     * @return type of the digital sensor
     */
    public Type getType() {
        return type;
    }

    public enum Type {
        NPN,
        PNP
    }
}
