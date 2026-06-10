package shipeditor.communication.events.viewer.layers;

import shipeditor.communication.events.BusEvent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import shipeditor.components.viewer.layers.ViewerLayer;

/** * Note: layer instance argument is always assumed by recipients to be active layer.
 * Passing any other than active layer as argument will likely lead to subtle bugs.
 * <p>
 * As of 16.10.23 also causes layer selection event through layer manager, thereby updating common instrument panels.*/
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public record ActiveLayerUpdated(ViewerLayer updated) implements BusEvent {

}
