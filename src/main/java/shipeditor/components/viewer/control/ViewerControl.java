package shipeditor.components.viewer.control;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

public interface ViewerControl extends MouseListener, MouseMotionListener, MouseWheelListener {

    Point getMousePoint();

    Point2D getAdjustedCursor();

    void refreshCursorPosition(MouseEvent event);

    void notifyCursorState(Point cursorLocation);

    void setZoomExact(double level);

    void rotateExact(double desiredDegrees);

}
