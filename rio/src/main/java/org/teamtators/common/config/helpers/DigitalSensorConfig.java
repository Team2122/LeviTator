package org.teamtators.common.config.helpers;

import org.teamtators.common.hw.DigitalSensor;

/**
 * Example mapping:
 * exampleSensor: {channel: 0, type: NPN}
 * OR
 * exampleSensor: {channel: 0, type: PNP}
 * <p>
 * Whichever one works!
 */
public class DigitalSensorConfig implements ConfigHelper<DigitalSensor> {
    private int channel;
    private DigitalSensor.Type type = DigitalSensor.Type.PNP;

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
