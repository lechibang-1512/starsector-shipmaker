package shipeditor.communication.events.components;

import shipeditor.components.instrument.ship.variant.VariantDataTab;

public record VariantDataTabSelected(VariantDataTab selected) implements ComponentEvent {

}
