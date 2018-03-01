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
    private DoubleUnaryOperator lookAheadFunction;

    private TrapezoidalProfileFollower speedFollower;
    private DriveSegments segments = new DriveSegments();

    private int currentSegmentIdx;
    private double previousTraveled;
    private double totalLength;
    private PursuitReport report;
    private LookaheadReport lookaheadReport;
    private double speedPower;

    public DriveSegmentsFollower(TankDrive drive) {
        super("DriveSegmentsFollower");
        this.drive = drive;

        speedFollower = new TrapezoidalProfileFollower("DriveSegmentsFollower.speedFollower");
        speedFollower.setPositionProvider(this::getTraveledDistance);
        speedFollower.setVelocityProvider(() ->
                report.isReverse ? -drive.getCenterRate() : drive.getCenterRate());
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

    public DoubleUnaryOperator getLookAheadFunction() {
        return lookAheadFunction;
    }

    public void setLookAheadFunction(DoubleUnaryOperator lookAheadFunction) {
        this.lookAheadFunction = lookAheadFunction;
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
        logger.debug("driving segment {}", seg);
        speedFollower.setTravelVelocity(seg.getTravelSpeed());
        speedFollower.setEndVelocity(seg.getEndSpeed());
        speedFollower.moveToPosition(previousTraveled + lookaheadReport.remainingDistance);
    }

    void updatePursuitReport(Pose2d currentPose, double centerWheelRate) {
        if (report.isFinished) {
            return;
        }
        if (currentSegmentIdx < 0) {
            currentSegmentIdx++;
            report.updateProfile = true;
        }
        if (!hasSegment()) {
            report.isFinished = true;
            return;
        }
        double lookahead = lookAheadFunction.applyAsDouble(Math.abs(centerWheelRate));
        DriveSegment currentSegment = getCurrentSegment();
        Pose2d pose = currentSegment.isReverse() ? currentPose.invertYaw() : currentPose;
        lookaheadReport = currentSegment.getLookaheadReport(pose, lookahead);
        while (Epsilon.isEpsilonNegative(lookaheadReport.remainingDistance)) {
            currentSegmentIdx++;
            if (!hasSegment()) {
                report.isFinished = true;
                break;
            } else {
                previousTraveled += lookaheadReport.traveledDistance;
                currentSegment = getCurrentSegment();
                pose = currentSegment.isReverse() ? currentPose.invertYaw() : currentPose;
                lookaheadReport = currentSegment.getLookaheadReport(pose, lookahead);
                report.updateProfile = true;
            }
        }
        report.traveledDistance = previousTraveled + lookaheadReport.traveledDistance;
        report.remainingDistance = totalLength - report.traveledDistance;
        report.nearestPoint = lookaheadReport.nearestPoint;
        report.lookaheadPoint = lookaheadReport.lookaheadPoint;
        report.trackError = lookaheadReport.trackError;
        report.yawError = lookaheadReport.yawError;
        report.isReverse = currentSegment.isReverse();
        if (lookaheadReport.lookaheadRemainingDistance < 0 && hasNextSegment()) {
            DriveSegment nextSegment = getNextSegment();
            double nextLookahead = -lookaheadReport.lookaheadRemainingDistance;
            report.lookaheadPoint = nextSegment
                    .getLookAhead(nextSegment.getStartPose(), nextLookahead);
        }
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

    @Override
    public synchronized void start() {
        if (!running) {
            running = true;
            reset();
            speedFollower.start();
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        speedFollower.stop();
    }

    public boolean isFinished() {
        return report.isFinished/* && speedFollower.isFinished()*/;
    }

    @Override
    protected void doUpdate(double delta) {
        if (isFinished()) {
            drive.stop();
            stop();
        }
        Pose2d currentPose = drive.getPose();
        double centerWheelRate = drive.getCenterRate();
        updatePursuitReport(currentPose, centerWheelRate);
        if (report.updateProfile) {
            updateProfile();
        }
//        logger.debug("currentPose: {}, report: {}", currentPose, report);
        Twist2d twist = Twist2d.fromTangentArc(currentPose, report.lookaheadPoint.getTranslation());
        if (report.isReverse) {
            twist = twist.invert();
        }
        speedFollower.update(delta);
        DriveOutputs driveOutputs = drive.getTankKinematics().calculateOutputs(twist, speedPower);
//        logger.debug("driving with {}, power {}, outputs {}", twist, speedPower, driveOutputs);
        drive.setPowers(driveOutputs);
    }

    @Override
    public void configure(Config config) {
        setLookAheadFunction(config.lookAhead);
        speedFollower.configure(config.speedFollower);
    }

    public static class Config {
        public LinearInterpolationFunction lookAhead;
        public TrapezoidalProfileFollower.Config speedFollower;
    }
}
