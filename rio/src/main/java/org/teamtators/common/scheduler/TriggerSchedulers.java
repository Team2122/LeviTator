package org.teamtators.common.scheduler;

import org.teamtators.common.control.Timer;

public class TriggerSchedulers {
    public static TriggerScheduler onActive(Runnable runnable) {
        return new LastStateScheduler(runnable) {
            @Override
            protected boolean shouldRun(boolean active, boolean lastActive) {
                return active && !lastActive;
            }
        };
    }

    public static TriggerScheduler onInactive(Runnable runnable) {
        return new LastStateScheduler(runnable) {
            @Override
            protected boolean shouldRun(boolean active, boolean lastActive) {
                return !active && lastActive;
            }
        };
    }

    public static TriggerScheduler whileActive(Runnable runnable) {
        return new RunnableScheduler(runnable) {
            @Override
            protected boolean shouldRun(boolean active) {
                return active;
            }
        };
    }

    public static TriggerScheduler whileInactive(Runnable runnable) {
        return new RunnableScheduler(runnable) {
            @Override
            protected boolean shouldRun(boolean active) {
                return !active;
            }
        };
    }

    public static TriggerScheduler activeFor(Runnable runnable, int duration) {
        return new RunnableScheduler(runnable) {
            private Timer timer = new Timer();
            private boolean hasRun = false;
            @Override
            protected boolean shouldRun(boolean active) {
                if(!active) {
                    timer.restart();
                    hasRun = false;
                    return false;
                }
                if (timer.hasPeriodElapsed(duration) && !hasRun) {
                    hasRun = true;
                    return true;
                }
                else return false;
            }
        };
    }

    public static abstract class RunnableScheduler implements TriggerScheduler {
        private Runnable runnable;

        public RunnableScheduler(Runnable runnable) {
            this.runnable = runnable;
        }

        protected Runnable getRunnable() {
            return runnable;
        }

        protected abstract boolean shouldRun(boolean active);

        @Override
        public void processTrigger(boolean active) {
            if (shouldRun(active))
                runnable.run();
        }
    }

    public static abstract class LastStateScheduler extends RunnableScheduler {
        private boolean lastActive = false;

        public LastStateScheduler(Runnable runnable) {
            super(runnable);
        }

        protected abstract boolean shouldRun(boolean active, boolean lastActive);

        @Override
        public boolean shouldRun(boolean active) {
            boolean result = shouldRun(active, lastActive);
            lastActive = active;
            return result;
        }
    }
}
