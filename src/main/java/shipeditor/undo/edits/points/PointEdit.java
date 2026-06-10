package shipeditor.undo.edits.points;

import shipeditor.components.viewer.entities.WorldPoint;
import shipeditor.undo.edits.LayerEdit;

public interface PointEdit extends LayerEdit {

    WorldPoint getPoint();

}
