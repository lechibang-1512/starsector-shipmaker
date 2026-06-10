package shipeditor.utility.graphics.opengl;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class SpriteRenderer {

    private ShaderProgram shader;
    private int quadVAO;

    public SpriteRenderer() {
        initShader();
        initRenderData();
    }

    private void initShader() {
        shader = new ShaderProgram();
        
        String vertexShader = 
            "#version 330 core\n" +
            "layout (location = 0) in vec4 vertex;\n" + // <vec2 position, vec2 texCoords>
            "\n" +
            "out vec2 TexCoords;\n" +
            "\n" +
            "uniform mat4 model;\n" +
            "uniform mat4 view;\n" +
            "uniform mat4 projection;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    TexCoords = vertex.zw;\n" +
            "    gl_Position = projection * view * model * vec4(vertex.xy, 0.0, 1.0);\n" +
            "}\n";

        String fragmentShader = 
            "#version 330 core\n" +
            "in vec2 TexCoords;\n" +
            "out vec4 color;\n" +
            "\n" +
            "uniform sampler2D image;\n" +
            "uniform vec4 spriteColor;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    color = spriteColor * texture(image, TexCoords);\n" +
            "}\n";

        shader.createVertexShader(vertexShader);
        shader.createFragmentShader(fragmentShader);
        shader.link();

        shader.createUniform("model");
        shader.createUniform("view");
        shader.createUniform("projection");
        shader.createUniform("spriteColor");
        shader.createUniform("image");
    }

    private void initRenderData() {
        // Configure VAO/VBO
        float[] vertices = {
            // Pos      // Tex
            0.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f,

            0.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 0.0f, 1.0f, 0.0f
        };

        quadVAO = GL30.glGenVertexArrays();
        int vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(quadVAO);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    public void drawSprite(int textureID, Vector2f position, Vector2f size, float rotationRadians, Vector4f color, Matrix4f projection, Matrix4f view) {
        Vector2f rotationAnchor = new Vector2f(position.x + 0.5f * size.x, position.y + 0.5f * size.y);
        drawSprite(textureID, position, size, rotationAnchor, rotationRadians, color, projection, view);
    }

    public void drawSprite(int textureID, Vector2f position, Vector2f size, Vector2f rotationAnchor, float rotationRadians, Vector4f color, Matrix4f projection, Matrix4f view) {
        // Prepare transformations
        shader.bind();
        
        Matrix4f model = new Matrix4f();
        model.translate(rotationAnchor.x, rotationAnchor.y, 0.0f);
        model.rotate(rotationRadians, new Vector3f(0.0f, 0.0f, 1.0f));
        model.translate(position.x - rotationAnchor.x, position.y - rotationAnchor.y, 0.0f);
        model.scale(size.x, size.y, 1.0f);

        shader.setUniform("model", model);
        shader.setUniform("view", view);
        shader.setUniform("projection", projection);
        shader.setUniform("spriteColor", color);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        shader.setUniform("image", 0);

        GL30.glBindVertexArray(quadVAO);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
        
        shader.unbind();
    }

    public void cleanup() {
        shader.cleanup();
    }
}
