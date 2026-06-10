package shipeditor.utility.objects;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SimpleRectangle {

    private int x;

    private int y;

    private int width;

    private int height;

    public SimpleRectangle() {}

    @SuppressWarnings("ParameterHidesMemberVariable")
    public SimpleRectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

}
