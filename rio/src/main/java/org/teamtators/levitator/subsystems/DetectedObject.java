package org.teamtators.levitator.subsystems;

public class DetectedObject {
    public Double x;
    public Double y;
    public Double area;
    public Double width;
    public Double height;

    public DetectedObject(Double x, Double y, Double area, Double width, Double height) {
        this.x = x;
        this.y = y;
        this.area = area;
        this.width = width;
        this.height = height;
    }

    public DetectedObject() {}
}
