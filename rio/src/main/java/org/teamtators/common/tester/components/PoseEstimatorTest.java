package org.teamtators.common.tester.components;

import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.drive.PoseEstimator;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.tester.ManualTest;

import java.util.Collection;
import java.util.Iterator;

public class PoseEstimatorTest extends ManualTest {
    private final PoseEstimator poseEstimator;

    public PoseEstimatorTest(PoseEstimator poseEstimator) {
        super("PoseEstimator");
        this.poseEstimator = poseEstimator;
    }

    @Override
    public void start() {
        logger.info("Press A to get current robot pose; Press B to reset robot pose");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case A:
                logger.info("Robot pose: " + poseEstimator.getPose());
                break;
            case B:
                logger.info("Reset robot pose");
                poseEstimator.setPose(Pose2d.zero());
                break;
        }
    }
}
