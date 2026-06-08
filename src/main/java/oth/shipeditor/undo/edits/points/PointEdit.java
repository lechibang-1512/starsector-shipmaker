package oth.shipeditor.undo.edits.points;

import oth.shipeditor.components.viewer.entities.WorldPoint;
import oth.shipeditor.undo.edits.LayerEdit;

public interface PointEdit extends LayerEdit {

    WorldPoint getPoint();

}
