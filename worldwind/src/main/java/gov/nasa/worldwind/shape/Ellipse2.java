/*
 * Copyright (c) 2018 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.shape;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec3;
import gov.nasa.worldwind.render.RenderContext;
import gov.nasa.worldwind.util.WWMath;

public class Ellipse2 extends Ellipse {

    protected double pixelsPerInterval = 50;

    protected int detailLevels = 5;

    public Ellipse2() {
    }

    public Ellipse2(ShapeAttributes attributes) {
        super(attributes);
    }

    public Ellipse2(Position center, double majorRadius, double minorRadius) {
        super(center, majorRadius, minorRadius);
    }

    public Ellipse2(Position center, double majorRadius, double minorRadius, ShapeAttributes attributes) {
        super(center, majorRadius, minorRadius, attributes);
    }

    @Override
    protected int calculateIntervals(RenderContext rc) {
        double distanceToCamera = this.distanceToCamera(rc);
        double pixelSizeAtDistance = rc.pixelSizeAtDistance(distanceToCamera);
        double circumference = this.calculateCircumference();

        int calculatedIntervals = MIN_INTERVALS;
        double deltaIntervals = (this.maximumIntervals - MIN_INTERVALS) / this.detailLevels;
        for (int i = 0; i < this.detailLevels; i++) {
            double circumferenceDensity = circumference / calculatedIntervals;
            if (circumferenceDensity < pixelSizeAtDistance * this.pixelsPerInterval) {
                break;
            }
            calculatedIntervals += deltaIntervals;
        }

        // Intervals must be divisible by two, this check is repeated in the assembleGeometry method
        if (calculatedIntervals % 2 != 0) {
            calculatedIntervals--;
        }

        if (calculatedIntervals > this.maximumIntervals) {
            calculatedIntervals = this.maximumIntervals;
        }

        if (calculatedIntervals < MIN_INTERVALS) {
            calculatedIntervals = MIN_INTERVALS;
        }

        return calculatedIntervals;
    }

    protected double distanceToCamera(RenderContext rc) {
        if (this.boundingSector.isEmpty()) {
            Vec3 point = rc.geographicToCartesian(this.center.latitude, this.center.longitude, this.center.altitude, this.altitudeMode, POINT);
            return point.distanceTo(rc.cameraPoint);
        }

        // borrowed from the Tile class
        // determine the nearest latitude
        double nearestLat = WWMath.clamp(rc.camera.latitude, this.boundingSector.minLatitude(), this.boundingSector.maxLatitude());
        // determine the nearest longitude and account for the antimeridian discontinuity
        double nearestLon;
        double lonDifference = rc.camera.longitude - this.boundingSector.centroidLongitude();
        if (lonDifference < -180.0) {
            nearestLon = this.boundingSector.maxLongitude();
        } else if (lonDifference > 180.0) {
            nearestLon = this.boundingSector.minLongitude();
        } else {
            nearestLon = WWMath.clamp(rc.camera.longitude, this.boundingSector.minLongitude(), this.boundingSector.maxLongitude());
        }

        rc.geographicToCartesian(nearestLat, nearestLon, this.center.altitude, WorldWind.ABSOLUTE, POINT);

        return rc.cameraPoint.distanceTo(POINT);
    }

    private double calculateCircumference() {
        double a = this.majorRadius;
        double b = this.minorRadius;
        return Math.PI * (3 * (a + b) - Math.sqrt((3 * a + b) * (a + 3 * b)));
    }
}
