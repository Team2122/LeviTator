package org.teamtators.common.control;

/**
 * @author Alex Mikhalev
 */
public class TrapezoidalProfileCalculator {
    // the profile this calculator is based off of
    private TrapezoidalProfile profile;
    // the overall change in position
    private double distance;
    // the velocity at the start of the profile, end of the profile, and during the travel portion
    private double start_v, end_v, travel_v;
    // the total time the profile takes to execute
    private double total_time;
    // the change in velocity, change in time, change in position, and acceleration during the start part of the profile
    private double start_d_v, start_d_t, start_d_s, start_a;
    // the change in velocity, change in time, change in position, and acceleration during the end part of the profile
    private double end_d_v, end_d_t, end_d_s, end_a;
    // the velocity at the beginning of the start part and the end part of the profile.
    // this is usually just start_v and travel_v, respectively, but if the distance is so small that it never gets up to
    // travel_v before the end, end_b will be the maximum velocity that it achieves
    private double start_b, end_b;
    // the change in position and change in time during the travel part of the profile
    private double travel_d_s, travel_d_t;

    // the current time for the calculation of the profile
    private double time;
    // the calculated acceleration, velocity and position based off the profile
    private double acceleration, velocity, position;

    TrapezoidalProfileCalculator(TrapezoidalProfile profile) {
        updateProfile(profile);
    }

    public TrapezoidalProfile getProfile() {
        return profile;
    }

    public void reset() {
        time = 0.0;
        velocity = profile.getStartVelocity();
        position = 0.0;
        acceleration = 0.0;
    }

    public boolean update(double delta) {
        time += delta;
        return calculate();
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public void offsetTime(double delta) {
        this.time += delta;
    }

    public boolean isDone() {
        return time >= total_time;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setPosition(double position) {
        // TODO implement
    }

    public double getPosition() {
        return position;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public boolean calculate() {
        if (time < 0) {
            // before the start of the profile. should not usually be used, but here just in case
            acceleration = 0;
            velocity = start_v;
            position = start_v * time;
        } else if (time < start_d_t) {
            // start part of profile
            acceleration = start_a;
            velocity = start_b + start_a * time;
            position = start_b * time + (start_a / 2) * Math.pow(time, 2);
        } else if (time < start_d_t + travel_d_t) {
            // travel part of the profile
            double travel_time = time - start_d_t;
            acceleration = 0;
            velocity = travel_v;
            position = start_d_s + travel_v * travel_time;
        } else if (time < total_time) {
            // end part of the profile
            double end_time = (time - start_d_t - travel_d_t);
            acceleration = end_a;
            velocity = end_b + end_a * end_time;
            position = start_d_s + travel_d_s + end_b * end_time + (end_a / 2) * Math.pow(end_time, 2);
        } else {
            // after the end part of the profile. should not usually be used, but here just in case
            double after_end_time = (time - total_time);
            acceleration = 0;
            velocity = end_v;
            position = distance + end_v * after_end_time;
        }
        return isDone();
    }

    @Override
    public String toString() {
        return "Calculator{" +
                "time=" + time +
                ", acceleration=" + acceleration +
                ", velocity=" + velocity +
                ", position=" + position +
                ", isDone=" + isDone() + '}';
    }

    public void updateProfile(TrapezoidalProfile profile) {
        this.profile = profile;
        distance = profile.getDistance();
        double max_a = profile.getMaxAcceleration();
        start_v = profile.getStartVelocity();
        end_v = profile.getEndVelocity();
        travel_v = profile.getTravelVelocity();

        start_d_v = travel_v - start_v;
        start_a = Math.copySign(max_a, start_d_v);
        start_d_t = start_d_v / start_a;
        start_d_s = start_d_t * (start_v + travel_v) / 2.0;
        start_b = start_v;

        end_d_v = end_v - travel_v;
        end_a = Math.copySign(max_a, end_d_v);
        end_d_t = end_d_v / end_a;
        end_d_s = end_d_t * (travel_v + end_v) / 2.0;
        end_b = travel_v;

        travel_d_s = distance - start_d_s - end_d_s;

        // if the sign of distance and the sign on travel_d_s do not match, that means that d is so small that it can
        // not get up to travel speed
        if (distance * travel_d_s <= 0) {
            max_a = Math.copySign(max_a, start_d_v);
            end_d_t = (Math.sqrt(2) * Math.sqrt(2 * Math.pow(max_a, 3) * distance
                    + Math.pow(max_a * end_v, 2) + Math.pow(max_a * start_v, 2)) - 2 * max_a * end_v)
                    / (2 * Math.pow(max_a, 2));
            start_d_t = (end_d_t * max_a + end_v - start_v) / max_a;
            end_b = start_d_t * max_a + start_v;
            start_d_v = end_b - start_v;
            start_d_s = start_d_t * (start_v + .5 * start_d_v);
            end_d_v = end_b - end_v;
            end_d_s = end_d_t * (travel_v + .5 * end_d_v);
            travel_d_t = 0.0;
            travel_d_s = 0.0;
        } else {
            travel_d_t = travel_d_s / travel_v;
        }
        total_time = start_d_t + travel_d_t + end_d_t;

        reset();
    }

    public double getTotalTime() {
        return total_time;
    }
}
