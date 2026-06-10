package shipeditor.components.viewer.entities;

import shipeditor.components.instrument.EditorInstrument;
import shipeditor.components.viewer.layers.LayerPainter;

import java.awt.geom.Point2D;

public interface WorldPoint {

    /**
     * @return contained position instance.
     */
    Point2D getPosition();

    /**
     * @param x position on X axis.
     * @param y position on Y axis.
     */
    void setPosition(double x, double y);

    void setPosition(Point2D input);

    /**
     * @return whether this point is selected, that is, active for some user input such as moving the point.
     */
    boolean isPointSelected();

    /**
     * @return layer associated with this point; can be null.
     */
    LayerPainter getParent();

    void setPointSelected(boolean selected);

    Point2D getCoordinatesForDisplay();

    void setPaintSizeMultiplier(double mult);

    EditorInstrument getAssociatedMode();

}
