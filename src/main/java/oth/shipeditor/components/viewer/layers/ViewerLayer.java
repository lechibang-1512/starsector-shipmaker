package oth.shipeditor.components.viewer.layers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("AbstractClassWithoutAbstractMethods")
@Getter @Setter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public abstract class ViewerLayer {

    private LayerPainter painter;

}
