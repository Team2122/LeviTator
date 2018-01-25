package org.teamtators.common.config;

import edu.wpi.first.wpilibj.Relay;

public class RelayConfig {
    public Integer channel = null;
    public Relay.Direction direction = null;

    public Relay create() {
        if (this.channel == null)
            throw new NullPointerException("Relay channel not specified");
        Relay relay = new Relay(channel);
        if (direction != null)
            relay.setDirection(direction);
        return relay;
    }

}
