package org.teamtators.common.scheduler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alex Mikhalev
 */
public class StateCommand<S> extends Command {
    private Initializer<S> initializer;
    private Stepper<S> stepper;
    private Finisher<S> finish;
    private S state;

    public StateCommand(String name, Initializer<S> initializer, Stepper<S> step, Finisher<S> finish) {
        super(name);
        this.initializer = checkNotNull(initializer);
        this.stepper = checkNotNull(step);
        this.finish = checkNotNull(finish);
    }

    public StateCommand(Initializer<S> initializer, Stepper<S> step, Finisher<S> finish) {
        this("StateCommand", initializer, step, finish);
    }

    @Override
    protected void initialize() {
        state = initializer.initialize();
    }

    @Override
    public boolean step() {
        return stepper.step(state);
    }

    @Override
    protected void finish(boolean interrupted) {
        finish.finish(state, interrupted);
    }

    @FunctionalInterface
    public interface Initializer<S> {
        S initialize();
    }

    @FunctionalInterface
    public interface Stepper<S> {
        boolean step(S state);
    }

    @FunctionalInterface
    public interface Finisher<S> {
        void finish(S state, boolean interrupted);
    }
}
