package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.Translation2d;
import org.teamtators.common.math.Twist2d;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.teamtators.common.math.Epsilon.isEpsilonZero;

/**
 * @author Alex Mikhalev
 */
public class DriveSegmentsFollowerTest {
    @Test
    public void testGetPursuitReport() throws Exception {
        DriveSegments segments = DrivePathTest.getTestPath().toSegments();
        DriveSegmentsFollower follower = new DriveSegmentsFollower(null);
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

    static class TwistTestCase {
        public Pose2d pose;
        public Translation2d point;
        public Twist2d twist;

        public TwistTestCase(Pose2d pose, Translation2d point, Twist2d twist) {
            this.pose = pose;
            this.point = point;
            this.twist = twist;
        }
    }

    @Test
    public void testGetTwist() throws Exception {
        List<TwistTestCase> testCases = Arrays.asList(
                new TwistTestCase(
                        new Pose2d(Translation2d.zero(), Rotation.fromDegrees(0)),
                        new Translation2d(10, 10),
                        new Twist2d(Rotation.fromDegrees(90), 15.707963267948966)
                ),
                new TwistTestCase(
                        new Pose2d(Translation2d.zero(), Rotation.fromDegrees(0)),
                        new Translation2d(-10, 10),
                        new Twist2d(Rotation.fromDegrees(-90), -15.707963267948966)
                ),
                new TwistTestCase(
                        new Pose2d(Translation2d.zero(), Rotation.fromDegrees(124)),
                        new Translation2d(-10.134, 10.1345),
                        new Twist2d(Rotation.fromDegrees(21.997173161334036), 14.42039418592253)
                ),
                new TwistTestCase(
                        new Pose2d(new Translation2d(10, -423), Rotation.fromDegrees(124)),
                        new Translation2d(-10.134, 10.1345),
                        new Twist2d(Rotation.fromDegrees(-20.235074533011908), -378.15579443370166)
                )
        );

        for (TwistTestCase testCase : testCases) {
            Twist2d actualTwist = DriveSegmentsFollower.getTwist(testCase.pose, testCase.point);
            Assert.assertTrue(actualTwist.epsilonEquals(testCase.twist),
                    "Expected twist: " + testCase.twist + ", actual twist: " + actualTwist);
        }
    }
}