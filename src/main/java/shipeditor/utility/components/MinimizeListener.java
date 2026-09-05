package shipeditor.utility.components;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.extern.log4j.Log4j2;

import javax.swing.JTabbedPane;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** * This minimize-restore listener is inspired by behavior of tabbed panels in IntelliJ;
 * It is certainly not something implemented in a matter of minutes, took me some hours to tune all interactions.*/
@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class MinimizeListener extends MouseAdapter {

    private final JTabbedPane parent;
    private final MinimizerWidget minimizer;
    private int pressTabIndex = -1;
    private java.awt.Point pressPoint;

    public MinimizeListener(JTabbedPane pane, MinimizerWidget widget) {
        this.parent = pane;
        this.minimizer = widget;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        pressPoint = e.getPoint();
        pressTabIndex = parent.indexAtLocation(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        if (pressTabIndex == -1) {
            minimizer.setPanelSwitched(false);
            return;
        }

        boolean withinDistance = true;
        if (pressPoint != null) {
            double distanceSq = pressPoint.distanceSq(e.getPoint());
            withinDistance = (distanceSq <= 100); // 10px drag threshold
        }

        if (withinDistance) {
            if (minimizer.isPanelSwitched()) {
                if (minimizer.isMinimized()) {
                    minimizer.maximize();
                }
            } else {
                if (minimizer.isMinimized()) {
                    minimizer.maximize();
                } else {
                    minimizer.minimize();
                }
            }
        }

        minimizer.setPanelSwitched(false);
        pressTabIndex = -1;
        pressPoint = null;
    }

}
