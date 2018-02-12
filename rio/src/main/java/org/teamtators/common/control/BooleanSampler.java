package org.teamtators.common.control;

import com.google.common.base.Preconditions;
import org.teamtators.common.config.Configurable;

import java.util.function.BooleanSupplier;

public class BooleanSampler implements Configurable<BooleanSampler.Config> {
    private BooleanSupplier source;
    private double period;
    private Timer timer = new Timer();

    public BooleanSampler(BooleanSupplier source, double period) {
        Preconditions.checkArgument(period >= 0, "period must be positive");
        Preconditions.checkNotNull(source);
        this.source = source;
        this.period = period;
    }

    public BooleanSampler(BooleanSupplier source) {
        this(source, 0.0);
    }

    public boolean get() {
        if (source.getAsBoolean()) {
            if (!timer.isRunning()) {
                timer.start();
            }
            if (timer.hasPeriodElapsed(this.period)) {
                return true;
            }
        } else {
            timer.stop();
        }
        return false;
    }

    public double getPeriod() {
        return period;
    }

    public void setPeriod(double period) {
        this.period = period;
    }

    @Override
    public void configure(Config config) {
        this.period = config.period;
    }

    public static class Config {
        public double period;
    }
}
