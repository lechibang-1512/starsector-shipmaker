package shipeditor.components.instrument.ship;

import shipeditor.components.instrument.ship.centers.CollisionPanel;
import shipeditor.components.instrument.ship.centers.ShieldPanel;
import shipeditor.components.instrument.ship.hull.ShipLayerInfoPanel;
import shipeditor.utility.components.CollapsibleSection;

import javax.swing.BoxLayout;
import shipeditor.utility.components.containers.TextScrollPanel;

/**
 * Composite hull panel that embeds the existing Layer, Collision, and Shield panels
 * inside collapsible sections. Each embedded panel retains its own EventBus subscriptions
 * and refresh lifecycle — this wrapper only provides the visual grouping.
 */
public class ShipHullPanel extends TextScrollPanel {

    public ShipHullPanel() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Hull Info — identity, sprite, weapon slots summary
        ShipLayerInfoPanel layerInfoPanel = new ShipLayerInfoPanel();
        this.add(new CollapsibleSection("Hull Info", layerInfoPanel));

        // Collision — center position, collision radius, module anchor
        CollisionPanel collisionPanel = new CollisionPanel();
        this.add(new CollapsibleSection("Collision", collisionPanel));

        // Shield — shield center, shield radius, visibility
        ShieldPanel shieldPanel = new ShieldPanel();
        this.add(new CollapsibleSection("Shield", shieldPanel));
    }

}
