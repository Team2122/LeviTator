def data = new PathData()

data.with {
    def v = variables
    addPoint 'startLeft', x: -v.centerToLoadingStation + v.halfBumpers.width, y: v.halfBumpers.length
    addPoint 'startRight', points.startLeft.mirrorX()
    addPoint 'startCenter',
            x: /* centerToExchangeZone + halfBumpers.width, */ 0, // TODO: Use correct number
            y: v.halfBumpers.length

    addPoint 'sideCommonDrive', x: 0, y: v.commonDriveDistanceSide

    addPoint 'centerCommon', points.startCenter + point(x: 0, y: v.commonDriveDistanceCenter)

    addPath 'startCenterLeftSwitch', {
        pathPoint translation: points.centerCommon
        pathPoint translation: points.centerCommon + point(x: -20.5, y: 49.5)
        pathPoint translation: points.centerCommon + point(x: -39.2, y: 94.5)
    }
}

data
