package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.util.FMSData;
import org.teamtators.levitator.TatorRobot;

public class Auto extends Subsystem implements Configurable<Auto.Config> {

    private SendableChooser<String> simpleAutoChoices = new SendableChooser<>();
    private FMSData data;
    private Config config;
    private TatorRobot robot;

    //private BooleanChooser

    public Auto(TatorRobot robot) {
        super("Auto");
        simpleAutoChoices.setName("Auto", "Choices");
        this.robot = robot;
    }

    @Override
    public void onFMSData(FMSData data) {
        this.data = data;
    }

    public FMSData getData() {
        return data;
    }

    public Command getSelectedCommand() {
        return robot.getCommandStore().getCommand(simpleAutoChoices.getSelected());
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        for(String choice : config.autoChoices) {
            simpleAutoChoices.addObject(choice, choice);
        }
        simpleAutoChoices.addDefault(config.defaultChoice, config.defaultChoice);
         SmartDashboard.putData(simpleAutoChoices);
    }

    private class BooleanChooser extends SendableChooser<Boolean> {
        public BooleanChooser(String name, String yes, String no) {
            super();
            setName("Auto", name);
            addObject(yes, true);
            addObject(no, false);
        }
    }

    public static class Config {
        public String[] autoChoices;
        public String defaultChoice;
    }
}
