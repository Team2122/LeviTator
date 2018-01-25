package org.teamtators.common.datalogging;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class SmartDashboardAdapter implements Dashboard {
    @Override
    public void putBoolean(String name, boolean val) {
        SmartDashboard.putBoolean(name, val);
    }

    @Override
    public void putNumber(String name, double val) {
        SmartDashboard.putNumber(name, val);
    }

    @Override
    public void putString(String name, String val) {
        SmartDashboard.putString(name, val);
    }
}
