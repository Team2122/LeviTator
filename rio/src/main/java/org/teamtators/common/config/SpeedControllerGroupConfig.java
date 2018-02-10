package org.teamtators.common.config;

import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.hw.SpeedControllerGroup;

import java.util.ArrayList;

public class SpeedControllerGroupConfig extends ArrayList<SpeedControllerConfig> {
    public SpeedControllerGroup create() {
        SpeedController[] controllers = new SpeedController[this.size()];
        for (int i = 0; i < controllers.length; i++) {
            controllers[i] = this.get(i).create();
        }
        return new SpeedControllerGroup(controllers);
    }
}
