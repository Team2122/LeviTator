package org.teamtators.common.config.helpers;

import edu.wpi.first.wpilibj.Solenoid;

/**
 * Example mapping:
 * exampleSolenoid: {channel: 0}
 */
public class SolenoidConfig implements ConfigHelper<Solenoid> {
    private int channel;

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public Solenoid create() {
        return new Solenoid(channel);
    }
}
