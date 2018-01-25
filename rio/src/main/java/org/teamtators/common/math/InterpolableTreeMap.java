package org.teamtators.common.math;

import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Alex Mikhalev
 */
public class InterpolableTreeMap<K extends InverseInterpolable<K> & Comparable<K>,
        V extends Interpolable<V>> extends TreeMap<K, V> {
    public InterpolableTreeMap() {
    }

    public InterpolableTreeMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    public InterpolableTreeMap(SortedMap<K, ? extends V> m) {
        super(m);
    }

    public Optional<V> getInterpolated(K key) {
        V val = get(key);
        if (val != null)
            return Optional.of(val);
        Map.Entry<K, V> ceil = ceilingEntry(key);
        Map.Entry<K, V> floor = floorEntry(key);
        if (ceil == null && floor == null) {
            return Optional.empty();
        }
        if (ceil == null) {
            return Optional.of(floor.getValue());
        }
        if (floor == null) {
            return Optional.of(ceil.getValue());
        }
        double value = floor.getKey().inverseInterpolate(ceil.getKey(), key);
        return Optional.of(floor.getValue().interpolate(ceil.getValue(), value));
    }
}
