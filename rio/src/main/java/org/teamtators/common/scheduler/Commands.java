package org.teamtators.common.scheduler;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

public class Commands {
    private static AtomicInteger nextLogCommandNumber = new AtomicInteger(1);

    public static Command instant(Runnable function) {
        return instant(function, null);
    }

    public static Command instant(Runnable function, Subsystem requirement) {
        StateCommand<Void> command = new StateCommand<>(() -> {
            function.run();
            return null;
        }, s -> true, (s, i) -> {
        });
        if (requirement != null)
            command.requires(requirement);
        return command;
    }

    public static Command stateless(BooleanSupplier step, Runnable initialize, Runnable finish, Subsystem requirement) {
        StateCommand<Void> command = new StateCommand<>(() -> {
            initialize.run();
            return null;
        }, s -> step.getAsBoolean(), (s, i) -> {
            finish.run();
        });
        if (requirement != null)
            command.requires(requirement);
        return command;
    }

    public static Command stateless(BooleanSupplier step, Runnable initialize, Runnable finish) {
        return stateless(step, initialize, finish, null);
    }

    public static Command stateless(BooleanSupplier step, Runnable initialize) {
        return stateless(step, initialize, () -> {
        });
    }

    public static Command stateless(BooleanSupplier step) {
        return stateless(step, () -> {
        }, () -> {
        });
    }

    public static <S> Command stateful(Supplier<S> initialize, Predicate<S> step, BiConsumer<S, Boolean> finish) {
        return new StateCommand<S>(initialize::get, step::test, finish::accept);
    }

    public static <S> Command stateful(Supplier<S> initialize, Predicate<S> step, Consumer<S> finish) {
        return new StateCommand<S>(initialize::get, step::test, (s, i) -> finish.accept(s));
    }

    public static <S> Command stateful(Supplier<S> initialize, Predicate<S> step) {
        return new StateCommand<S>(initialize::get, step::test, (s, i) -> {
        });
    }

    public static Command sequence(Command... sequence) {
        return new SequentialCommand(sequence);
    }

    public static Command log(String message) {
        return new LogCommand("LogCommand" + nextLogCommandNumber.getAndIncrement(), message);
    }

}
