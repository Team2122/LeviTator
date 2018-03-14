import groovy.transform.ToString

/**
 * Path variables (all units in inches unless specified otherwise)
 * @author Alex Mikhalev
 */
@ToString(includeNames = true)
class Variables {
    double bumperThickness = 2.5
    Size robot = new Size(length: 33, width: 28)
    Size bumpers = robot + new Size(
            length: 2 * bumperThickness,
            width: 2 * bumperThickness
    )
    Size halfBumpers = bumpers * (1 / 2f)

    double lineThickness = 2
    double centerToLoadingStation = 132
    double centerToExchangeZone = -12
    double baselineToAutoLine = 120
    double baselineToPlatform = 261.47

    Size exchangeZone = new Size(width: 48, length: 36)
    Size powerCube = new Size(length: 13, width: 13, height: 11)

    double commonDriveDistanceSide = 90
    double commonDriveDistanceCenter = 12
}
