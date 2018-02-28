package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.Translation2d;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.teamtators.common.math.Epsilon.isEpsilonZero;

/**
 * @author Alex Mikhalev
 */
public class DriveSegmentsFollowerTest {
    @Test
    public void testGetPursuitReport() throws Exception {
        DriveSegments segments = DrivePathTest.getTestPath().toSegments();
        DriveSegmentsFollower follower = new DriveSegmentsFollower();
        follower.setSegments(segments);
        follower.setLookaheadFunction(operand -> 1);

        Pose2d currentPose = new Pose2d(Translation2d.zero(), Rotation.fromDegrees(90));
        while (true) {
            PursuitReport report = follower.getPursuitReport(currentPose, 0.0);
            System.out.println(report);
            if (report.isFinished) {
                break;
            }
            currentPose = report.lookaheadPoint;
            if (!isEpsilonZero(report.trackError)) {
                Assert.fail("trackError not zero: " + report.trackError);
            }
            if (!isEpsilonZero(report.yawError.toRadians())) {
                Assert.fail("yawError not zero: " + report.yawError);
            }
        }
    }
}