package org.teamtators.common.util;

public enum FieldSide {
    LEFT,
    RIGHT,
    UNKNOWN;

    public static FieldSide[] fromString(String gameMessage) {
        FieldSide[] configuration = new FieldSide[3];
        String[] splitMessage = gameMessage.split("");
        for (int i = 0; i < 3; i++) {
            if (splitMessage.length != 3)
                configuration[i] = UNKNOWN;
            else
                configuration[i] = fromSingleCharacter(splitMessage[i]);
        }
        return configuration;
    }

    private static FieldSide fromSingleCharacter(String character) {
        return character.equals("L") ? LEFT : character.equals("R") ? RIGHT : UNKNOWN;
    }
}
