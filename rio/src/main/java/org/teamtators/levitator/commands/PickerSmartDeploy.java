package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Lift;
import org.teamtators.levitator.subsystems.OperatorInterface;
import org.teamtators.levitator.subsystems.Picker;

import java.util.Map;

public class PickerSmartDeploy extends Command implements Configurable<PickerSmartDeploy.Config> {
    private final Picker picker;
    private final Lift lift;
    private final OperatorInterface oi;
    private final Timer timer = new Timer();
    private Config config;
    private boolean isQuick;
    private DeployPower powerName;
    private Config.PowerConfig powerConfig;

    public PickerSmartDeploy(TatorRobot robot) {
        super("PickerSmartDeploy");
        this.picker = robot.getSubsystems().getPicker();
        this.lift = robot.getSubsystems().getLift();
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
        powerName = null;
        isQuick = !picker.isExtended();
        updatePower();
    }

    private void updatePower() {
        if (lift.getCurrentHeight() < config.maxSwitchHeight) {
            powerConfig = config.switchPower;
            logger.info("Lift below height, using switch deploy");
        }
        int numPowers = DeployPower.values().length;
        double sliderValue = oi.getReleasePower();
        int powerIdx = (int) (((sliderValue + 1.0) / 2.0) * numPowers);
        powerIdx = Math.max(0, Math.min(numPowers - 1, powerIdx));
        DeployPower powerName = DeployPower.values()[powerIdx];
        if (this.powerName != powerName) {
            this.powerName = powerName;
            powerConfig = config.powers.get(powerName);
            if (isQuick) {
                logger.info("Picker is retracted, doing quick deploy with kickPower {}", powerName);
            } else {
                logger.info("Picker is extended, doing long deploy with kickPower {}", powerName);
            }
        }
    }

    @Override
    protected boolean step() {
        updatePower();
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
        if (isQuick) {
            if (!Double.isNaN(powerConfig.timeBeforeRetract)) {
                picker.setPickerExtended(false);
            }
        } else {
            picker.extendDefault();
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
        public Map<DeployPower, PowerConfig> powers;
        public PowerConfig switchPower;
        public double maxSwitchHeight;
    }
}
