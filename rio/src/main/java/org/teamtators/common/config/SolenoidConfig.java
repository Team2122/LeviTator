package org.teamtators.common.config;

import edu.wpi.first.wpilibj.Solenoid;

public class SolenoidConfig {
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
