import org.kordamp.ikonli.boxicons.BoxiconsRegular;
public class FindIcons {
    public static void main(String[] args) {
        for (BoxiconsRegular b : BoxiconsRegular.values()) {
            if (b.name().contains("ROCKET") || b.name().contains("SHIP") || b.name().contains("PLANE") || b.name().contains("TARGET") || b.name().contains("CROSSHAIR") || b.name().contains("JET") || b.name().contains("SWORD") || b.name().contains("GUN") || b.name().contains("AIM")) {
                System.out.println(b.name());
            }
        }
    }
}
