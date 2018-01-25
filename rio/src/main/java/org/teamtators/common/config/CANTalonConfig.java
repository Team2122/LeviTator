package org.teamtators.common.config;

import com.ctre.CANTalon;
import org.teamtators.common.Robot;

/**
 * @author Alex Mikhalev
 */
public class CANTalonConfig {
    public int canId;
    public CANTalon.TalonControlMode controlMode = CANTalon.TalonControlMode.Voltage;
    public double p = 0.0;
    public double i = 0.0;
    public double d = 0.0;
    public double f = 0.0;
    public int izone = 0;
    public double closeLoopRampRate = 0.0;
    public boolean reverseSensor = false;
    public boolean reverseOutput = false;
    public int encoderCpr = -1;
    public CANTalon.FeedbackDevice feedbackDevice = CANTalon.FeedbackDevice.QuadEncoder;
    public boolean brake = true;
    public double maxOutputVoltage = Double.NaN;
    public double peakForwardVoltage = +12.0;
    public double peakReverseVoltage = -12.0;
    public double nominalForwardVoltage = +0.0;
    public double nominalReverseVoltage = -0.0;
    public CANTalon.VelocityMeasurementPeriod velocityMeasurementPeriod = CANTalon.VelocityMeasurementPeriod.Period_100Ms;
    public int velocityMeasurementWindow = 64;
    public int powerChannel;

    public CANTalon create() {
        CANTalon talon = new CANTalon(canId);

        talon.setFeedbackDevice(feedbackDevice);
        if (encoderCpr != -1) {
            talon.configEncoderCodesPerRev(encoderCpr);
        }
        if (talon.isSensorPresent(feedbackDevice) != CANTalon.FeedbackDeviceStatus.FeedbackStatusPresent) {
            Robot.logger.warn("CANTalon id " + canId + " missing feedback device " + feedbackDevice);
        }
        talon.reverseSensor(reverseSensor);
        talon.reverseOutput(reverseOutput);
        if (controlMode.isPID()) {
            // TODO multiple profiles
            talon.setPID(p, i, d, f, izone, closeLoopRampRate, 0);
        }
        if (!Double.isNaN(maxOutputVoltage)) {
            talon.configMaxOutputVoltage(maxOutputVoltage);
        } else {
            talon.configPeakOutputVoltage(peakForwardVoltage, peakReverseVoltage);
        }
        talon.configNominalOutputVoltage(nominalForwardVoltage, nominalReverseVoltage);
        talon.enableBrakeMode(brake);
        talon.changeControlMode(controlMode);
        talon.SetVelocityMeasurementPeriod(velocityMeasurementPeriod);
        talon.SetVelocityMeasurementWindow(velocityMeasurementWindow);

        return talon;
    }
}
