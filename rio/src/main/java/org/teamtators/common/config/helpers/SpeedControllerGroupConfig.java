package org.teamtators.common.config.helpers;

import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.hw.SpeedControllerGroup;

import java.util.ArrayList;

public class SpeedControllerGroupConfig extends ArrayList<SpeedControllerConfig>
        implements ConfigHelper<SpeedControllerGroup> {
    public SpeedControllerGroup create() {
        SpeedController[] controllers = new SpeedController[this.size()];
        for (int i = 0; i < controllers.length; i++) {
            controllers[i] = this.get(i).create();
        }
        return new SpeedControllerGroup(controllers);
    }

    public double getTotalCurrent(PowerDistributionPanel pdp) {
        double totalCurrent = 0;
        for (SpeedControllerConfig speedController : this) {
            totalCurrent += speedController.getTotalCurrent(pdp);
        }
        return totalCurrent;
    }
}
