package org.teamtators.common.util;

/**
 * Utilities for DriveTank input
 */
public class JoystickModifiers {
    public double offset = 0.0;
    public double deadzone = 0.0;
    public double exponent = 1.0;
    public double scale = 1.0;

    public JoystickModifiers() {
    }

    public JoystickModifiers(double offset, double deadzone, double exponent, double scale) {
        this.offset = offset;
        this.deadzone = deadzone;
        this.exponent = exponent;
        this.scale = scale;
    }

    public double apply(double input) {
        input = input + offset;
        input = applyDeadzone(input, deadzone);
        input = applyExponent(input, exponent);
        input = input * scale;
        return input;
    }

    public static double applyDriveModifiers(double input, double deadzone, double exponent) {
        input = applyDeadzone(input, deadzone);
        return applyExponent(input, exponent);
    }

    public static double applyExponent(double input, double exponent) {
        double absolute = Math.abs(input);
        double sign = Math.signum(input);
        return sign * Math.pow(absolute, exponent);
    }

    public static double applyDeadzone(double input, double deadzone) {
        if (Math.abs(input) <= deadzone)
            return 0;
        return input * (1 - deadzone) + (deadzone * Math.signum(input));
    }
}
