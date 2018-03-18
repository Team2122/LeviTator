package org.teamtators.shuffleboardplugin;

import com.google.common.collect.ImmutableList;
import edu.wpi.first.shuffleboard.api.plugin.Plugin;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;

public class TatorShuffleboardPlugin extends Plugin {
    @Override
    public List<Property<?>> getProperties() {
        return ImmutableList.of(
                new SimpleStringProperty(null, "group", "org.teamtators.shuffleboardplugin"),
                new SimpleStringProperty(null, "name", "Tator Shuffleboard Plugin")
        );
    }
}
