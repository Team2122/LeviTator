package org.teamtators.common.config.helpers;

import edu.wpi.first.wpilibj.DoubleSolenoid;

public class DoubleSolenoidConfig implements ConfigHelper<DoubleSolenoid> {
    private int forwardChannel;
    private int reverseChannel;

    public int getForwardChannel() {
        return forwardChannel;
    }

    public void setForwardChannel(int forwardChannel) {
        this.forwardChannel = forwardChannel;
    }

    public int getReverseChannel() {
        return reverseChannel;
    }

    public void setReverseChannel(int reverseChannel) {
        this.reverseChannel = reverseChannel;
    }

    public DoubleSolenoid create() {
        return new DoubleSolenoid(forwardChannel, reverseChannel);
    }
}
