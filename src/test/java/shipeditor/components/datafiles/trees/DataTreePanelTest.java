package shipeditor.components.datafiles.trees;

import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DataTreePanelTest {

    @Test
    void testHeadlessTreePanelInstantiation() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            assertDoesNotThrow(() -> {
                // Instantiate the panels headlessly to ensure components build without NPEs
                HullsTreePanel hullsPanel = new HullsTreePanel();
                assertNotNull(hullsPanel.getTree());
                
                WeaponsTreePanel weaponsPanel = new WeaponsTreePanel();
                assertNotNull(weaponsPanel.getTree());
                
                WingsTreePanel wingsPanel = new WingsTreePanel();
                assertNotNull(wingsPanel.getTree());
                
                ProjectilesTreePanel projectilesPanel = new ProjectilesTreePanel();
                assertNotNull(projectilesPanel.getTree());
                
                HullmodsTreePanel hullmodsPanel = new HullmodsTreePanel();
                assertNotNull(hullmodsPanel.getTree());
                
                ShipSystemsTreePanel systemsPanel = new ShipSystemsTreePanel();
                assertNotNull(systemsPanel.getTree());
            });
        });
    }

}
