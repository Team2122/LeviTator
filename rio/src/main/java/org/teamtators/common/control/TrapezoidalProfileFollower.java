package org.teamtators.common.control;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.DataLoggable;
import org.teamtators.common.datalogging.LogDataProvider;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alex Mikhalev
 */
public class TrapezoidalProfileFollower extends AbstractUpdatable implements DataLoggable,
        Configurable<TrapezoidalProfileFollower.Config> {
    private static final double EPSILON = 1e-5;
    private final DataCollector dataCollector = DataCollector.getDataCollector();
    private final LogDataProvider logDataProvider = new TrapezoidalProfileFollower.ControllerLogDataProvider();

    // Inputs and outputs
    private ControllerInput positionProvider;
    private ControllerInput velocityProvider;
    private ControllerOutput outputConsumer;
    private Predicate<TrapezoidalProfileFollower> onTargetPredicate = ControllerPredicates.finished();

    private Config config = new Config();
    private TrapezoidalProfile baseProfile;

    private volatile double maxOutput = Double.POSITIVE_INFINITY;
    private volatile double minOutput = Double.NEGATIVE_INFINITY;
    private volatile double holdPower = 0.0;

    // Variable data
    private volatile TrapezoidalProfileCalculator calculator;
    private volatile double initialPosition;
    private volatile double initialVelocity;
    private volatile double currentPosition;
    private volatile double currentVelocity;
    private volatile double positionError;
    private volatile double velocityError;
    private volatile double output;
    private volatile double totalPError;
    private volatile boolean finished;
    private volatile boolean onTarget;

    public TrapezoidalProfileFollower(String name) {
        super(name);
        reset();
    }

    public TrapezoidalProfileFollower(String name, ControllerInput positionProvider, ControllerInput velocityProvider,
                                      ControllerOutput outputConsumer) {
        this(name);
        setPositionProvider(positionProvider);
        setVelocityProvider(velocityProvider);
        setOutputConsumer(outputConsumer);
    }

    private static double applyLimits(double value, double min, double max) {
        if (value > max) return max;
        else if (value < min) return min;
        else return value;
    }

    public synchronized void reset() {
        currentPosition = 0.0;
        currentVelocity = 0.0;
        output = 0.0;
        resetTotalPError();
        finished = false;
        onTarget = false;
    }

    public synchronized void resetTotalPError() {
        totalPError = 0;
    }

    public ControllerInput getPositionProvider() {
        return positionProvider;
    }

    public void setPositionProvider(ControllerInput positionProvider) {
        checkNotNull(positionProvider);
        this.positionProvider = positionProvider;
    }

    public ControllerInput getVelocityProvider() {
        return velocityProvider;
    }

    public void setVelocityProvider(ControllerInput velocityProvider) {
        checkNotNull(velocityProvider);
        this.velocityProvider = velocityProvider;
    }

    public ControllerOutput getOutputConsumer() {
        return outputConsumer;
    }

    public void setOutputConsumer(ControllerOutput outputConsumer) {
        this.outputConsumer = outputConsumer;
    }

    public TrapezoidalProfile getBaseProfile() {
        return baseProfile;
    }

    public synchronized void setBaseProfile(TrapezoidalProfile baseProfile) {
        this.baseProfile = baseProfile;
    }

    public synchronized void updateProfile() {
        initialPosition = positionProvider.getControllerInput();
        initialVelocity = velocityProvider.getControllerInput();
        if (calculator == null) {
            calculator = baseProfile.createCalculator();
        } else {
            calculator.updateProfile(baseProfile);
        }
    }

    public Predicate<TrapezoidalProfileFollower> getOnTargetPredicate() {
        return onTargetPredicate;
    }

    public void setOnTargetPredicate(Predicate<TrapezoidalProfileFollower> onTargetPredicate) {
        checkNotNull(onTargetPredicate);
        this.onTargetPredicate = onTargetPredicate;
    }

    public synchronized double getMinOutput() {
        return minOutput;
    }

    public synchronized void setMinOutput(double minOutput) {
        this.minOutput = minOutput;
    }

    public synchronized double getMaxOutput() {
        return maxOutput;
    }

    public synchronized void setMaxOutput(double maxOutput) {
        this.maxOutput = maxOutput;
    }

    public synchronized double getHoldPower() {
        return holdPower;
    }

    public synchronized void setHoldPower(double holdPower) {
        this.holdPower = holdPower;
    }

    @Override
    protected synchronized final void doUpdate(double delta) {
        checkNotNull(velocityProvider, "currentPosition must be set on a TrapezoidalProfileFollower before using");
        currentPosition = this.positionProvider.getControllerInput() - initialPosition;
        checkNotNull(velocityProvider, "velocityProvider must be set on a TrapezoidalProfileFollower before using");
        currentVelocity = this.velocityProvider.getControllerInput();

        finished = calculator.update(delta);
        onTarget = onTargetPredicate.test(this);

        if (onTarget) {
            stop();
            return;
        }

        double computedOutput = computeOutput(delta);
        computedOutput = applyLimits(computedOutput, this.minOutput, this.maxOutput);

        output = computedOutput;
        if (outputConsumer != null)
            outputConsumer.controllerWrite(output);
    }

    protected double computeOutput(double delta) {
        positionError = calculator.getPosition() - getCurrentPosition();
        velocityError = calculator.getVelocity() - getCurrentVelocity();

        double output = positionError * config.kpP + velocityError * config.kpV +
                calculator.getVelocity() * config.kfV + calculator.getAcceleration() * config.kfA + holdPower;

        if (calculator.getVelocity() >= EPSILON) {
            output += config.kMinOutput;
        } else if (calculator.getVelocity() <= -EPSILON) {
            output -= config.kMinOutput;
        }

        double endPositionError = calculator.getProfile().getDistance() - getCurrentPosition();
        if (Math.abs(endPositionError) <= config.maxIError) {
            totalPError += endPositionError * delta;
            output += config.kiP * totalPError;
        } else {
            totalPError = 0;
        }

        if (Double.isInfinite(output) || Double.isNaN(output))
            return 0;

        return output;
    }

    public synchronized void start() {
        if (!running) {
            reset();
            updateProfile();

            if (config.logData) {
                dataCollector.startProvider(logDataProvider);
            }

            logger.trace("Starting trapezoidal follower " + getName() + ".");
            running = true;
        }
    }

    public synchronized void stop() {
        if (running) {
            running = false;
            logger.trace("Stopping trapezoidal follower " + getName() + ".");
            dataCollector.stopProvider(logDataProvider);
            if (outputConsumer != null)
                outputConsumer.controllerWrite(0.0);
        }
    }

    public synchronized double getInitialPosition() {
        return initialPosition;
    }

    public synchronized double getInitialVelocity() {
        return initialVelocity;
    }

    public synchronized double getCurrentPosition() {
        return currentPosition;
    }

    public synchronized double getCurrentVelocity() {
        return currentVelocity;
    }

    public synchronized double getPositionError() {
        return positionError;
    }

    public synchronized double getVelocityError() {
        return velocityError;
    }

    public synchronized boolean isFinished() {
        return this.finished;
    }

    public synchronized boolean isOnTarget() {
        return onTarget;
    }

    public synchronized double getOutput() {
        return output;
    }

    public TrapezoidalProfileCalculator getCalculator() {
        return calculator;
    }

    @Override
    public LogDataProvider getLogDataProvider() {
        return logDataProvider;
    }

    public boolean isDataLogging() {
        return config.logData;
    }

    @Override
    public synchronized String toString() {
        return "TrapezoidalProfileFollower{" +
                "name='" + getName() + '\'' +
                ", calculator=" + calculator +
                ", currentPosition=" + currentPosition +
                ", currentVelocity=" + currentVelocity +
                ", output=" + output +
                ", finished=" + finished +
                ", onTarget=" + onTarget +
                '}';
    }

    @Override
    public void configure(Config config) {
        synchronized (this) {
            this.config = config;

            if (!Double.isNaN(config.maxAbsoluteOutput)) {
                setMaxOutput(config.maxAbsoluteOutput);
                setMinOutput(-config.maxAbsoluteOutput);
            } else {
                setMinOutput(config.minOutput);
                setMaxOutput(config.maxOutput);
            }

            setHoldPower(config.kHoldPower);
        }
    }

    public Config getConfig() {
        return config;
    }

    public static class Config {
        public double kpP = 0.0; // position error proportion
        public double kiP = 0.0; // position error integral
        public double kpV = 0.0; // velocity error proportion
        public double kfV = 0.0; // velocity feed forward
        public double kMinOutput = 0.0; // intercept for velocity feed forward (aka minimum velocity)
        public double kfA = 0.0; // acceleration feed forward
        public double kHoldPower = 0.0; // always added to output power

        public double maxIError = Double.POSITIVE_INFINITY; // maximum absolute error for which it will apply kiP
        public double maxAbsoluteOutput = Double.NaN; // maximum absolute output power
        public double maxOutput = Double.POSITIVE_INFINITY; // maximum output power
        public double minOutput = Double.NEGATIVE_INFINITY; // minumum output power

        public boolean logData = false; // whether datalog is enabled or not
    }

    private class ControllerLogDataProvider implements LogDataProvider {
        @Override
        public String getName() {
            return TrapezoidalProfileFollower.this.getName();
        }

        @Override
        public List<Object> getKeys() {
            return Arrays.asList("tPosition", "tVelocity", "tAcceleration", "aPosition", "aVelocity", "output",
                    "onTarget");
        }

        @Override
        public List<Object> getValues() {
            synchronized (TrapezoidalProfileFollower.this) {
                return Arrays.asList(calculator.getPosition(), calculator.getVelocity(), calculator.getAcceleration(),
                        currentPosition, currentVelocity, output, onTarget);
            }
        }
    }
}
