package org.teamtators.common.hw;

import com.google.common.base.Preconditions;
import edu.wpi.first.wpilibj.DigitalInput;

/**
 * Reads digital inputs and returns the current value on the channel
 */
public class DigitalSensor {
    private DigitalInput digitalSensor;
    private Type type = Type.PNP;

    public DigitalSensor(int channel, Type type) {
        Preconditions.checkNotNull(type, "type can not be null");
        digitalSensor = new DigitalInput(channel);
        this.type = type;
    }

    public DigitalSensor(int channel) {
        this(channel, Type.PNP);
    }

    /**
     * @return the value from a digital input channel, taking into account the type
     */
    public boolean get() {
        boolean value = getRaw();
        switch (type) {
            case NPN:
                return !value;
            case PNP:
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
