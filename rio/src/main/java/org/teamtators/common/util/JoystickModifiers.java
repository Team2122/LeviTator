package org.teamtators.common.util;

/**
 * Utilities for DriveTank input
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class JoystickModifiers {
    public double offset = 0.0;
    public double deadZone = 0.0;
    public double exponent = 1.0;
    public double scale = 1.0;

    public JoystickModifiers() {
    }

    public JoystickModifiers(double offset, double deadZone, double exponent, double scale) {
        this.offset = offset;
        this.deadZone = deadZone;
        this.exponent = exponent;
        this.scale = scale;
    }

    public static double applyDriveModifiers(double input, double deadZone, double exponent) {
        input = applyDeadZone(input, deadZone);
        return applyExponent(input, exponent);
    }

    public static double applyExponent(double input, double exponent) {
        double absolute = Math.abs(input);
        double sign = Math.signum(input);
        return sign * Math.pow(absolute, exponent);
    }

    public static double applyDeadZone(double input, double deadZone) {
        if (Math.abs(input) <= deadZone)
            return 0;
        return input * (1 - deadZone) + (deadZone * Math.signum(input));
    }

    public double apply(double input) {
        input = input + offset;
        input = applyDeadZone(input, deadZone);
        input = applyExponent(input, exponent);
        input = input * scale;
        return input;
    }
}
