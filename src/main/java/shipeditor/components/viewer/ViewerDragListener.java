package shipeditor.components.viewer;

import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;

public class ViewerDragListener extends DragSourceAdapter {

    @Override
    public void dragDropEnd(DragSourceDropEvent dsde) {
        super.dragDropEnd(dsde);
        ViewerDropReceiver.finishDragToViewer();
    }

}
