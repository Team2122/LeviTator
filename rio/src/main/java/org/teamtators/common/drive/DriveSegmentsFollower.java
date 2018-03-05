package org.teamtators.common.drive;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.AbstractUpdatable;
import org.teamtators.common.control.TrapezoidalProfileFollower;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.LogDataProvider;
import org.teamtators.common.math.*;

import java.util.Arrays;
import java.util.List;
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
    private LogDataProvider logDataProvider = new LogDataProvder();
    private Pose2d currentPose;
    private Twist2d twist;
    private DriveOutputs driveOutputs;

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
        if (!hasSegment()) {
            logger.warn("can not update profile when does not have segment");
            return;
        }
        DriveSegment seg = getCurrentSegment();
        speedFollower.setTravelVelocity(seg.getTravelSpeed());
        speedFollower.setEndVelocity(seg.getEndSpeed());
        speedFollower.moveToPosition(previousTraveled + lookaheadReport.remainingDistance);
        logger.debug("driving segment \n{} with profile \n{}", seg, speedFollower.getCalculator().getProfile());
    }

    void updatePursuitReport(Pose2d currentPose, double centerWheelRate) {
        if (report.isFinished) {
            return;
        }
        report = new PursuitReport();
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
            speedFollower.reset();
            speedFollower.start();
            DataCollector.getDataCollector().startProvider(logDataProvider);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        speedFollower.stop();
        DataCollector.getDataCollector().stopProvider(logDataProvider);
    }

    public boolean isFinished() {
        return report.isFinished/* && speedFollower.isFinished()*/;
    }

    public boolean isOnTarget() {
        return report.remainingDistance < 0.1;
    }

    @Override
    protected void doUpdate(double delta) {
        if (isFinished()) {
            drive.stop();
            stop();
            return;
        }
        currentPose = drive.getPose();
        double centerWheelRate = drive.getCenterRate();
        updatePursuitReport(currentPose, centerWheelRate);
        if (!isFinished() && report.updateProfile) {
            updateProfile();
        }
        twist = Twist2d.fromTangentArc(currentPose, report.lookaheadPoint.getTranslation());
        if (report.isReverse) {
            twist = new Twist2d(twist.getDeltaYaw(), -twist.getDeltaX());
        }
        speedFollower.update(delta);
        driveOutputs = drive.getTankKinematics().calculateOutputs(twist, speedPower);
        drive.setPowers(driveOutputs);
        if (isOnTarget()) {
            report.isFinished = true;
        }
    }

    @Override
    public void configure(Config config) {
        setLookAheadFunction(config.lookAhead);
        speedFollower.configure(config.speedFollower);
    }

    public PursuitReport getReport() {
        return report;
    }

    public static class Config {
        public LinearInterpolationFunction lookAhead;
        public TrapezoidalProfileFollower.Config speedFollower;
    }

    private class LogDataProvder implements LogDataProvider {
        @Override
        public String getName() {
            return DriveSegmentsFollower.this.getName();
        }

        @Override
        public List<Object> getKeys() {
            return Arrays.asList("traveledDistance", "remainingDistance", "currentPose", "nearestPoint", "lookaheadPoint",
                    "twist", "speedPower", "driveOutputs");
        }

        @Override
        public List<Object> getValues() {
            return Arrays.asList(report.traveledDistance, report.remainingDistance, currentPose, report.nearestPoint, report.lookaheadPoint,
                    twist, speedPower, driveOutputs);
        }
    }
}
