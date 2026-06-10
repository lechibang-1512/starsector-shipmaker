package shipeditor.undo.edits;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import shipeditor.communication.EventBus;
import shipeditor.communication.events.viewer.layers.ActiveLayerUpdated;
import shipeditor.components.datafiles.entities.HullmodCSVEntry;
import shipeditor.components.viewer.layers.LayerPainter;
import shipeditor.components.viewer.layers.ship.ShipLayer;
import shipeditor.undo.AbstractEdit;
import shipeditor.undo.EditCategory;
import shipeditor.utility.overseers.StaticController;

import java.util.List;
import java.util.function.Consumer;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class HullmodEdits {

    private HullmodEdits() {
    }

    public static class HullmodAddEdit extends AbstractEdit implements LayerEdit {
        private final List<HullmodCSVEntry> hullmodIndex;
        private ShipLayer layer;
        private final HullmodCSVEntry entry;

        public HullmodAddEdit(List<HullmodCSVEntry> index, ShipLayer shipLayer, HullmodCSVEntry hullmod) {
            this.hullmodIndex = index;
            this.layer = shipLayer;
            this.entry = hullmod;
        }

        @Override
        public void undo() {
            hullmodIndex.remove(entry);
            if (StaticController.getActiveLayer() == layer) {
                EventBus.publish(new ActiveLayerUpdated(layer));
            }
        }

        @Override
        public void redo() {
            hullmodIndex.add(entry);
            if (StaticController.getActiveLayer() == layer) {
                EventBus.publish(new ActiveLayerUpdated(layer));
            }
        }

        @Override
        public String getName() {
            return "Add Hullmod";
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

    public static class HullmodRemoveEdit extends AbstractEdit implements LayerEdit {
        private final List<HullmodCSVEntry> hullmodIndex;
        private ShipLayer layer;
        private final HullmodCSVEntry entry;
        private final int positionIndex;

        public HullmodRemoveEdit(List<HullmodCSVEntry> index, ShipLayer shipLayer, HullmodCSVEntry hullmod, int positionIndex) {
            this.hullmodIndex = index;
            this.layer = shipLayer;
            this.entry = hullmod;
            this.positionIndex = positionIndex;
        }

        @Override
        public void undo() {
            hullmodIndex.add(positionIndex, entry);
            if (StaticController.getActiveLayer() == layer) {
                EventBus.publish(new ActiveLayerUpdated(layer));
            }
        }

        @Override
        public void redo() {
            hullmodIndex.remove(entry);
            if (StaticController.getActiveLayer() == layer) {
                EventBus.publish(new ActiveLayerUpdated(layer));
            }
        }

        @Override
        public String getName() {
            return "Remove Hullmod";
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

    public static class HullmodsSortEdit extends AbstractEdit implements LayerEdit  {
        private final List<HullmodCSVEntry> oldList;
        private final List<HullmodCSVEntry> newList;
        private ShipLayer layer;
        private final Consumer<List<HullmodCSVEntry>> sortSetter;

        public HullmodsSortEdit(List<HullmodCSVEntry> old, List<HullmodCSVEntry> updated, ShipLayer shipLayer,
                                Consumer<List<HullmodCSVEntry>> setter) {
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
            return "Sort Hullmods";
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
