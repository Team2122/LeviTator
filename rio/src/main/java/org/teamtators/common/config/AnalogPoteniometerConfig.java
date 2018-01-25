package org.teamtators.common.config;

import org.teamtators.common.hw.AnalogPotentiometer;

public class AnalogPoteniometerConfig {
    public int channel;
    public double fullRange = 360;
    public double offset = 0;
    public boolean continuous = true;

    public AnalogPotentiometer create() {
        return new AnalogPotentiometer(channel, fullRange, offset, continuous);
    }
}
