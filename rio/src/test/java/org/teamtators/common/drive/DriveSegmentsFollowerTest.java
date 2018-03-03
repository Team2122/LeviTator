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
        follower.setLookAheadFunction(operand -> 1);

        Pose2d currentPose = new Pose2d(Translation2d.zero(), Rotation.fromDegrees(90));
        while (true) {
            follower.updatePursuitReport(currentPose, 0.0);
            PursuitReport report = follower.getReport();
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
                        new Twist2d(Rotation.fromDegrees(-90), 15.707963267948966)
                ),
                new TwistTestCase(
                        new Pose2d(Translation2d.zero(), Rotation.fromDegrees(124)),
                        new Translation2d(-10.134, 10.1345),
                        new Twist2d(Rotation.fromDegrees(21.997173161334032), 14.42039418592253)
                ),
                new TwistTestCase(
                        new Pose2d(new Translation2d(10, -423), Rotation.fromDegrees(124)),
                        new Translation2d(-10.134, 10.1345),
                        new Twist2d(Rotation.fromDegrees(-62.677111125787484), 456.00121602939987)
                ),
                new TwistTestCase(
                        new Pose2d(new Translation2d(1.01496032180358, 26.94195544013806), Rotation.fromDegrees(75.34119787607021)),
                        new Translation2d(9.119180503066943, 27.65666136386981),
                        new Twist2d(Rotation.fromDegrees(-140.60271757847312), 10.602888466093074)
                )
        );

        for (TwistTestCase testCase : testCases) {
            Twist2d actualTwist = Twist2d.fromTangentArc(testCase.pose, testCase.point);
            Assert.assertTrue(actualTwist.epsilonEquals(testCase.twist),
                    "Expected twist: " + testCase.twist + ", actual twist: " + actualTwist);
        }
    }
}