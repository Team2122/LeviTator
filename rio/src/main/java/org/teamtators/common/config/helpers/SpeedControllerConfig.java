package org.teamtators.common.config.helpers;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.wpilibj.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.ConfigException;
import org.teamtators.common.hw.NoSpeedController;
import org.teamtators.common.hw.SpeedControllerGroup;

/**
 * Example Mapping:
 * <p>
 * minimum required:
 * controllerName: {channel: 0}
 * <p>
 * all values:
 * controllerName: {channel: 0, inverted: true, type: Victor, powerChannels: [0, 1, 2]}
 */
public class SpeedControllerConfig implements ConfigHelper<SpeedController> {
    public static final ObjectMapper CONFIG_MAPPER = TatorRobotBase.configMapper;
    private int channel;
    private boolean inverted = false;
    private Type type = Type.NONE;
    private int[] powerChannels;
    private JsonNode config;
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

    public JsonNode getConfig() {
        return config;
    }

    public void setConfig(JsonNode config) {
        this.config = config;
    }

    @Override
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
            default:
                return configureController();
        }
        speedController.setInverted(inverted);
        return speedController;
    }

    private SpeedController configureController() {
        if (this.config == null) {
            throw new ConfigException("Must specify config for GROUP, TALON_SRX and VICTOR_SPX SpeedControllerConfig");
        }
        ConfigHelper<? extends SpeedController> configHelper;
        Class<? extends ConfigHelper<? extends SpeedController>> configHelperClass;
        switch (this.type) {
            case GROUP:
                configHelperClass = SpeedControllerGroupConfig.class;
                break;
            case TALON_SRX:
                configHelperClass = TalonSRXConfig.class;
                break;
            case VICTOR_SPX:
                configHelperClass = VictorSPXConfig.class;
                break;
            default:
                throw new ConfigException("Invalid SpeedControllerConfig type " + type + ".");
        }
        try {
            configHelper = CONFIG_MAPPER.treeToValue(this.config, configHelperClass);
        } catch (Exception e) {
            throw new ConfigException(String.format("Error creating %s from SpeedControllerConfig",
                    configHelperClass.getSimpleName()), e);
        }
        return configHelper.create();
    }

    public enum Type {
        NONE,
        GROUP,
        JAGUAR,
        SD540,
        SPARK,
        TALON,
        VICTOR,
        VICTOR_SP,
        TALON_SRX,
        VICTOR_SPX
    }

    /**
     * Frees the object of any SpeedController, if it is able to be freed
     * @param speedController
     */
    public static void free(SpeedController speedController) {
        if (speedController instanceof PWM) {
            ((PWM) speedController).free();
        } else if (speedController instanceof WPI_TalonSRX) {
            ((WPI_TalonSRX) speedController).free();
        } else if (speedController instanceof WPI_VictorSPX) {
            ((WPI_VictorSPX) speedController).free();
        } else if (speedController instanceof SpeedControllerGroup) {
            for (SpeedController childController : ((SpeedControllerGroup) speedController).getSpeedControllers()) {
                free(childController);
            }
            ((SpeedControllerGroup) speedController).free();
        }
    }
}
