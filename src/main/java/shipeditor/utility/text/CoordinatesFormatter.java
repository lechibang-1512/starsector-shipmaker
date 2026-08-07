package shipeditor.utility.text;

import java.awt.geom.Point2D;
import java.text.DecimalFormat;

public final class CoordinatesFormatter {

    private static final ThreadLocal<DecimalFormat> DISPLAY_FORMAT =
            ThreadLocal.withInitial(() -> new DecimalFormat("0.000", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US)));

    private CoordinatesFormatter() {
    }

    public static double round(double value) {
        return shipeditor.utility.Utility.round(value, 3);
    }

    public static Point2D roundPoint(Point2D point) {
        if (point == null) return null;
        double roundedX = round(point.getX());
        double roundedY = round(point.getY());
        return new Point2D.Double(roundedX, roundedY);
    }

    public static String formatDisplay(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) return "NaN";
        DecimalFormat format = DISPLAY_FORMAT.get();
        return format.format(x) + ", " + format.format(y);
    }

    public static String formatDisplay(Point2D point) {
        if (point == null) return "null";
        return formatDisplay(point.getX(), point.getY());
    }

}
