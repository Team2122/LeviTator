package org.teamtators.common.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class UpdatableCollection extends ArrayList<Updatable> implements Updatable {
    private final String name;

    public UpdatableCollection(String name) {
        super();
        this.name = name;
    }

    public UpdatableCollection(String name, Collection<? extends Updatable> c) {
        this(name);
        addAll(c);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean addAll(Collection<? extends Updatable> c) {
        if (c.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("UpdatableCollection cannot contain null: " + c);
        }
        return super.addAll(c);
    }

    @Override
    public boolean add(Updatable updatable) {
        if (updatable == null) {
            throw new IllegalArgumentException("Cannot add null Updatable to UpdatableCollection");
        }
        return super.add(updatable);
    }

    @Override
    public void update(double delta) {
        this.forEach(updatable -> updatable.update(delta));
    }
}
