package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.OperatorInterface;
import org.teamtators.levitator.subsystems.Picker;

import java.util.Map;

public class PickerSmartDeploy extends Command implements Configurable<PickerSmartDeploy.Config> {
    private final Picker picker;
    private final OperatorInterface oi;
    private final Timer timer = new Timer();
    private Config config;
    private boolean isQuick;
    private DeployPower powerName;
    private Config.PowerConfig powerConfig;

    public PickerSmartDeploy(TatorRobot robot) {
        super("PickerSmartDeploy");
        this.picker = robot.getSubsystems().getPicker();
        this.oi = robot.getSubsystems().getOI();
        requires(picker);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    @Override
    public void initialize() {
        timer.start();
        int numPowers = DeployPower.values().length;
        double sliderValue = oi.getReleasePower();
        int powerIdx = (int) (((sliderValue + 1.0) / 2.0) * (1.0 / numPowers));
        powerName = DeployPower.values()[powerIdx];
        powerConfig = config.deployPowers.get(powerName);
        if (picker.isExtended()) {
            logger.info("Picker is extended, doing long deploy with kickPower {}", powerName);
        } else {
            logger.info("Picker is retracted, doing quick deploy with kickPower {}", powerName);
        }
    }

    @Override
    protected boolean step() {
        if (!isQuick) {
            picker.setRollerPower(-powerConfig.kickPower);
            return false;
        }
        boolean canStart = timer.hasPeriodElapsed(powerConfig.timeBeforeKick);
        boolean startRetracting = timer.hasPeriodElapsed(powerConfig.timeBeforeRetract);
        boolean endKick = timer.hasPeriodElapsed(powerConfig.timeToKick);
        if (canStart && !endKick) {
            picker.setRollerPower(-powerConfig.kickPower);
        } else {
            picker.setRollerPower(0.0);
        }
        if (startRetracting) {
            if (!Double.isNaN(powerConfig.timeBeforeRetract)) {
                picker.setPickerExtended(false);
            }
        } else {
            picker.setPickerExtended(true);
        }
        return endKick;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        picker.setRollerPower(0.0);
        if (!Double.isNaN(powerConfig.timeBeforeRetract)) {
            picker.setPickerExtended(false);
        }
    }

    public enum DeployPower {
        SOFT,
        MEDIUM,
        HARD
    }

    public static class Config {
        public static class PowerConfig {
            public double kickPower;
            public double timeBeforeKick;
            public double timeToKick;
            public double timeBeforeRetract;
        }
        public Map<DeployPower, PowerConfig> deployPowers;
    }
}
