package oth.shipeditor.utility.text;

import java.awt.geom.Point2D;
import java.text.DecimalFormat;

public final class CoordinatesFormatter {

    private static final DecimalFormat DISPLAY_FORMAT = new DecimalFormat("0.000");

    private CoordinatesFormatter() {
    }

    public static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static Point2D roundPoint(Point2D point) {
        double roundedX = round(point.getX());
        double roundedY = round(point.getY());
        return new Point2D.Double(roundedX, roundedY);
    }

    public static String formatDisplay(double x, double y) {
        return DISPLAY_FORMAT.format(x) + ", " + DISPLAY_FORMAT.format(y);
    }

    public static String formatDisplay(Point2D point) {
        return formatDisplay(point.getX(), point.getY());
    }

}
