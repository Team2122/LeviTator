package org.teamtators.common.control;

import com.fasterxml.jackson.databind.JsonNode;
import org.teamtators.common.config.ConfigException;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.DataLoggable;
import org.teamtators.common.datalogging.LogDataProvider;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractController extends AbstractUpdatable implements DataLoggable {

    private DataCollector dataCollector = DataCollector.getDataCollector();
    private ControllerInput inputProvider;
    private ControllerOutput outputConsumer;
    private Predicate<AbstractController> targetPredicate = ControllerPredicates.alwaysFalse();
    private LogDataProvider logDataProvider = new ControllerLogDataProvider();
    private boolean dataLog = false;

    private double minSetpoint = Double.NEGATIVE_INFINITY;
    private double maxSetpoint = Double.POSITIVE_INFINITY;
    private double minOutput = Double.NEGATIVE_INFINITY;
    private double maxOutput = Double.POSITIVE_INFINITY;

    private volatile double lastDelta;
    private volatile double setpoint;
    private volatile double input;
    private volatile double output;
    private volatile double holdPower = Double.NaN;
    private volatile boolean onTarget;

    public AbstractController(String name) {
        super(name);
        reset();
    }

    private static double applyLimits(double value, double min, double max) {
        if (value > max) return max;
        else if (value < min) return min;
        else return value;
    }

    public synchronized void reset() {
        lastDelta = 0.0;
        setpoint = 0.0;
        input = 0.0;
        output = 0.0;
        onTarget = false;
    }

    @Override
    protected synchronized final void doUpdate(double delta) {
        if (!running) {
            return;
        }

        checkNotNull(inputProvider, "input must be set on a Controller before using");
        this.lastDelta = delta;
        input = this.inputProvider.getControllerInput();

        this.onTarget = this.targetPredicate.test(this);

        double computedOutput;
        if (onTarget && !Double.isNaN(holdPower)) {
            computedOutput = holdPower;
        } else {
            computedOutput = computeOutput(delta);
        }
        double minOutput = this.minOutput;
        double maxOutput = this.maxOutput;
        computedOutput = applyLimits(computedOutput, minOutput, maxOutput);

        output = computedOutput;
        if (outputConsumer != null)
            outputConsumer.controllerWrite(output);
    }

    protected abstract double computeOutput(double delta);

    public ControllerInput getInputProvider() {
        return inputProvider;
    }

    public void setInputProvider(ControllerInput inputProvider) {
        this.inputProvider = inputProvider;
    }

    public ControllerOutput getOutputConsumer() {
        return outputConsumer;
    }

    public void setOutputConsumer(ControllerOutput outputConsumer) {
        this.outputConsumer = outputConsumer;
    }

    public Predicate getTargetPredicate() {
        return targetPredicate;
    }

    public void setTargetPredicate(Predicate targetPredicate) {
        checkNotNull(targetPredicate);
        this.targetPredicate = targetPredicate;
    }

    public void configureTarget(JsonNode config) {
        if (config == null) return;
        if (!config.isObject()) {
            throw new ConfigException("Controller target config must be an object");
        }
        Predicate targetPredicate;
        if (config.has("within")) {
            targetPredicate = ControllerPredicates.withinError(config.get("within").asDouble());
        } else {
            targetPredicate = getTargetPredicate();
        }
        if (config.has("time")) {
            double time = config.get("time").asDouble();
            targetPredicate = new ControllerPredicates.SampleTime(time, targetPredicate);
        }
        setTargetPredicate(targetPredicate);
        if (config.has("stop") && config.get("stop").asBoolean()) {
            setHoldPower(0);
        }
    }

    public double getMinSetpoint() {
        return minSetpoint;
    }

    public void setMinSetpoint(double minSetpoint) {
        this.minSetpoint = minSetpoint;
    }

    public double getMaxSetpoint() {
        return maxSetpoint;
    }

    public void setMaxSetpoint(double maxSetpoint) {
        this.maxSetpoint = maxSetpoint;
    }

    public double getMinOutput() {
        return minOutput;
    }

    public void setMinOutput(double minOutput) {
        this.minOutput = minOutput;
    }

    public double getMaxOutput() {
        return maxOutput;
    }

    public void setMaxOutput(double maxOutput) {
        this.maxOutput = maxOutput;
    }

    public synchronized double getHoldPower() {
        return holdPower;
    }

    public synchronized void setHoldPower(double holdPower) {
        this.holdPower = holdPower;
    }

    public void unsetHoldPower() {
        setHoldPower(Double.NaN);
    }

    public synchronized double getLastDelta() {
        return lastDelta;
    }

    public synchronized double getSetpoint() {
        return setpoint;
    }

    public synchronized void setSetpoint(double setpoint) {
        this.setpoint = applyLimits(setpoint, minSetpoint, maxSetpoint);
    }

    public synchronized double getInput() {
        return input;
    }

    protected synchronized double getError() {
        return setpoint - input;
    }

    public synchronized boolean isOnTarget() {
        return this.onTarget;
    }

    public synchronized void start() {
        if (!running) {
            logger.trace("Starting controller " + getName() + ".");
            running = true;
//            reset();
            if (dataLog) {
                dataCollector.startProvider(logDataProvider);
            }
        }
    }

    public synchronized void stop() {
        if (running) {
            logger.trace("Stopping controller " + getName() + ".");
            running = false;
            if (outputConsumer != null)
                outputConsumer.controllerWrite(0.0);
            dataCollector.stopProvider(logDataProvider);
        }
    }

    protected void configure(Config config) {
        dataLog = config.logData;
        if (!Double.isNaN(config.maxAbsoluteSetpoint)) {
            setMaxSetpoint(config.maxAbsoluteSetpoint);
            setMinSetpoint(-config.maxAbsoluteSetpoint);
        } else {
            setMaxSetpoint(config.maxSetpoint);
            setMinSetpoint(config.minSetpoint);
        }
        if (!Double.isNaN(config.maxAbsoluteOutput)) {
            setMaxOutput(config.maxAbsoluteOutput);
            setMinOutput(-config.maxAbsoluteOutput);
        } else {
            setMinOutput(config.minOutput);
            setMaxOutput(config.maxOutput);
        }
        setHoldPower(config.holdPower);
        configureTarget(config.target);
    }

    public synchronized double getOutput() {
        return output;
    }

    @Override
    public LogDataProvider getLogDataProvider() {
        return logDataProvider;
    }

    public boolean isDataLogging() {
        return dataLog;
    }

    @Override
    public synchronized String toString() {
        return "AbstractController{" +
                "name='" + getName() + '\'' +
                ", setpoint=" + setpoint +
                ", input=" + input +
                ", output=" + output +
                ", onTarget=" + onTarget +
                '}';
    }

    protected static class Config {
        public double maxAbsoluteSetpoint = Double.NaN;
        public double maxSetpoint = Double.POSITIVE_INFINITY, minSetpoint = Double.NEGATIVE_INFINITY;
        public double maxAbsoluteOutput = Double.NaN;
        public double maxOutput = Double.POSITIVE_INFINITY, minOutput = Double.NEGATIVE_INFINITY;
        public JsonNode target;
        public double holdPower = Double.NaN;
        public boolean logData = false;
    }

    private class ControllerLogDataProvider implements LogDataProvider {
        @Override
        public String getName() {
            return AbstractController.this.getName();
        }

        @Override
        public List<Object> getKeys() {
            return Arrays.asList("setpoint", "input", "output", "onTarget");
        }

        @Override
        public List<Object> getValues() {
            synchronized (AbstractController.this) {
                return Arrays.asList(getSetpoint(), getInput(), output, isOnTarget());
            }
        }
    }
}
