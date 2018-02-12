package org.teamtators.common.config.helpers;

import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;

public class VictorSPXConfig extends CtreMotorControllerConfig implements ConfigHelper<WPI_VictorSPX> {
    public static int REQUIRED_FIRMWARE = 0x0301; // VictorSPX firmware 3.1

    public WPI_VictorSPX create() {
        super.validate();
        WPI_VictorSPX motor = new WPI_VictorSPX(id);
        super.configure(motor);
        super.checkVersion(motor, REQUIRED_FIRMWARE);

        return motor;
    }
}
