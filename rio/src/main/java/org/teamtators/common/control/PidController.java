package org.teamtators.common.control;

import edu.wpi.first.wpilibj.Sendable;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SendableBuilder;
import org.teamtators.common.config.Configurable;

/**
 * A PID Controller implementation, with feed forward.
 */
public class PidController extends AbstractController implements Configurable<PidController.Config> {
    private double kP = 0.0;
    private double kI = 0.0;
    private double kD = 0.0;
    private double kF = 0.0;
    private double maxIError = Double.POSITIVE_INFINITY;
    private double minISetpoint = 0.0;
    private double lastInput;
    private double totalError;

    public PidController(String name) {
        this(name, 0.0, 0.0, 0.0);
    }

    public PidController(String name, double kP, double kI, double kD) {
        super(name);
        LiveWindow.add(this);
        setPID(kP, kI, kD);
    }

    public void setPIDF(double kP, double kI, double kD, double kF) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.kF = kF;
    }

    public void setPID(double kP, double kI, double kD) {
        setPIDF(kP, kI, kD, 0.0);
    }

    public double getP() {
        return kP;
    }

    public void setP(double kP) {
        this.kP = kP;
    }

    public double getI() {
        return kI;
    }

    public void setI(double kI) {
        this.kI = kI;
    }

    public double getD() {
        return kD;
    }

    public void setD(double kD) {
        this.kD = kD;
    }

    public double getMaxIError() {
        return maxIError;
    }

    public void setMaxIError(double maxIError) {
        this.maxIError = maxIError;
    }

    public double getMinISetpoint() {
        return minISetpoint;
    }

    public void setMinISetpoint(double minISetpoint) {
        this.minISetpoint = minISetpoint;
    }

    public double getF() {
        return kF;
    }

    public void setF(double kF) {
        this.kF = kF;
    }

    @Override
    protected double computeOutput(double delta) {
        double error = getError();
        double output = error * kP;
        if (Math.abs(error) < maxIError && Math.abs(getSetpoint()) >= minISetpoint) {
            totalError += error * delta;
        } else {
            totalError = 0;
        }
        output += kI * totalError;
        if (!Double.isNaN(lastInput) && delta != 0) {
            output += kD * (getInput() - lastInput) / delta;
        }
        output += computeFeedForward();

        lastInput = getInput();

        if (Double.isInfinite(output) || Double.isNaN(output))
            return 0;

        return output;
    }

    protected double computeFeedForward() {
        return kF * getSetpoint();
    }

    public void resetTotalError() {
        totalError = 0;
    }

    @Override
    public void reset() {
        super.reset();
        lastInput = Double.NaN;
        resetTotalError();
    }

    public void configure(Config config) {
        if (config == null) return;
        setPIDF(config.P, config.I, config.D, config.F);
        setMaxIError(config.maxIError);
        setMinISetpoint(config.minISetpoint);
        super.configure(config);
    }

    public static class Config extends AbstractController.Config {
        public double P = 0.0, I = 0.0, D = 0.0, F = 0.0;
        public double maxIError = Double.POSITIVE_INFINITY, minISetpoint = 0.0;
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("PIDController");
        builder.setSafeState(this::reset);
        super.initSendable(builder);
        builder.addDoubleProperty("p", this::getP, this::setP);
        builder.addDoubleProperty("i", this::getI, this::setI);
        builder.addDoubleProperty("d", this::getD, this::setD);
        builder.addDoubleProperty("f", this::getF, this::setF);
        builder.addDoubleProperty("setpoint", this::getSetpoint, this::setSetpoint);
    }
}
