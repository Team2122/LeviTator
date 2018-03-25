package org.teamtators.common.tester;

import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;

import java.util.ArrayList;
import java.util.List;

/**
 * An automated test
 */
public class AutomatedTest extends Command {
    protected LogitechF310.Button lastButton;
    private List<AutomatedTestMessage> messageList;
    private boolean isHybrid;
    private boolean skip;

    public AutomatedTest(String name, boolean isHybrid) {
        super(name);
        this.isHybrid = isHybrid;
        messageList = new ArrayList<>();
        validIn(RobotState.TEST);
    }

    public AutomatedTest(String name) {
        this(name, false);
    }

    public void setLastButton(LogitechF310.Button button) {
        this.lastButton = button;
    }

    public void skip() {
        skip = true;
    }

    public void resetSkip() {
        skip = false;
    }

    protected boolean skipped() {
        return skip;
    }

    @Override
    public boolean step() {
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        logger.info("Automated test {} {}.", getName(), (interrupted ? "interrupted" : "finished"));
    }

    protected void sendMessage(String message, AutomatedTestMessage.Level level) {
        switch (level) {
            case INFO:
                logger.info(message);
                break;
            case WARN:
                logger.warn(message);
                messageList.add(new AutomatedTestMessage(message, level, this));
                break;
            case ERROR:
                logger.error(message);
                messageList.add(new AutomatedTestMessage(message, level, this));
        }
    }

    public List<AutomatedTestMessage> getMessages() {
        return messageList;
    }

    public void clearMessages() {
        messageList.clear();
    }

    public boolean isHybrid() {
        return isHybrid;
    }
}
