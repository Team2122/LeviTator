package org.teamtators.common.util;

import com.google.common.base.Preconditions;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;

import java.util.function.BooleanSupplier;

public class BooleanSampler implements Configurable<BooleanSampler.Config> {
    private BooleanSupplier source;
    private double minPeriod;
    private Timer timer = new Timer();

    public BooleanSampler(BooleanSupplier source, double minPeriod) {
        Preconditions.checkArgument(minPeriod >= 0, "minPeriod must be positive");
        Preconditions.checkNotNull(source);
        this.source = source;
        this.minPeriod = minPeriod;
    }

    public BooleanSampler(BooleanSupplier source) {
        this(source, 0.0);
    }

    public boolean get() {
        if (source.getAsBoolean()) {
            if (!timer.isRunning()) {
                timer.start();
            }
            if (timer.periodically(minPeriod)) {
                return true;
            }
        } else {
            timer.reset();
        }
        return false;
    }

    @Override
    public void configure(Config config) {
        this.minPeriod = config.minPeriod;
    }

    public static class Config {
        public double minPeriod;
    }
}
