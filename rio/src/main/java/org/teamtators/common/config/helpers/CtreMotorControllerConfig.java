package org.teamtators.common.config.helpers;

import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.LimitSwitchNormal;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.RemoteLimitSwitchSource;
import org.teamtators.common.config.ConfigException;

public class CtreMotorControllerConfig {
    public static int CONFIG_TIMEOUT = 500;

    public int id = -1;
    public boolean inverted = false;
    public NeutralMode neutralMode = NeutralMode.EEPROMSetting;
    public double neutralDeadband = 0.04; // factory default
    public double openLoopRamp = 0.0; // # of seconds from 0 to full output, or 0 to disable
    public double voltageCompensationSaturation = Double.NaN;
    public boolean logTiming = false;
    public FeedbackDevice feedbackDevice = FeedbackDevice.None;
    public double neutralToFullTime = 0;

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
        motor.configSelectedFeedbackSensor(feedbackDevice, 0, CONFIG_TIMEOUT);
        motor.configForwardLimitSwitchSource(RemoteLimitSwitchSource.Deactivated, LimitSwitchNormal.Disabled, 0, CONFIG_TIMEOUT);
        motor.configReverseLimitSwitchSource(RemoteLimitSwitchSource.Deactivated, LimitSwitchNormal.Disabled, 0, CONFIG_TIMEOUT);
        motor.configForwardSoftLimitEnable(false, CONFIG_TIMEOUT);
        motor.configReverseSoftLimitEnable(false, CONFIG_TIMEOUT);
        motor.configOpenloopRamp(neutralToFullTime, CONFIG_TIMEOUT);
    }

    protected void checkVersion(com.ctre.phoenix.motorcontrol.can.BaseMotorController motor, int requiredVersion) {
        int firmwareVersion = motor.getFirmwareVersion();
        if (firmwareVersion != requiredVersion) {
//            Robot.logger.warn(String.format("%s (id %d) has wrong firmware version: %d.%d",
//                    motor.getClass().getSimpleName(), id, firmwareVersion >> 2, firmwareVersion % 0xff));
        }
    }
}
