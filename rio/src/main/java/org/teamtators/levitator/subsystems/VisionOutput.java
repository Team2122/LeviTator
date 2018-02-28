package org.teamtators.levitator.subsystems;

public class VisionOutput {
    public Double x;
    public Double y;
    public Double area;
    public Double width;
    public Double height;

    public VisionOutput(Double x, Double y, Double area, Double width, Double height) {
        this.x = x;
        this.y = y;
        this.area = area;
        this.width = width;
        this.height = height;
    }

    public VisionOutput() {}
}
