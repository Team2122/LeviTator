package org.teamtators.common.drive;

import org.teamtators.common.math.Epsilon;
import org.teamtators.common.math.Pose2d;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * @author Alex Mikhalev
 */
public class DriveSegmentsFollower {
    private DriveSegments segments;
    private DoubleUnaryOperator lookaheadFunction;

    private int currentSegmentIdx;
    private double previousTraveled;
    private double totalLength;

    public void reset() {
        currentSegmentIdx = 0;
        previousTraveled = 0.0;
        totalLength = segments.getArcLength();
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

    public PursuitReport getPursuitReport(Pose2d currentPose, double centerWheelRate) {
        PursuitReport report = new PursuitReport();
        List<DriveSegment> segs = this.segments.getSegments();
        if (!hasSegment()) {
            report.isFinished = true;
            return report;
        }
        DriveSegment currentSegment = segs.get(currentSegmentIdx);
        double lookahead = lookaheadFunction.applyAsDouble(centerWheelRate);
        LookaheadReport lookaheadReport = currentSegment.getLookaheadReport(currentPose, lookahead);
        if (Epsilon.isEpsilonNegative(lookaheadReport.remainingDistance)) {
            currentSegmentIdx++;
            if (!hasSegment()) {
                report.isFinished = true;
            } else {
                previousTraveled += lookaheadReport.traveledDistance;
                currentSegment = segs.get(currentSegmentIdx);
                lookaheadReport = currentSegment.getLookaheadReport(currentPose, lookahead);
            }
        }
        report.traveledDistance = previousTraveled + lookaheadReport.traveledDistance;
        report.remainingDistance = totalLength - report.traveledDistance;
        report.nearestPoint = lookaheadReport.nearestPoint;
        report.lookaheadPoint = lookaheadReport.lookaheadPoint;
        report.trackError = lookaheadReport.trackError;
        report.yawError = lookaheadReport.yawError;
        if (lookaheadReport.lookaheadRemainingDistance < 0 && hasNextSegment()) {
            DriveSegment nextSegment = segs.get(currentSegmentIdx + 1);
            double nextLookahead = -lookaheadReport.lookaheadRemainingDistance;
            report.lookaheadPoint = nextSegment
                    .getLookAhead(nextSegment.getStartPose(), nextLookahead);
        }
        return report;
    }

    private boolean hasSegment() {
        return currentSegmentIdx < getSegments().getSegments().size();
    }

    private boolean hasNextSegment() {
        return currentSegmentIdx + 1 < getSegments().getSegments().size();
    }
}
