package org.teamtators.levitator.commands;

import com.fasterxml.jackson.databind.JsonNode;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.CommandStore;
import org.teamtators.levitator.TatorRobot;

import java.util.HashMap;
import java.util.Map;

public class ChooserCommand extends Command implements Configurable<ChooserCommand.Config> {

    private String choice;
    private Command command;
    private boolean hasStarted = false;

    private Map<PartnerCombo, String> partnerComboMap = new HashMap<>();

    private SendableChooser<String> choices = new SendableChooser<>();

    private CommandStore commandStore;

    public ChooserCommand(TatorRobot robot) {
        super("ChooserCommand");
        commandStore = robot.getCommandStore();
    }

    @Override
    public void configure(Config config) {
        choices.addDefault(config.defaultAuto, config.defaultAuto);
        if(config.needSwitch != null) {
            partnerComboMap.put(PartnerCombo.NEED_SWITCH_NO_YIELD, config.needSwitch.get("noYield").asText(config.defaultAuto));
            partnerComboMap.put(PartnerCombo.NEED_SWITCH_YIELD, config.needSwitch.get("yield").asText(config.defaultAuto));
        }
        if(config.noSwitch != null) {
            partnerComboMap.put(PartnerCombo.NO_SWITCH_NO_YIELD, config.noSwitch.get("noYield").asText(config.defaultAuto));
            partnerComboMap.put(PartnerCombo.NO_SWITCH_YIELD, config.noSwitch.get("yield").asText(config.defaultAuto));
        }
        partnerComboMap.forEach(((partnerCombo, auto) -> choices.addObject(partnerCombo.getDesc(), auto)));

        SmartDashboard.putData("autochooser", choices);
    }

    public Command getCommand() {
        return commandStore.getCommand(choices.getSelected());
    }

    @Override
    protected void initialize() {
        hasStarted = false;
        if (choice != null) {
            try {
                command = getCommand();
                logger.info("Starting chosen command: {}", command.getName());
                startWithContext(command, this);
            } catch (IllegalArgumentException e) {
                logger.warn("Chosen command not found", e);
            }
        }
    }

    @Override
    protected boolean step() {
        boolean running = command == null || command.isRunning();
        if (running)
            hasStarted = true;
        return command == null || (!running && hasStarted);
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        if (interrupted && command != null) {
            logger.info("Chooser cancelled, cancelling {}", command.getName());
            command.cancel();
        }
    }

    public static class Config {
        public JsonNode needSwitch;
        public JsonNode noSwitch;
        public String defaultAuto;
        //public String[] specialOverrides;
    }

    private enum PartnerCombo {
        NEED_SWITCH_NO_YIELD("Needs Switch, Don't Yield Scale"),
        NEED_SWITCH_YIELD("Needs Switch, Yield Scale"),
        NO_SWITCH_NO_YIELD("Doesn't need Switch, Don't Yield Scale"),
        NO_SWITCH_YIELD("Doesn't need Switch, Yield Scale");
        private String desc;
        private PartnerCombo(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }
    }
}
