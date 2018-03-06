package org.teamtators.common.control;


import static com.google.common.base.Preconditions.checkNotNull;

public class InputDerivative implements Updatable, ControllerInput {
    private ControllerInput inputProvider;
    private double lastInput;
    private double lastRate;
    private String name;

    public InputDerivative(String name) {
        this.name = name;
        reset();
    }

    public InputDerivative(String name, ControllerInput inputProvider) {
        this(name);
        setInputProvider(inputProvider);
    }

    public void reset() {
        lastInput = Double.NaN;
        lastRate = 0.0;
    }

    @Override
    public synchronized void update(double delta) {
        double input = inputProvider.getControllerInput();
        lastRate = Double.isNaN(lastInput) ? 0.0 : (input - lastInput) / delta;
        lastInput = input;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public synchronized double getControllerInput() {
        return lastRate;
    }

    public ControllerInput getInputProvider() {
        return inputProvider;
    }

    public void setInputProvider(ControllerInput inputProvider) {
        this.inputProvider = checkNotNull(inputProvider);
    }
}
