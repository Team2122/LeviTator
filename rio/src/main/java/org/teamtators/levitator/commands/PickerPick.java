package org.teamtators.levitator.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.control.BooleanSampler;
import org.teamtators.levitator.TatorRobot;
import org.teamtators.levitator.subsystems.Picker;

public class PickerPick extends Command implements Configurable<PickerPick.Config> {
    private Picker picker;
    private Config config;


    private boolean cubePresent;
    private boolean cubeLeft;
    private boolean cubeRight;

    private BooleanSampler jammed = new BooleanSampler(() ->
            cubePresent && (!cubeLeft || !cubeRight));
    private BooleanSampler finished = new BooleanSampler(() ->
            (cubePresent && cubeLeft && cubeRight) || (!cubePresent && cubeLeft));

    private boolean unjamming;
    private boolean unjamRight;
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
        unjamRight = false;
        picker.unlockArms();
        jammed.reset();
        finished.reset();
    }

    @Override
    public boolean step() {
        this.cubePresent = picker.isCubeDetected();
        this.cubeLeft = picker.isCubeDetectedLeft();
        this.cubeRight = picker.isCubeDetectedRight();

        boolean jammed = this.jammed.get();
        boolean finished = this.finished.get() && !config.force;

        if (jammed && !unjamming && (!unjamTimer.isRunning() || unjamTimer.hasPeriodElapsed(config.afterUnjamWait))) {
            logger.trace("Jam detected, unjamming");
            unjamming = true;
            unjamTimer.start();
        }

        if (unjamming && unjamTimer.hasPeriodElapsed(config.unjamPeriod)) {
            unjamming = false;
            unjamRight = !unjamRight;
        }

        if (unjamming && !config.force) {
            picker.setRollerPowers(unjamRight ? config.unjamPowers.right : config.unjamPowers.left,
                    unjamRight ? config.unjamPowers.left : config.unjamPowers.right);
        } else {
            picker.setRollerPowers(config.pickPowers);
        }

        return finished;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        if (interrupted && !config.force) {
            picker.setRollerPower(0.0);
        } else {
            picker.setRollerPowers(config.holdPowers);
            picker.lockArms();
        }
        picker.extendDefault();
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
        public Picker.RollerPowers holdPowers;
        public double jamDetectPeriod;
        public double finishDetectPeriod;
        public double unjamPeriod;
        public double afterUnjamWait;
        public boolean force = false;
    }
}
