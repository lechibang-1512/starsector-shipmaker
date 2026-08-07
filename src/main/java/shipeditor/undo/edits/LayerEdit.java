package shipeditor.undo.edits;

import shipeditor.components.viewer.layers.LayerPainter;

public interface LayerEdit {

    default LayerPainter getLayerPainter() {
        for (java.lang.reflect.Field f : this.getClass().getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                Object val = f.get(this);
                if (val instanceof LayerPainter lp) return lp;
                if (val instanceof shipeditor.components.viewer.entities.WorldPoint wp) return wp.getParent();
                
                if (val != null) {
                    try {
                        java.lang.reflect.Method m = val.getClass().getMethod("getParentLayer");
                        Object res = m.invoke(val);
                        if (res instanceof LayerPainter lp) return lp;
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {}
        }
        return null;
    }

    default void cleanupReferences() {
    }

}
