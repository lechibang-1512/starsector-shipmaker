package oth.shipeditor.utility.objects;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SimpleRectangle {

    public int x;

    public int y;

    public int width;

    public int height;

    public SimpleRectangle() {}

    @SuppressWarnings("ParameterHidesMemberVariable")
    public SimpleRectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

}
