package shipeditor.utility.graphics.opengl;

import lombok.extern.log4j.Log4j2;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class ShapeRenderer {

    private ShaderProgram shader;
    private int vao;
    private int vbo;

    private boolean isDrawing;
    private Matrix4f currentProjection;
    private Matrix4f currentView;

    private static final int CIRCLE_SEGMENTS = 64;
    private static final float[] UNIT_CIRCLE_COS = new float[CIRCLE_SEGMENTS];
    private static final float[] UNIT_CIRCLE_SIN = new float[CIRCLE_SEGMENTS];
    static {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double theta = 2.0 * Math.PI * i / CIRCLE_SEGMENTS;
            UNIT_CIRCLE_COS[i] = (float) Math.cos(theta);
            UNIT_CIRCLE_SIN[i] = (float) Math.sin(theta);
        }
    }

    private final java.nio.FloatBuffer circleBuffer = org.lwjgl.system.MemoryUtil.memAllocFloat(CIRCLE_SEGMENTS * 3 * 2);

    public ShapeRenderer() {
        initShader();
        initRenderData();
    }

    private void initShader() {
        shader = new ShaderProgram();

        String vertexShader = 
            "#version 330 core\n" +
            "layout (location = 0) in vec2 position;\n" +
            "uniform mat4 projection;\n" +
            "uniform mat4 view;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = projection * view * vec4(position, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShader = 
            "#version 330 core\n" +
            "out vec4 color;\n" +
            "uniform vec4 shapeColor;\n" +
            "uniform int useCheckerboard;\n" +
            "void main()\n" +
            "{\n" +
            "    if (useCheckerboard == 1) {\n" +
            "        float checkSize = 20.0;\n" +
            "        float total = floor(gl_FragCoord.x / checkSize) + floor(gl_FragCoord.y / checkSize);\n" +
            "        if (mod(total, 2.0) == 0.0) {\n" +
            "            color = vec4(1.0, 1.0, 1.0, 0.5);\n" +
            "        } else {\n" +
            "            color = vec4(0.8, 0.8, 0.8, 0.5);\n" +
            "        }\n" +
            "    } else {\n" +
            "        color = shapeColor;\n" +
            "    }\n" +
            "}\n";

        shader.createVertexShader(vertexShader);
        shader.createFragmentShader(fragmentShader);
        shader.link();

        shader.createUniform("projection");
        shader.createUniform("view");
        shader.createUniform("shapeColor");
        shader.createUniform("useCheckerboard");
    }

    private void initRenderData() {
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        // Dynamic draw since we update this frequently
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 1024 * Float.BYTES, GL15.GL_DYNAMIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    public void begin(Matrix4f projection, Matrix4f view) {
        if (isDrawing) {
            log.warn("ShapeRenderer was already drawing! Forcing end of previous batch to recover.");
            try {
                end();
            } catch (IllegalStateException e) {
                isDrawing = false;
            }
        }
        isDrawing = true;
        this.currentProjection = projection;
        this.currentView = view;
        shader.bind();
        shader.setUniform("projection", currentProjection);
        shader.setUniform("view", currentView);
        shader.setUniform("useCheckerboard", 0);
        GL30.glBindVertexArray(vao);
    }

    public void setUseCheckerboard(boolean enabled) {
        shader.setUniform("useCheckerboard", enabled ? 1 : 0);
    }

    public void end() {
        if (!isDrawing) throw new IllegalStateException("ShapeRenderer is not drawing!");
        isDrawing = false;
        GL30.glBindVertexArray(0);
        shader.unbind();
    }

    public void drawLine(Vector2f start, Vector2f end, Vector4f color) {
        float[] vertices = {
            start.x, start.y,
            end.x, end.y
        };

        shader.setUniform("shapeColor", color);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertices);
        
        GL11.glDrawArrays(GL11.GL_LINES, 0, 2);
    }

    public void drawRect(Vector2f position, Vector2f size, Vector4f color, boolean filled) {
        drawRect(position.x, position.y, size.x, size.y, color, filled);
    }

    public void drawRect(float x, float y, float w, float h, Vector4f color, boolean filled) {
        float[] vertices;
        int mode;
        int count;

        if (filled) {
            vertices = new float[]{
                x, y + h,
                x + w, y,
                x, y,
                x, y + h,
                x + w, y + h,
                x + w, y
            };
            mode = GL11.GL_TRIANGLES;
            count = 6;
        } else {
            vertices = new float[]{
                x, y,
                x + w, y,
                x + w, y + h,
                x, y + h
            };
            mode = GL11.GL_LINE_LOOP;
            count = 4;
        }

        shader.setUniform("shapeColor", color);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertices);

        GL11.glDrawArrays(mode, 0, count);
    }

    public void drawCircle(Vector2f center, float radius, Vector4f color, boolean filled) {
        int count = filled ? CIRCLE_SEGMENTS * 3 : CIRCLE_SEGMENTS;
        int capacityFloats = count * 2;

        circleBuffer.clear();
        
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            int nextIdx = (i + 1) % CIRCLE_SEGMENTS;
            
            float x1 = radius * UNIT_CIRCLE_COS[i] + center.x;
            float y1 = radius * UNIT_CIRCLE_SIN[i] + center.y;
            
            float x2 = radius * UNIT_CIRCLE_COS[nextIdx] + center.x;
            float y2 = radius * UNIT_CIRCLE_SIN[nextIdx] + center.y;
            
            if (filled) {
                circleBuffer.put(center.x).put(center.y);
                circleBuffer.put(x1).put(y1);
                circleBuffer.put(x2).put(y2);
            } else {
                circleBuffer.put(x1).put(y1);
            }
        }

        circleBuffer.flip();

        shader.setUniform("shapeColor", color);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        // If buffer size is larger than initial, reallocate
        if (capacityFloats * Float.BYTES > 1024 * Float.BYTES) {
             GL15.glBufferData(GL15.GL_ARRAY_BUFFER, circleBuffer, GL15.GL_DYNAMIC_DRAW);
        } else {
             GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, circleBuffer);
        }

        GL11.glDrawArrays(filled ? GL11.GL_TRIANGLES : GL11.GL_LINE_LOOP, 0, count);
    }

    public void cleanup() {
        shader.cleanup();
        if (circleBuffer != null) {
            org.lwjgl.system.MemoryUtil.memFree(circleBuffer);
        }
    }
}
