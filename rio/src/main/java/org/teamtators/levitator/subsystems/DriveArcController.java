package org.teamtators.levitator.subsystems;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.AbstractUpdatable;
import org.teamtators.common.control.PidController;
import org.teamtators.common.control.TrapezoidalProfileFollower;

class DriveArcController extends AbstractUpdatable implements Configurable<DriveArcController.Config> {
    private final Drive drive;
    private final TrapezoidalProfileFollower leftMotionFollower = new TrapezoidalProfileFollower("Drive.leftMotionFollower");
    private final TrapezoidalProfileFollower rightMotionFollower = new TrapezoidalProfileFollower("Drive.rightMotionFollower");
    private final PidController yawAngleController = new PidController("Drive.yawAngleController");
    private final OutputController outputController;

    private Config config;
    private double deltaHeading;
    private double deltaCenterDistance;

    private double forcedInitialYawAngle;
    private double initialYawAngle;
    private double leftDistance;
    private double rightDistance;

    public DriveArcController(Drive drive) {
        super("DriveArcController");
        this.drive = drive;
        outputController = new OutputController(drive);

        leftMotionFollower.setPositionProvider(drive::getLeftDistance);
        leftMotionFollower.setVelocityProvider(drive::getLeftRate);
        rightMotionFollower.setPositionProvider(drive::getRightDistance);
        rightMotionFollower.setVelocityProvider(drive::getRightRate);
        yawAngleController.setInputProvider(drive::getYawAngle);

        rightMotionFollower.setOutputConsumer(outputController::setRightOutput);
        leftMotionFollower.setOutputConsumer(outputController::setLeftOutput);
        yawAngleController.setOutputConsumer(outputController::setRotationOutput);

        resetForcedInitialYawAngle();
    }

    public double getForcedInitialYawAngle() {
        return forcedInitialYawAngle;
    }

    public void setForcedInitialYawAngle(double forcedInitialYawAngle) {
        this.forcedInitialYawAngle = forcedInitialYawAngle;
    }

    public void resetForcedInitialYawAngle() {
        setForcedInitialYawAngle(Double.NaN);
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

    public void setParameters(double deltaHeading, double deltaCenterDistance) {
        setDeltaHeading(deltaHeading);
        setDeltaCenterDistance(deltaCenterDistance);
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

    @Override
    public synchronized void start() {
        double sideDelta = config.effectiveTrackWidth * Math.toRadians(deltaHeading);
        leftDistance = deltaCenterDistance + .5 * sideDelta;
        rightDistance = deltaCenterDistance - .5 * sideDelta;
        leftMotionFollower.moveDistance(leftDistance);
        rightMotionFollower.moveDistance(rightDistance);
        initialYawAngle = drive.getYawAngle();
        yawAngleController.setSetpoint(initialYawAngle);
        super.start();
        leftMotionFollower.start();
        rightMotionFollower.start();
        yawAngleController.start();
        outputController.start();
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
        double angleDelta = (leftDelta - rightDelta) / config.effectiveTrackWidth;
        yawAngleController.setSetpoint(initialYawAngle + angleDelta);
        yawAngleController.update(delta);

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

    public static class Config {
        public double effectiveTrackWidth;
        public TrapezoidalProfileFollower.Config straightMotionFollower;
        public PidController.Config yawAngleController;
    }

    private static class OutputController extends AbstractUpdatable {
        private double leftOutput;
        private double rightOutput;
        private double rotationOutput;
        private Drive drive;

        public OutputController(Drive drive) {
            this.drive = drive;
        }

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
            leftOutput = Double.NaN;
            rightOutput = Double.NaN;
            left += rotationOutput;
            right -= rotationOutput;
            rotationOutput = Double.NaN;

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
