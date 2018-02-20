package org.teamtators.common.math;

/**
 * @author Alex Mikhalev
 */
public class Polynomial3 implements DoubleFunction {
    private double a;
    private double b;
    private double c;

    public Polynomial3() {
        this(0.0, 0.0, 0.0);
    }

    public Polynomial3(double a, double b, double c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
    }

    public double calculate(double x) {
        return a * x * x + b * x + c;
    }
}
