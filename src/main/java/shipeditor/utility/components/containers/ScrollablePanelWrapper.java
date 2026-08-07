package shipeditor.utility.components.containers;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

public class ScrollablePanelWrapper extends JPanel implements Scrollable {

    public ScrollablePanelWrapper(Component view) {
        super(new BorderLayout());
        this.add(view, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
