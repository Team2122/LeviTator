package org.teamtators.common.math;

import java.util.function.DoubleUnaryOperator;

/**
 * @author Alex Mikhalev
 */
public interface DoubleFunction extends DoubleUnaryOperator {
    @Override
    default double applyAsDouble(double operand) {
        return calculate(operand);
    }

    double calculate(double operand);
}
