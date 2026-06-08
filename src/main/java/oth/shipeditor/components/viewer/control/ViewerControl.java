package oth.shipeditor.components.viewer.control;

import de.javagl.viewer.MouseControl;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

public interface ViewerControl extends MouseControl {

    Point getMousePoint();

    Point2D getAdjustedCursor();

    void refreshCursorPosition(MouseEvent event);

    void notifyCursorState(Point cursorLocation);

    void setZoomExact(double level);

    void rotateExact(double desiredDegrees);

}
