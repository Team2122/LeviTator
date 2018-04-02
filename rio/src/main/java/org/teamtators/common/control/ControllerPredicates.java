package org.teamtators.common.control;

import java.util.Arrays;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ControllerPredicates {
    public static <T> Predicate<T> staticValue(boolean value) {
        return (controller) -> value;
    }

    public static <T> Predicate<T> alwaysTrue() {
        return staticValue(true);
    }

    public static <T> Predicate<T> alwaysFalse() {
        return staticValue(false);
    }

    public static Predicate<AbstractController> withinError(double threshold) {
        return (controller) -> Math.abs(controller.getError()) < threshold;
    }

    public static Predicate<AbstractController> sampleWithinError(double time, double threshold) {
        return new SampleTime<>(time, withinError(threshold));
    }

    public static Predicate<AbstractController> withinPercentage(double percentage) {
        return (controller -> Math.abs(controller.getError() / controller.getSetpoint()) < percentage);
    }

    public static Predicate<AbstractController> sampleWithinPercentage(double time, double percentage) {
        return new SampleTime<>(time, withinPercentage(percentage));
    }

    public static Predicate<TrapezoidalProfileFollower> finished() {
        return TrapezoidalProfileFollower::isFinished;
    }

    public static Predicate<TrapezoidalProfileFollower> positionWithin(double positionTolerance) {
        return follower -> Math.abs(follower.getPositionError()) <= positionTolerance;
    }

    public static Predicate<TrapezoidalProfileFollower> velocityWithin(double velocityTolerance) {
        return follower -> Math.abs(follower.getVelocityError()) <= velocityTolerance;
    }

    @SafeVarargs
    public static <T> Predicate<T> and(Predicate<? super T>... predicates) {
        return o -> Arrays.stream(predicates).allMatch(predicate -> predicate.test(o));
    }

    @SafeVarargs
    public static <T> Predicate<T> or(Predicate<? super T>... predicates) {
        return o -> Arrays.stream(predicates).anyMatch(predicate -> predicate.test(o));
    }

    public static <T extends AbstractUpdatable> Predicate<T> sampleTime(double time, Predicate<T> basePredicate) {
        return new SampleTime<>(time, basePredicate);
    }

    public static class SampleTime<TController extends AbstractUpdatable> implements Predicate<TController> {
        private double currentTime;
        private double time;
        private Predicate<TController> basePredicate;

        public SampleTime(double time, Predicate<TController> basePredicate) {
            this.time = time;
            this.basePredicate = checkNotNull(basePredicate);
        }

        @Override
        public boolean test(TController controller) {
            if (basePredicate.test(controller)) {
                currentTime += controller.getLastDelta();
                return currentTime > time;
            } else {
                currentTime = 0.0;
            }
            return false;
        }
    }
}
