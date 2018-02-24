package org.teamtators.common.util;

import com.google.common.base.Objects;
import edu.wpi.first.wpilibj.DriverStation;

/**
 * @author Alex Mikhalev
 */
public class FMSData {
    public String gameSpecificMessage;
    public FieldSide[] elementSides;
    public String eventName;
    public int matchNumber;
    public int replayNumber;
    public DriverStation.MatchType matchType;
    public DriverStation.Alliance alliance;
    public int station;

    public static FMSData fromDriverStation(DriverStation ds) {
        FMSData data = new FMSData();
        data.gameSpecificMessage = ds.getGameSpecificMessage();
        data.eventName = ds.getEventName();
        data.matchType = ds.getMatchType();
        data.matchNumber = ds.getMatchNumber();
        data.replayNumber = ds.getReplayNumber();
        data.alliance = ds.getAlliance();
        data.station = ds.getLocation();
        data.elementSides = FieldSide.fromString(data.gameSpecificMessage);
        return data;
    }

    public static FMSData fromDriverStation() {
        return fromDriverStation(DriverStation.getInstance());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FMSData fmsData = (FMSData) o;
        return matchNumber == fmsData.matchNumber &&
                replayNumber == fmsData.replayNumber &&
                station == fmsData.station &&
                Objects.equal(gameSpecificMessage, fmsData.gameSpecificMessage) &&
                Objects.equal(eventName, fmsData.eventName) &&
                matchType == fmsData.matchType &&
                alliance == fmsData.alliance;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(gameSpecificMessage, eventName, matchNumber, replayNumber, matchType, alliance, station);
    }

    public String toString() {
        return String.format("Event \"%s\" %s match %d replay %d. In %s station %d. GSM: \"%s\"",
                eventName, matchType, matchNumber, replayNumber, alliance, station, gameSpecificMessage);
    }
}
