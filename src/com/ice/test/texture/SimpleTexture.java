package com.ice.test.texture;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import com.ice.common.AbstractRenderer;
import com.ice.common.TestCase;
import com.ice.graphics.geometry.CoordinateSystem;
import com.ice.graphics.geometry.Geometry;
import com.ice.graphics.geometry.GeometryData;
import com.ice.graphics.geometry.GeometryDataFactory;
import com.ice.graphics.geometry.VBOGeometry;
import com.ice.graphics.shader.FragmentShader;
import com.ice.graphics.shader.Program;
import com.ice.graphics.shader.ShaderBinder;
import com.ice.graphics.shader.VertexShader;
import com.ice.graphics.texture.BitmapTexture;
import com.ice.model.ObjLoader;
import com.ice.test.R;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.HashMap;
import java.util.Map;

import static android.graphics.BitmapFactory.decodeResource;
import static android.opengl.GLES20.*;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_P_MATRIX;
import static com.ice.graphics.shader.ShaderFactory.fragmentShader;
import static com.ice.graphics.shader.ShaderFactory.vertexShader;
import static com.ice.graphics.texture.Texture.Params.LINEAR_REPEAT;

/**
 * User: Jason
 * Date: 13-2-12
 */
public class SimpleTexture extends TestCase {
    private static final String VERTEX_SRC = "texture/vertex.glsl";
    private static final String FRAGMENT_SRC = "texture/fragment.glsl";

    @Override
    protected GLSurfaceView.Renderer buildRenderer() {
        return new Renderer();
    }

    private class Renderer extends AbstractRenderer {
        Program program;
        Geometry geometryA;
        Geometry geometryB;

        @Override
        protected void onCreated(GL10 glUnused, EGLConfig config) {
            glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);

            VertexShader vertexShader = vertexShader(getAssets(), VERTEX_SRC);
            FragmentShader fragmentShader = fragmentShader(getAssets(), FRAGMENT_SRC);

            program = new Program();
            program.attachShader(vertexShader, fragmentShader);
            program.link();

            GeometryData geometryData = GeometryDataFactory.createCubeData(1);

            Map<String, String> nameMap = new HashMap<String, String>();
            nameMap.put(ShaderBinder.POSITION, "a_Position");
            nameMap.put(ShaderBinder.TEXTURE_COORD, "a_TexCoordinate");

            geometryData.getFormatDescriptor().namespace(nameMap);

            geometryA = new VBOGeometry(geometryData, vertexShader);
            geometryA.setTexture(
                    new BitmapTexture(decodeResource(getResources(), R.drawable.freshfruit2))
            );

            geometryData = ObjLoader.loadObj(
                    getResources().openRawResource(R.raw.teaport)
            );
            geometryData.getFormatDescriptor().namespace(nameMap);

            geometryB = new VBOGeometry(geometryData, vertexShader);
            Bitmap bitmap = decodeResource(getResources(), R.drawable.mask1);
            geometryB.setTexture(
                    new BitmapTexture(bitmap, LINEAR_REPEAT)
            );

        }

        @Override
        protected void onChanged(GL10 glUnused, int width, int height) {
            glViewport(0, 0, width, height);

            CoordinateSystem.Global global = CoordinateSystem.global();

            if (global == null) {
                global = new CoordinateSystem.SimpleGlobal();
                CoordinateSystem.buildGlobal(global);
            }

            CoordinateSystem.SimpleGlobal simpleGlobal = (CoordinateSystem.SimpleGlobal) global;
            simpleGlobal.eye(6);
            simpleGlobal.perspective(45, width / (float) height, 1, 10);
        }


        @Override
        protected void onFrame(GL10 glUnused) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Do a complete rotation every 10 seconds.
            long time = System.currentTimeMillis() % 10000L;
            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

            geometryA.attach();
            styleA(angleInDegrees, geometryA);
            styleB(angleInDegrees, geometryA);
            geometryA.detach();

            geometryB.attach();
            styleC(angleInDegrees, geometryB);
            geometryB.detach();
        }

        private void styleA(float angleInDegrees, Geometry geometry) {
            float[] modelMatrix = geometry.selfCoordinateSystem();

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.rotateM(
                    modelMatrix, 0,
                    angleInDegrees,
                    1.0f, 1.0f, 1.0f
            );
            updateMVPMatrix(geometry);
            geometry.draw();
        }

        private void styleB(float angleInDegrees, Geometry geometry) {
            float[] modelMatrix = geometry.selfCoordinateSystem();

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, 1.5f, -1, 0);
            Matrix.rotateM(
                    modelMatrix, 0,
                    angleInDegrees,
                    1.0f, 1.0f, 1.0f
            );
            updateMVPMatrix(geometry);
            geometry.draw();
        }

        private void styleC(float angleInDegrees, Geometry geometry) {
            float[] modelMatrix = geometry.selfCoordinateSystem();

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.rotateM(
                    modelMatrix, 0,
                    angleInDegrees,
                    0f, 0f, 1f
            );

            Matrix.translateM(modelMatrix, 0, 1.5f, 0, 0);

            Matrix.rotateM(
                    modelMatrix, 0,
                    angleInDegrees,
                    1, 1, 1
            );

            updateMVPMatrix(geometry);
            geometry.draw();
        }

        private void updateMVPMatrix(Geometry geometry) {
            CoordinateSystem coordinateSystem = geometry.getCoordinateSystem();

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);

            program.getVertexShader().uploadUniform("u_MVPMatrix", M_V_P_MATRIX);
        }

    }

}
