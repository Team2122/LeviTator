package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;

public interface DriveSegment {
    Pose2d getStartPose();
    Pose2d getEndPose();
    double getArcLength();

    LookaheadReport getLookaheadReport(Pose2d currentPose, double lookaheadDistance);

    Pose2d getLookAhead(Pose2d nearestPoint, double lookahead);
}
