package org.teamtators.common.datalogging;

import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.control.Updatable;

import java.util.ArrayList;

public class DashboardUpdater extends ArrayList<DashboardUpdatable> implements Updatable {
    private Dashboard smartDashboard;
    private Dashboard noopDashboard;

    private Dashboard current;

    private Dashboard.Type type;
    private TatorRobotBase robot;

    public DashboardUpdater(TatorRobotBase robot, Dashboard.Type type) {
        this.type = type;
        this.robot = robot;
    }

    public void setDashboard(Dashboard.Type type) {
        switch (type) {
            case SMART_DASHBOARD:
                current = smartDashboard;
                break;
            case NONE:
                current = noopDashboard;
                break;
        }
    }

    public void setUpDashboards() {
        smartDashboard = new SmartDashboardAdapter();
        noopDashboard = new NoopDashboardAdapter();
        setDashboard(type);
    }

    @Override
    public String getName() {
        return "dashboardUpdater";
    }

    @Override
    public void update(double delta) {
        if (current == null) {
            return;
        }
        this.forEach(updatable -> updatable.updateDashboard(current));
    }
}
