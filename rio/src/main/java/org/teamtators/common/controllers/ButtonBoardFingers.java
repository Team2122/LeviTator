package org.teamtators.common.controllers;

import org.teamtators.common.scheduler.TriggerSource;

public class ButtonBoardFingers
        extends ControllerBase<org.teamtators.common.controllers.ButtonBoardFingers.Button, org.teamtators.common.controllers.ButtonBoardFingers.Axis, ControllerBase.Config> {

    public ButtonBoardFingers(String name) {
        super(name);
        setAxisCount(0);
        setButtonCount(12);
    }

    @Override
    public Class<org.teamtators.common.controllers.ButtonBoardFingers.Button> getButtonClass() {
        return org.teamtators.common.controllers.ButtonBoardFingers.Button.class;
    }

    @Override
    public Class<ButtonBoardFingers.Axis> getAxisClass() {
        return Axis.class;
    }

    @Override
    public double getAxisValue(org.teamtators.common.controllers.ButtonBoardFingers.Axis axis) {
        return 0;
    }

    @Override
    public boolean isButtonDown(org.teamtators.common.controllers.ButtonBoardFingers.Button button) {
        switch (button) {
            default:
                return super.isRawButtonDown(button.getButtonID());
        }
    }

    @Override
    public boolean isButtonPressed(org.teamtators.common.controllers.ButtonBoardFingers.Button button) {
        return isRawButtonPressed(button.getButtonID());
    }

    @Override
    public boolean isButtonReleased(org.teamtators.common.controllers.ButtonBoardFingers.Button button) {
        return isRawButtonDown(button.getButtonID());
    }

    @Override
    public TriggerSource getTriggerSource(org.teamtators.common.controllers.ButtonBoardFingers.Button button) {
        return () -> isButtonDown(button);
    }

    /**
     * Enum to reference buttons
     */
    public enum Button {
        LEFT_PINKY(1),
        LEFT_RING(2),
        LEFT_MIDDLE(3),
        LEFT_INDEX(4),
        LEFT_THUMB(5),
        RIGHT_THUMB(6),
        RIGHT_INDEX(7),
        RIGHT_MIDDLE(8),
        RIGHT_RING(9),
        RIGHT_PINKY(10);

        private int buttonID;

        Button(int buttonID) {
            this.buttonID = buttonID;
        }


        /**
         * Gets the button ID
         *
         * @return the button ID
         */
        public int getButtonID() {
            return buttonID;
        }
    }

    public enum Axis {

    }
}