package org.teamtators.levitator.subsystems;

import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.util.FMSData;
import org.teamtators.common.util.FieldSide;
import org.teamtators.levitator.TatorRobot;

public class Auto extends Subsystem implements Configurable<Auto.Config> {

    private SendableChooser<String> autoChoices = new SendableChooser<>();
    private FMSData data;
    private Config config;
    private TatorRobot robot;

    private SendableChooser<String> startPosition = new SendableChooser<>();

    public Auto(TatorRobot robot) {
        super("Auto");
        autoChoices.setName("Auto", "Choices");
        startPosition.setName("Auto", "StartPos");
        startPosition.addDefault("Left", "Left");
        startPosition.addObject("Center", "Center");
        startPosition.addObject("Right", "Right");
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
        return robot.getCommandStore().getCommand(autoChoices.getSelected());
    }

    @Override
    public void configure(Config config) {
        super.configure();
        this.config = config;
        for (String choice : config.autoChoices) {
            autoChoices.addObject(choice, choice);
        }
        autoChoices.addDefault(config.defaultChoice, config.defaultChoice);
        SmartDashboard.putData(autoChoices);
    }

    public FieldSide getFieldConfiguration(int object) {
        return data.elementSides[object];
    }

    public String getStartingPosition() {
        return startPosition.getSelected();
    }

    @Override
    public void deconfigure() {
        super.deconfigure();
        simpleAutoChoices.free();
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
