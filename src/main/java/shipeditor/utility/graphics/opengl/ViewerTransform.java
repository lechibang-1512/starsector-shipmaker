package shipeditor.utility.graphics.opengl;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import org.joml.Matrix4f;

public class ViewerTransform {

    private final AffineTransform worldToScreen = new AffineTransform();

    public AffineTransform getWorldToScreen() {
        return new AffineTransform(worldToScreen);
    }

    public AffineTransform getScreenToWorld() {
        try {
            return worldToScreen.createInverse();
        } catch (NoninvertibleTransformException e) {
            return new AffineTransform();
        }
    }

    public void translate(double dx, double dy) {
        AffineTransform tx = AffineTransform.getTranslateInstance(dx, dy);
        worldToScreen.preConcatenate(tx);
    }

    public void zoom(double x, double y, double factorX, double factorY) {
        AffineTransform tx = AffineTransform.getTranslateInstance(x, y);
        tx.scale(factorX, factorY);
        tx.translate(-x, -y);
        worldToScreen.preConcatenate(tx);
    }

    public void rotate(double x, double y, double angleRadians) {
        AffineTransform tx = AffineTransform.getTranslateInstance(x, y);
        tx.rotate(angleRadians);
        tx.translate(-x, -y);
        worldToScreen.preConcatenate(tx);
    }

    public void resetTransform() {
        worldToScreen.setToIdentity();
    }

    public static Matrix4f convertToMatrix4f(AffineTransform at) {
        if (at == null) {
            return new Matrix4f();
        }
        double[] matrix = new double[6];
        at.getMatrix(matrix);
        // matrix = [m00, m10, m01, m11, m02, m12]
        float m00 = (float) matrix[0];
        float m10 = (float) matrix[1];
        float m01 = (float) matrix[2];
        float m11 = (float) matrix[3];
        float m02 = (float) matrix[4];
        float m12 = (float) matrix[5];

        return new Matrix4f(
            m00, m10, 0f, 0f,
            m01, m11, 0f, 0f,
            0f,  0f,  1f, 0f,
            m02, m12, 0f, 1f
        );
    }
}
