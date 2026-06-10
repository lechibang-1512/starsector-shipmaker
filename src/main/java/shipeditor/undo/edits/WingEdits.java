package shipeditor.undo.edits;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.ActiveLayerUpdated;
import shipeditor.components.datafiles.entities.WingCSVEntry;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.undo.AbstractEdit;
import shipeditor.undo.EditCategory;
import shipeditor.utility.overseers.StaticController;

import java.util.List;
import java.util.function.Consumer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class WingEdits {

    private WingEdits() {
    }

    public static class WingAddEdit extends AbstractEdit {
        private final List<WingCSVEntry> wingIndex;
        private final ShipLayer layer;
        private final WingCSVEntry entry;

        public WingAddEdit(List<WingCSVEntry> index, ShipLayer shipLayer, WingCSVEntry wing) {
            this.wingIndex = index;
            this.layer = shipLayer;
            this.entry = wing;
        }

        @Override
        public void undo() {
            wingIndex.remove(entry);
            if (StaticController.getActiveLayer() == layer) {
                EventBus.publish(new ActiveLayerUpdated(layer));
            }
        }

        @Override
        public void redo() {
            wingIndex.add(entry);
            if (StaticController.getActiveLayer() == layer) {
                EventBus.publish(new ActiveLayerUpdated(layer));
            }
        }

        @Override
        public String getName() {
            return "Add Wing";
        }

        @Override
        public EditCategory getCategory() {
            return EditCategory.VARIANT;
        }
    }

    public static class WingRemoveEdit extends AbstractEdit implements LayerEdit {
        private final List<WingCSVEntry> wingIndex;
        private ShipLayer layer;
        private final WingCSVEntry entry;
        private final int positionIndex;

        public WingRemoveEdit(List<WingCSVEntry> index, ShipLayer shipLayer, WingCSVEntry wing, int entryIndex) {
            this.wingIndex = index;
            this.layer = shipLayer;
            this.entry = wing;
            this.positionIndex = entryIndex;
        }

        @Override
        public void undo() {
            wingIndex.add(positionIndex, entry);
            if (StaticController.getActiveLayer() == layer) {
                EventBus.publish(new ActiveLayerUpdated(layer));
            }
        }

        @Override
        public void redo() {
            wingIndex.remove(positionIndex);
            if (StaticController.getActiveLayer() == layer) {
                EventBus.publish(new ActiveLayerUpdated(layer));
            }
        }

        @Override
        public String getName() {
            return "Remove Wing";
        }

        @Override
        public LayerPainter getLayerPainter() {
            return layer.getPainter();
        }

        @Override
        public void cleanupReferences() {
            layer = null;
        }

        @Override
        public EditCategory getCategory() {
            return EditCategory.VARIANT;
        }
    }

    public static class WingsSortEdit extends AbstractEdit implements LayerEdit {
        private final List<WingCSVEntry> oldList;
        private final List<WingCSVEntry> newList;
        private ShipLayer layer;
        private final Consumer<List<WingCSVEntry>> sortSetter;

        public WingsSortEdit(List<WingCSVEntry> old, List<WingCSVEntry> updated, ShipLayer shipLayer,
                             Consumer<List<WingCSVEntry>> setter) {
            this.oldList = old;
            this.newList = updated;
            this.layer = shipLayer;
            this.sortSetter = setter;
        }

        @Override
        public void undo() {
            sortSetter.accept(oldList);
            if (StaticController.getActiveLayer() == layer) {
                EventBus.publish(new ActiveLayerUpdated(layer));
            }
        }

        @Override
        public void redo() {
            sortSetter.accept(newList);
            if (StaticController.getActiveLayer() == layer) {
                EventBus.publish(new ActiveLayerUpdated(layer));
            }
        }

        @Override
        public String getName() {
            return "Sort Wings";
        }

        @Override
        public LayerPainter getLayerPainter() {
            return layer.getPainter();
        }

        @Override
        public void cleanupReferences() {
            layer = null;
        }

        @Override
        public EditCategory getCategory() {
            return EditCategory.VARIANT;
        }
    }
}
