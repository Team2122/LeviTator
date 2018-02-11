package org.teamtators.common.config;

import com.ctre.phoenix.motorcontrol.can.WPI_VictorSPX;

public class VictorSPXConfig {
    public int id;

    public WPI_VictorSPX create() {
        WPI_VictorSPX motor = new WPI_VictorSPX(id);
//        motor.brea
        return motor;
    }
}
