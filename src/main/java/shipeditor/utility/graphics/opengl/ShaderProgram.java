package shipeditor.utility.graphics.opengl;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class ShaderProgram {

    @Getter
    private final int programId;
    private int vertexShaderId;
    private int fragmentShaderId;

    private final Map<String, Integer> uniforms;

    public ShaderProgram() {
        programId = GL20.glCreateProgram();
        if (programId == 0) {
            log.error("Could not create Shader");
        }
        uniforms = new HashMap<>();
    }

    public void createVertexShader(String shaderCode) {
        vertexShaderId = createShader(shaderCode, GL20.GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) {
        fragmentShaderId = createShader(shaderCode, GL20.GL_FRAGMENT_SHADER);
    }

    protected int createShader(String shaderCode, int shaderType) {
        int shaderId = GL20.glCreateShader(shaderType);
        if (shaderId == 0) {
            log.error("Error creating shader. Type: {}", shaderType);
            return 0;
        }

        GL20.glShaderSource(shaderId, shaderCode);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            log.error("Error compiling Shader code: {}", GL20.glGetShaderInfoLog(shaderId, 1024));
        }

        GL20.glAttachShader(programId, shaderId);

        return shaderId;
    }

    public void link() {
        GL20.glLinkProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            log.error("Error linking Shader code: {}", GL20.glGetProgramInfoLog(programId, 1024));
        }

        if (vertexShaderId != 0) {
            GL20.glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            GL20.glDetachShader(programId, fragmentShaderId);
        }

        GL20.glValidateProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == 0) {
            log.warn("Warning validating Shader code: {}", GL20.glGetProgramInfoLog(programId, 1024));
        }
    }

    public void bind() {
        GL20.glUseProgram(programId);
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
        }
    }

    public void createUniform(String uniformName) {
        int uniformLocation = GL20.glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            log.warn("Could not find uniform: {}", uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }

    public void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            GL20.glUniformMatrix4fv(uniforms.get(uniformName), false, fb);
        }
    }

    public void setUniform(String uniformName, int value) {
        GL20.glUniform1i(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, float value) {
        GL20.glUniform1f(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, Vector2f value) {
        GL20.glUniform2f(uniforms.get(uniformName), value.x, value.y);
    }

    public void setUniform(String uniformName, Vector3f value) {
        GL20.glUniform3f(uniforms.get(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, Vector4f value) {
        GL20.glUniform4f(uniforms.get(uniformName), value.x, value.y, value.z, value.w);
    }
}
