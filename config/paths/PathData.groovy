import groovy.transform.ToString
import org.teamtators.common.drive.DrivePath
import org.teamtators.common.drive.PathPoint
import org.teamtators.common.math.Translation2d

class GroovyTranslation2d extends Translation2d {
    GroovyTranslation2d(double x, double y) {
        super(x, y)
    }

    GroovyTranslation2d(final Map data) {
        this(data.x ?: 0, data.y ?: 0)
    }
}

class GroovyDrivePath extends DrivePath {
    double radius = 30
    double speed = 140
    double arcSpeed = 60
    boolean reverse = false

    GroovyDrivePath(String name) {
        super(name)
    }

    GroovyDrivePath(GroovyDrivePath other) {
        super(other.name)
        this.points = other.points
        this.radius = other.radius
        this.speed = other.speed
        this.arcSpeed = other.arcSpeed
        this.reverse = other.reverse
    }

    PathPoint pathPoint(final Map data) {
        def point = new PathPoint()
        if (data.translation) point.setTranslation(data.translation as Translation2d)
        point.x = data.x ?: point.x
        point.y = data.y ?: point.y
        point.radius = data.radius ?: radius
        point.speed = data.speed ?: speed
        point.arcSpeed = data.arcSpeed ?: arcSpeed
        point
    }
}

/**
 * @author Alex Mikhalev
 */
@ToString(includeNames=true)
class PathData {
    Variables variables = new Variables()
    Map<String, Translation2d> points = new LinkedHashMap<>()
    Map<String, DrivePath> paths = new LinkedHashMap<>()

    Translation2d addPoint(String name, Translation2d point) {
        points.put name, point
        point
    }

    GroovyTranslation2d addPoint(final Map data, String name) {
        def p = new GroovyTranslation2d(data)
        addPoint name, p
    }

    GroovyTranslation2d addPoint(String name, @DelegatesTo(GroovyTranslation2d) Closure closure) {
        def p = new GroovyTranslation2d()
        closure.delegate = p
        closure()
        addPoint name, p
        point
    }

    DrivePath addPath(String name, DrivePath path) {
        paths.put name, path
        path
    }

    GroovyDrivePath addPath(String name) {
        def path = new GroovyDrivePath(name)
        paths.put name, path
        path
    }

    GroovyDrivePath addPath(String name, @DelegatesTo(GroovyDrivePath) Closure closure) {
        def path = addPath(name)
        closure.delegate = path
        closure()
        path
    }

    static GroovyTranslation2d point(final Map data) {
        new GroovyTranslation2d(data)
    }
}