package shipeditor.undo.edits;

import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.LayerEvents.ActiveLayerUpdated;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.layers.ship.data.ShipVariant;
import shipeditor.undo.AbstractEdit;
import shipeditor.utility.UtilityEnums.EditCategory;

public final class VariantDataEdits {

    private VariantDataEdits() {
    }

    public static class VariantFieldEdit<T> extends AbstractEdit {
        private final ShipVariant variant;
        private final ViewerLayer layer;
        private final T oldValue;
        private final T newValue;
        private final java.util.function.Consumer<T> setter;
        private final String fieldName;

        public VariantFieldEdit(ShipVariant variant, ViewerLayer layer, T oldValue, T newValue, java.util.function.Consumer<T> setter, String fieldName) {
            this.variant = variant;
            this.layer = layer;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.setter = setter;
            this.fieldName = fieldName;
        }

        @Override
        public void undo() {
            setter.accept(oldValue);
            EventBus.publish(new ActiveLayerUpdated(layer));
        }

        @Override
        public void redo() {
            setter.accept(newValue);
            EventBus.publish(new ActiveLayerUpdated(layer));
        }

        @Override
        public String getName() {
            return "Change Variant " + fieldName;
        }

        @Override
        public EditCategory getCategory() {
            return EditCategory.VARIANT;
        }
    }
}
