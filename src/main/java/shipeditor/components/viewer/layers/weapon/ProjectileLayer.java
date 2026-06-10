package shipeditor.components.viewer.layers.weapon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import shipeditor.components.viewer.layers.ViewerLayer;
import shipeditor.components.viewer.painters.points.weapon.ProjectilePainter;
import shipeditor.representation.weapon.ProjectileSpecFile;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public class ProjectileLayer extends ViewerLayer {

    @Getter @Setter
    private ProjectileSpecFile specFile;

    @Override
    public ProjectileLayerPainter getPainter() {
        return (ProjectileLayerPainter) super.getPainter();
    }

    public String getSpecFileName() {
        if (specFile != null && specFile.getProjectileSpecFilePath() != null) {
            return String.valueOf(specFile.getProjectileSpecFilePath().getFileName());
        }
        return "";
    }

}
