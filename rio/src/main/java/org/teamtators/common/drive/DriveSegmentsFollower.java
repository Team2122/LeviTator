package org.teamtators.common.drive;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.AbstractUpdatable;
import org.teamtators.common.control.TrapezoidalProfileFollower;
import org.teamtators.common.math.*;

import java.util.function.DoubleUnaryOperator;

/**
 * @author Alex Mikhalev
 */
public class DriveSegmentsFollower extends AbstractUpdatable
        implements Configurable<DriveSegmentsFollower.Config> {
    private final TankDrive drive;
    private DoubleUnaryOperator lookaheadFunction;

    private TrapezoidalProfileFollower speedFollower;
    private DriveSegments segments = new DriveSegments();

    private int currentSegmentIdx;
    private double previousTraveled;
    private double totalLength;
    private PursuitReport report;
    private double speedPower;

    public DriveSegmentsFollower(TankDrive drive) {
        super("DriveSegmentsFollower");
        this.drive = drive;

        speedFollower = new TrapezoidalProfileFollower("DriveSegmentsFollower.speedFollower");
        speedFollower.setPositionProvider(this::getTraveledDistance);
        speedFollower.setVelocityProvider(drive::getCenterRate);
        speedFollower.setOutputConsumer(this::setSpeedPower);
        speedFollower.setOnTargetPredicate(t -> false);

        reset();
    }

    private void setSpeedPower(double speedPower) {
        this.speedPower = speedPower;
    }

    public void reset() {
        currentSegmentIdx = -1;
        previousTraveled = 0.0;
        totalLength = segments.getArcLength();
        report = new PursuitReport();
        setSpeedPower(0.0);
    }

    public DriveSegments getSegments() {
        return segments;
    }

    public void setSegments(DriveSegments segments) {
        this.segments = segments;
        reset();
    }

    public DoubleUnaryOperator getLookaheadFunction() {
        return lookaheadFunction;
    }

    public void setLookaheadFunction(DoubleUnaryOperator lookaheadFunction) {
        this.lookaheadFunction = lookaheadFunction;
    }

    public TrapezoidalProfileFollower getSpeedFollower() {
        return speedFollower;
    }

    public void setMaxAcceleration(double maxAcceleration) {
        speedFollower.setMaxAcceleration(maxAcceleration);
    }

    public double getMaxAcceleration() {
        return speedFollower.getMaxAcceleration();
    }

    private void updateProfile() {
        DriveSegment seg = getCurrentSegment();
        speedFollower.setTravelVelocity(seg.getTravelSpeed());
        speedFollower.setEndVelocity(seg.getEndSpeed());
        speedFollower.moveDistance(seg.getArcLength());
    }

    PursuitReport getPursuitReport(Pose2d currentPose, double centerWheelRate) {
        if (currentSegmentIdx < 0) {
            currentSegmentIdx++;
            updateProfile();
        }
        PursuitReport report = new PursuitReport();
        if (!hasSegment()) {
            report.isFinished = true;
            return report;
        }
        DriveSegment currentSegment = getCurrentSegment();
        double lookahead = lookaheadFunction.applyAsDouble(centerWheelRate);
        LookaheadReport lookaheadReport = currentSegment.getLookaheadReport(currentPose, lookahead);
        if (Epsilon.isEpsilonNegative(lookaheadReport.remainingDistance)) {
            currentSegmentIdx++;
            if (!hasSegment()) {
                report.isFinished = true;
            } else {
                previousTraveled += lookaheadReport.traveledDistance;
                currentSegment = getCurrentSegment();
                lookaheadReport = currentSegment.getLookaheadReport(currentPose, lookahead);
                updateProfile();
            }
        }
        report.traveledDistance = previousTraveled + lookaheadReport.traveledDistance;
        report.remainingDistance = totalLength - report.traveledDistance;
        report.nearestPoint = lookaheadReport.nearestPoint;
        report.lookaheadPoint = lookaheadReport.lookaheadPoint;
        report.trackError = lookaheadReport.trackError;
        report.yawError = lookaheadReport.yawError;
        if (lookaheadReport.lookaheadRemainingDistance < 0 && hasNextSegment()) {
            DriveSegment nextSegment = getNextSegment();
            double nextLookahead = -lookaheadReport.lookaheadRemainingDistance;
            report.lookaheadPoint = nextSegment
                    .getLookAhead(nextSegment.getStartPose(), nextLookahead);
        }
        return report;
    }

    private DriveSegment getNextSegment() {
        return segments.getSegments().get(currentSegmentIdx + 1);
    }

    private DriveSegment getCurrentSegment() {
        return segments.getSegments().get(currentSegmentIdx);
    }

    private boolean hasSegment() {
        return currentSegmentIdx < getSegments().getSegments().size();
    }

    private boolean hasNextSegment() {
        return currentSegmentIdx + 1 < getSegments().getSegments().size();
    }

    private double getTraveledDistance() {
        if (report == null) {
            return 0.0;
        }
        return report.traveledDistance;
    }

    static Twist2d getTwist(Pose2d currentPose, Translation2d lookaheadPoint) {
        Translation2d diff = lookaheadPoint.sub(currentPose.getTranslation());
        Translation2d halfDiff = diff.scale(0.5);
        Pose2d perpBisector = new Pose2d(halfDiff, diff.getDirection().ccwNormal());
        Rotation startHeading = currentPose.getYaw();
        Rotation startNormal = startHeading.ccwNormal();
        Pose2d startNormalLine = new Pose2d(currentPose.getTranslation(), startNormal);
        Translation2d center = perpBisector.getIntersection(startNormalLine);
        Twist2d twist = new Twist2d();
        if (center.isNaN()) { // if the radii don't intersect, it is a straight line
            twist.setDeltaYaw(Rotation.identity());
            twist.setDeltaX(diff.getMagnitude());
        } else {
            boolean isCcw = startNormalLine.getDistanceAhead(center) > 0;
            double radius = lookaheadPoint.sub(center).getMagnitude();
            Rotation endNormal = lookaheadPoint.sub(center).getDirection();
            Rotation endHeading = isCcw ? endNormal.ccwNormal() : endNormal.cwNormal();
            Rotation deltaHeading = endHeading.sub(startHeading);
            double arcLength = deltaHeading.toRadians() * radius;
            twist.setDeltaYaw(deltaHeading);
            twist.setDeltaX(arcLength);
        }
        return twist;
    }

    @Override
    public synchronized void start() {
        if (!running) {
            running = true;
            reset();
        }
    }

    public boolean isFinished() {
        return report.isFinished && speedFollower.isFinished();
    }

    @Override
    protected void doUpdate(double delta) {
        if (isFinished()) {
            drive.stop();
            stop();
        }
        Pose2d currentPose = drive.getPose();
        double centerWheelRate = drive.getCenterRate();
        report = getPursuitReport(currentPose, centerWheelRate);
        Twist2d twist = getTwist(currentPose, report.lookaheadPoint.getTranslation());
        speedFollower.update(delta);
        DriveOutputs driveOutputs = drive.getTankKinematics().calculateOutputs(twist, speedPower);
        drive.setPowers(driveOutputs);
    }

    @Override
    public void configure(Config config) {
        setMaxAcceleration(config.maxAcceleration);
        setLookaheadFunction(config.lookahead);
        speedFollower.configure(config.speedController);
    }

    public static class Config {
        public double maxAcceleration;
        public LinearInterpolationFunction lookahead;
        public TrapezoidalProfileFollower.Config speedController;
    }
}
