package org.teamtators.common.tester;

/**
 * Object holding a message brought up during an automated test
 */
public class AutomatedTestMessage {
    private String message;
    private Level level;
    private AutomatedTest sender;

    public AutomatedTestMessage(String message, Level level, AutomatedTest sender) {
        this.message = message;
        this.level = level;
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public AutomatedTest getSender() {
        return sender;
    }

    public void setSender(AutomatedTest sender) {
        this.sender = sender;
    }

    public enum Level {
        INFO,
        WARN,
        ERROR
    }
}
