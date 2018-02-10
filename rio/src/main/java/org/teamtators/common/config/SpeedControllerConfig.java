package org.teamtators.common.config;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import edu.wpi.first.wpilibj.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.hw.NoSpeedController;

/**
 * Example Mapping:
 * <p>
 * minimum required:
 * controllerName: {channel: 0}
 * <p>
 * all values:
 * controllerName: {channel: 0, inverted: true, type: Victor, powerChannels: [0, 1, 2]}
 */
public class SpeedControllerConfig {
    private int channel;
    private boolean inverted = false;
    private Type type = Type.NONE;
    private int[] powerChannels;
    //Not config
    private Logger logger = LoggerFactory.getLogger(SpeedControllerConfig.class);


    public void setPowerChannel(int[] powerChannels) {
        this.powerChannels = powerChannels;
    }

    public void setPowerChannels(int[] powerChannels) {
        this.powerChannels = powerChannels;
    }

    public int[] getPowerChannels() {
        return this.powerChannels;
    }

    public int getPowerChannel() {
        if (powerChannels == null || powerChannels.length == 0) {
            logger.error("No power channel specified");
            return 0;
        }
        return powerChannels[0];
    }

    public double getTotalCurrent(PowerDistributionPanel pdp) {
        if (powerChannels == null) {
            return 0;
        }
        double totalCurrent = 0;
        for (int powerChanel : powerChannels) {
            totalCurrent += pdp.getCurrent(powerChanel);
        }
        return totalCurrent;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setPowerChannel(int powerChannel) {
        this.powerChannels = new int[1];
        this.powerChannels[0] = powerChannel;
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public SpeedController create() {
        SpeedController speedController;
        switch (type) {
            case NONE:
                speedController = new NoSpeedController();
                break;
            case JAGUAR:
                speedController = new Jaguar(channel);
                break;
            case SD540:
                speedController = new SD540(channel);
                break;
            case SPARK:
                speedController = new Spark(channel);
                break;
            case TALON:
                speedController = new Talon(channel);
                break;
            case VICTOR:
                speedController = new Victor(channel);
                break;
            case VICTOR_SP:
                speedController = new VictorSP(channel);
                break;
            case TALON_SRX:
                speedController = new WPI_TalonSRX(channel);
                break;
            case VICTOR_SPX:
                speedController = new WPI_VictorSPX(channel);
                break;
            default:
                throw new RuntimeException("Invalid speed controller type " + type + ".");
        }
        speedController.setInverted(inverted);
        return speedController;
    }

    public enum Type {
        NONE,
        JAGUAR,
        SD540,
        SPARK,
        TALON,
        VICTOR,
        VICTOR_SP,
        TALON_SRX,
        VICTOR_SPX
    }
}
