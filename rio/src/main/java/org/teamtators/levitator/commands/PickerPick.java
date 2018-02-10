package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.util.BooleanSampler;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Picker;
import org.teamtators.levitator.subsystems.Subsystems;

public class PickerPick extends Command implements Configurable<PickerPick.Config> {
    private Picker picker;
    private Config config;


    private boolean cubePresent;
    private boolean cubeLeft;
    private boolean cubeRight;

    private BooleanSampler jammed = new BooleanSampler(() ->
            cubePresent && (!cubeLeft || !cubeRight));
    private BooleanSampler finished = new BooleanSampler(() ->
            cubePresent && cubeLeft && cubeRight);

    private boolean unjamming;
    private Timer unjamTimer = new Timer();

    public PickerPick(TatorRobot robot) {
        super("PickerPick");
        picker = robot.getSubsystems().getPicker();
        requires(picker);
    }

    @Override
    protected void initialize() {
        super.initialize();
        picker.setPickerExtended(true);
        unjamming = false;
    }

    @Override
    protected boolean step() {
        this.cubePresent = picker.isCubeDetected();
        this.cubeLeft = picker.isCubeDetectedLeft();
        this.cubeRight = picker.isCubeDetectedRight();

        boolean jammed = this.jammed.get();
        boolean finished = this.finished.get();

        if (jammed && !unjamming) {
            unjamming = true;
            unjamTimer.start();
        }

        if (unjamming && unjamTimer.hasPeriodElapsed(config.unjamPeriod)) {
            unjamming = false;
        }

        if (unjamming) {
            picker.setRollerPowers(config.unjamPowers);
        } else {
            picker.setRollerPowers(config.pickPowers);
        }

        return finished;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        picker.stopRollers();
        picker.setPickerExtended(false);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        this.jammed.setPeriod(config.jamDetectPeriod);
        this.finished.setPeriod(config.finishDetectPeriod);
    }

    public static class Config {
        public Picker.RollerPowers pickPowers;
        public Picker.RollerPowers unjamPowers;
        public double jamDetectPeriod;
        public double finishDetectPeriod;
        public double unjamPeriod;
    }
}
