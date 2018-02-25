package org.teamtators.levitator.subsystems;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.AbstractUpdatable;
import org.teamtators.common.control.PidController;
import org.teamtators.common.control.TrapezoidalProfileFollower;

import java.util.function.Predicate;

public class DriveArcController extends AbstractUpdatable implements Configurable<DriveArcController.Config> {
    private final Drive drive;
    private final TrapezoidalProfileFollower leftMotionFollower = new TrapezoidalProfileFollower("Drive.leftMotionFollower");
    private final TrapezoidalProfileFollower rightMotionFollower = new TrapezoidalProfileFollower("Drive.rightMotionFollower");
    private final PidController yawAngleController = new PidController("Drive.yawAngleController");
    private final OutputController outputController = new OutputController();

    private Config config;
    private Predicate<DriveArcController> onTargetPredicate = DriveArcController::areStraightsOnTarget;
    private double deltaHeading;
    private double deltaCenterDistance;

    private double initialYawAngle;
    private boolean onTarget;

    public DriveArcController(Drive drive) {
        super("DriveArcController");
        this.drive = drive;

        leftMotionFollower.setPositionProvider(drive::getLeftDistance);
        leftMotionFollower.setVelocityProvider(drive::getLeftRate);
        rightMotionFollower.setPositionProvider(drive::getRightDistance);
        rightMotionFollower.setVelocityProvider(drive::getRightRate);
        yawAngleController.setInputProvider(drive::getYawAngle);

        rightMotionFollower.setOutputConsumer(outputController::setRightOutput);
        leftMotionFollower.setOutputConsumer(outputController::setLeftOutput);
        yawAngleController.setOutputConsumer(outputController::setRotationOutput);
    }

    public double getInitialYawAngle() {
        return initialYawAngle;
    }

    public void setInitialYawAngle(double initialYawAngle) {
        this.initialYawAngle = initialYawAngle;
    }

    public void setDeltaHeading(double deltaHeading) {
        this.deltaHeading = deltaHeading;
    }

    public void setDeltaCenterDistance(double deltaCenterDistance) {
        this.deltaCenterDistance = deltaCenterDistance;
    }

    public double getDeltaHeading() {
        return deltaHeading;
    }

    public double getDeltaCenterDistance() {
        return deltaCenterDistance;
    }

    @Override
    public synchronized void start() {
        double sideDelta = .5 * config.effectiveTrackWidth * Math.toRadians(deltaHeading);
        double leftDistance = deltaCenterDistance + sideDelta;
        double rightDistance = deltaCenterDistance - sideDelta;
        leftMotionFollower.moveDistance(leftDistance);
        rightMotionFollower.moveDistance(rightDistance);

        double leftTime = leftMotionFollower.getCalculator().getTotalTime();
        double rightTime = rightMotionFollower.getCalculator().getTotalTime();
        logger.trace("Initial left, right times: {}, {}", leftTime, rightTime);
        TrapezoidalProfileFollower slowSide, fastSide;
        double slowDistance;
        if (leftTime > rightTime) {
            slowSide = rightMotionFollower;
            slowDistance = rightDistance;
            fastSide = leftMotionFollower;
        } else {
            slowSide = leftMotionFollower;
            slowDistance = leftDistance;
            fastSide = rightMotionFollower;
        }
        if (leftTime != rightTime) {
            double start_a = slowSide.getMaxAcceleration();
            double end_a = -slowSide.getMaxAcceleration();
            double start_v = slowSide.getInitialVelocity();
            double end_v = slowSide.getEndVelocity();
            double tt = fastSide.getCalculator().getTotalTime();
            double distance = slowDistance;
            double max_velocity =
                    (-2 * end_v * start_a + 2 * end_a * start_v + 2 * end_a * start_a * tt +
                            Math.sqrt(-4 * (-end_a + start_a) * (-2 * distance * end_a * start_a +
                                    Math.pow(end_v, 2) * start_a - end_a * Math.pow(start_v, 2)) +
                                    Math.pow(-2 * end_v * start_a + 2 * end_a * start_v + 2 * end_a * start_a * tt, 2))) /
                                    (2 * (end_a - start_a));
            slowSide.setTravelVelocity(max_velocity);
            slowSide.moveDistance(slowDistance);
            double slowTime = rightMotionFollower.getCalculator().getTotalTime();
            if (leftTime > rightTime) {
                rightTime = slowTime;
                logger.trace("After slowing down right to max vel {}; left, right times: {}, {}",
                        max_velocity, leftTime, rightTime);
            } else if (rightTime > leftTime) {
                leftTime = slowTime;
                logger.trace("After slowing down left to max vel {}; left, right times: {}, {}",
                        max_velocity, leftTime, rightTime);
            }
        }

        yawAngleController.setSetpoint(initialYawAngle);
        super.start();
        leftMotionFollower.start();
        rightMotionFollower.start();
        yawAngleController.start();
        outputController.start();
        onTarget = false;
    }

