package org.teamtators.common.config.helpers;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import org.teamtators.common.Robot;
import org.teamtators.common.config.ConfigException;

public class CtreMotorControllerConfig {
    public static int CONFIG_TIMEOUT = 0; // no wait/error checking

    public int id = -1;
    public boolean inverted = false;
    public NeutralMode neutralMode = NeutralMode.EEPROMSetting;
    public double neutralDeadband = 0.04; // factory default
    public double openLoopRamp = 0.0; // # of seconds from 0 to full output, or 0 to disable
    public double voltageCompensationSaturation = Double.NaN;

    protected void validate() {
        if (id == -1) {
            throw new ConfigException("Must set id on CtreMotorControllerConfig");
        }
    }

    protected void configure(com.ctre.phoenix.motorcontrol.can.BaseMotorController motor) {
        motor.setInverted(this.inverted);
        motor.setNeutralMode(this.neutralMode);
        motor.configNeutralDeadband(this.neutralDeadband, CONFIG_TIMEOUT);
        motor.configOpenloopRamp(this.openLoopRamp, CONFIG_TIMEOUT);
        motor.configVoltageCompSaturation(this.openLoopRamp, CONFIG_TIMEOUT);
        if (!Double.isNaN(this.voltageCompensationSaturation)) {
            motor.configVoltageCompSaturation(this.voltageCompensationSaturation, CONFIG_TIMEOUT);
            motor.enableVoltageCompensation(true);
        } else {
            motor.enableVoltageCompensation(false);
        }
    }

    protected void checkVersion(com.ctre.phoenix.motorcontrol.can.BaseMotorController motor, int requiredVersion) {
        int firmwareVersion = motor.getFirmwareVersion();
        if (firmwareVersion != requiredVersion) {
            Robot.logger.warn(String.format("%s has wrong firmware version: %d.%d",
                    motor.getClass().getSimpleName(), firmwareVersion >> 2, firmwareVersion % 0xff));
        }
    }
}