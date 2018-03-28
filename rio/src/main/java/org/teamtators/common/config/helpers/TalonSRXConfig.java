package org.teamtators.common.config.helpers;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;

public class TalonSRXConfig extends CtreMotorControllerConfig implements ConfigHelper<WPI_TalonSRX> {
    public static int REQUIRED_FIRMWARE = 0x0303; // TalonSRX firmware 3.3

    public WPI_TalonSRX create() {
        super.validate();
        WPI_TalonSRX motor = new WPI_TalonSRX(id);
        motor.enableCurrentLimit(false);
        super.configure(motor);
        super.checkVersion(motor, REQUIRED_FIRMWARE);
        return motor;
    }
}
