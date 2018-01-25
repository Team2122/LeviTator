package org.teamtators.common.config;

import org.teamtators.common.hw.DigitalSensor;

public class DigitalSensorConfig {
    private int channel;
    private DigitalSensor.Type type;

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public DigitalSensor.Type getType() {
        return type;
    }

    public void setType(DigitalSensor.Type type) {
        this.type = type;
    }

    public DigitalSensor create() {
        DigitalSensor digitalSensor = new DigitalSensor(channel, type);
        return digitalSensor;
    }
}