    @Override
    public synchronized void stop() {
        super.stop();
        leftMotionFollower.stop();
        rightMotionFollower.stop();
        yawAngleController.stop();
        outputController.stop();
    }

    @Override
    protected void doUpdate(double delta) {
        leftMotionFollower.update(delta);
        rightMotionFollower.update(delta);

        double leftDelta = leftMotionFollower.getCalculator().getPosition();
        double rightDelta = rightMotionFollower.getCalculator().getPosition();
        double angleDelta = Math.toDegrees((leftDelta - rightDelta) / config.effectiveTrackWidth);
        yawAngleController.setSetpoint(initialYawAngle + angleDelta);
        yawAngleController.update(delta);

        onTarget = onTargetPredicate.test(this);
        if (onTarget) {
            stop();
        }

        outputController.update(delta);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        this.leftMotionFollower.configure(config.straightMotionFollower);
        this.rightMotionFollower.configure(config.straightMotionFollower);
        this.yawAngleController.configure(config.yawAngleController);

    }

    public PidController getYawAngleController() {
        return yawAngleController;
    }

    public TrapezoidalProfileFollower getLeftMotionFollower() {
        return leftMotionFollower;
    }

    public TrapezoidalProfileFollower getRightMotionFollower() {
        return rightMotionFollower;
    }

    public boolean isLeftOnTarget() {
        return leftMotionFollower.isOnTarget();
    }

    public boolean isRightOnTarget() {
        return rightMotionFollower.isOnTarget();
    }

    public boolean areStraightsOnTarget() {
        return isLeftOnTarget() && isRightOnTarget();
    }

    public boolean isOnTarget() {
        return onTarget;
    }

    public double getLeftDistance() {
        return leftMotionFollower.getCurrentPosition();
    }

    public double getRightDistance() {
        return rightMotionFollower.getCurrentPosition();
    }

    public double getAverageDistance() {
        return (getLeftDistance() + getRightDistance()) / 2.0;
    }

    public void setMaxSpeed(double maxSpeed) {
        leftMotionFollower.setTravelVelocity(maxSpeed);
        rightMotionFollower.setTravelVelocity(maxSpeed);
    }

    public void setMaxAcceleration(double maxAcceleration) {
        leftMotionFollower.setMaxAcceleration(maxAcceleration);
        rightMotionFollower.setMaxAcceleration(maxAcceleration);
    }

    public void setEndVelocity(double endVelocity) {
        leftMotionFollower.setEndVelocity(endVelocity);
        rightMotionFollower.setEndVelocity(endVelocity);
    }

    public void setOnTargetPredicate(Predicate<DriveArcController> onTargetPredicate) {
        this.onTargetPredicate = onTargetPredicate;
    }

    public Predicate<DriveArcController> getOnTargetPredicate() {
        return onTargetPredicate;
    }

    public static class Config {
        public double effectiveTrackWidth;
        public double scrubCoefficient;
        public TrapezoidalProfileFollower.Config straightMotionFollower;
        public PidController.Config yawAngleController;
    }

    private class OutputController extends AbstractUpdatable {
        private double leftOutput;
        private double rightOutput;
        private double rotationOutput;

        public void setStraightOutput(double straightOutput) {
            setLeftOutput(straightOutput);
            setRightOutput(straightOutput);
        }

        public void setLeftOutput(double leftOutput) {
            this.leftOutput = leftOutput;
        }

        public void setRightOutput(double rightOutput) {
            this.rightOutput = rightOutput;
        }

        public void setRotationOutput(double rotationOutput) {
            this.rotationOutput = rotationOutput;
        }

        @Override
        protected void doUpdate(double delta) {
            double left = 0, right = 0;

            left += leftOutput;
            right += rightOutput;
            leftOutput = /*Double.NaN*/ 0.0;
            rightOutput = /*Double.NaN*/ 0.0;

            left += rotationOutput;
            right -= rotationOutput;
            rotationOutput = /*Double.NaN*/ 0.0;

            double scrubPower = (left - right) * config.scrubCoefficient;
            left += scrubPower;
            right -= scrubPower;

            if (!Double.isNaN(left) && !Double.isNaN(right)) {
                drive.setLeftMotorPower(left);
                drive.setRightMotorPower(right);
                logger.trace("driving at powers {}, {}", left, right);
            } else {
                drive.setLeftMotorPower(0.0);
                drive.setRightMotorPower(0.0);
                logger.trace("not driving, something was NaN: {}, {}", left, right);
            }
        }
    }
}
