package oth.shipeditor.communication.events.components;

import oth.shipeditor.components.instrument.ship.variant.VariantDataTab;

public record VariantDataTabSelected(VariantDataTab selected) implements ComponentEvent {

}
