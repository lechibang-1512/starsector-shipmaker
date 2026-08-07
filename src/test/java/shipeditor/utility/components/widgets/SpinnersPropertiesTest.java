package shipeditor.utility.components.widgets;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import javax.swing.JPanel;
import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SpinnersPropertiesTest {

    @Property
    void testCreateLocationSpinners(
            @ForAll @DoubleRange(min = -10000.0, max = 10000.0) double initialX,
            @ForAll @DoubleRange(min = -10000.0, max = 10000.0) double initialY,
            @ForAll String labelX,
            @ForAll String labelY) {

        Point2D initial = new Point2D.Double(initialX, initialY);
        AtomicReference<Point2D> current = new AtomicReference<>(initial);

        TwinSpinnerPanel panel = Spinners.createLocationSpinners(
                initial,
                current::get,
                current::set,
                labelX == null ? "X" : labelX,
                labelY == null ? "Y" : labelY
        );

        assertNotNull(panel);
        assertNotNull(panel.getFirstSpinner());
        assertNotNull(panel.getSecondSpinner());

        // Test clearing spinners
        panel.clear();
        assertEquals(0.0d, panel.getFirstSpinner().getValue());
        assertEquals(0.0d, panel.getSecondSpinner().getValue());

        // Test enabling/disabling
        panel.setEnabled(false);
        assertFalse(panel.getFirstSpinner().isEnabled());
        assertFalse(panel.getSecondSpinner().isEnabled());

        panel.setEnabled(true);
        assertTrue(panel.getFirstSpinner().isEnabled());
        assertTrue(panel.getSecondSpinner().isEnabled());
    }

    @Property
    void testAddLabelWithDegreeSpinnerDoesNotCrash(
            @ForAll String label,
            @ForAll int yPos) {

        JPanel container = new JPanel(new java.awt.GridBagLayout());
        assertDoesNotThrow(() -> Spinners.addLabelWithDegreeSpinner(
                container,
                label == null ? "" : label,
                val -> {},
                Math.abs(yPos) % 100
        ));
    }
}
