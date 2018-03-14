import groovy.transform.ToString

/**
 * @author Alex Mikhalev
 */
@ToString(includeNames = true)
class Size {
    double length = 0
    double width = 0
    double height = 0

    Size plus(Size other) {
        new Size(length: this.length + other.length,
                width: this.width + other.width,
                height: this.height + other.height);
    }

    Size multiply(Size other) {
        new Size(length: this.length * other.length,
                width: this.width * other.width,
                height: this.height * other.height);
    }

    Size multiply(double scaleFactor) {
        new Size(length: this.length * scaleFactor,
                width: this.width * scaleFactor,
                height: this.height * scaleFactor);
    }
}
