package shipeditor.utility.graphics.opengl;

import org.joml.Matrix4f;

public interface OpenGLPainter {
    /**
     * Called during the render loop.
     * @param spriteRenderer The renderer for textured quads.
     * @param shapeRenderer The renderer for primitive shapes.
     * @param projection The current camera projection matrix.
     * @param view The current camera view matrix.
     */
    void paint(SpriteRenderer spriteRenderer, ShapeRenderer shapeRenderer, Matrix4f projection, Matrix4f view);
}
