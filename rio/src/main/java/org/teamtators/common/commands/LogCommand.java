package org.teamtators.common.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class LogCommand extends Command implements Configurable<LogCommand.Config> {
    private Config config;
    public LogCommand() {
        super("LogCommand");
    }

    @Override
    public void initialize() {
        switch (config.level) {
            case INFO:
                logger.info(config.message);
                break;
            case DEBUG:
                logger.debug(config.message);
                break;
            case TRACE:
                logger.trace(config.message);
                break;
            case WARN:
                logger.warn(config.message);
                break;
            case ERROR:
                logger.error(config.message);
                break;
        }
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    @Override
    public boolean step() {
        return true;
    }

    @Override
    protected void finish(boolean interrupted) {
    }

    public static class Config {
        public LogLevel level = LogLevel.INFO;
        public String message;
    }

    public enum LogLevel {
        INFO,
        DEBUG,
        TRACE,
        WARN,
        ERROR
    }
}
