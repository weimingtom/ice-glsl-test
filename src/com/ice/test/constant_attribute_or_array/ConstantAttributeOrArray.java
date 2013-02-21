package com.ice.test.constant_attribute_or_array;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import com.ice.common.AbstractRenderer;
import com.ice.common.TestCase;
import com.ice.graphics.geometry.*;
import com.ice.graphics.shader.FragmentShader;
import com.ice.graphics.shader.Program;
import com.ice.graphics.shader.VertexShader;
import com.ice.model.ObjLoader;
import com.ice.test.R;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.HashMap;
import java.util.Map;

import static android.opengl.GLES20.*;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_P_MATRIX;
import static com.ice.graphics.shader.ShaderBinder.COLOR;
import static com.ice.graphics.shader.ShaderBinder.POSITION;
import static com.ice.graphics.shader.ShaderFactory.fragmentShader;
import static com.ice.graphics.shader.ShaderFactory.vertexShader;

public class ConstantAttributeOrArray extends TestCase {
    private static final String VERTEX_SRC = "coordinate_system/vertex.glsl";
    private static final String FRAGMENT_SRC = "coordinate_system/fragment.glsl";

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

            VertexShader vertexShader = vertexShader(getAssets(), VERTEX_SRC);
            FragmentShader fragmentShader = fragmentShader(getAssets(), FRAGMENT_SRC);

            program = new Program();
            program.attachShader(vertexShader, fragmentShader);
            program.link();

            GeometryData geometryData = GeometryDataFactory.createTriangleData(3);

            Map<String, String> nameMap = new HashMap<String, String>();
            nameMap.put(POSITION, "aPosition");
            nameMap.put(COLOR, "aColor");

            geometryA = new VBOGeometry(geometryData, vertexShader);
            ((VBOGeometry) geometryA).binder(nameMap);

            geometryData = ObjLoader.loadObj(
                    getResources().openRawResource(R.raw.teaport)
            );
            geometryB = new VBOGeometry(geometryData, vertexShader);
            ((VBOGeometry) geometryB).binder(nameMap);
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
            simpleGlobal.eye(19.9f);
            simpleGlobal.perspective(45, width / (float) height, 1, 20);
        }

        @Override
        protected void onFrame(GL10 glUnused) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Do a complete rotation every 10 seconds.
            long time = SystemClock.uptimeMillis() % 10000L;
            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

            geometryA.attach();
            styleA(angleInDegrees, geometryA);
            styleB(angleInDegrees, geometryA);
            geometryA.detach();

            geometryB.attach();

            VertexShader vertexShader = geometryB.getVertexShader();

            int colorAttribute = vertexShader.findAttribute("aColor");

            glDisableVertexAttribArray(colorAttribute);

            vertexShader.uploadAttribute(colorAttribute, 0.5f, 0.6f, 0.5f, 1.0f);

            styleC(angleInDegrees, geometryB);

            geometryB.detach();

        }


        private void styleA(float angleInDegrees, Geometry geometry) {
            float[] modelMatrix = geometry.selfCoordinateSystem();

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.rotateM(
                    modelMatrix, 0,
                    angleInDegrees,
                    0f, 0f, 1.0f
            );
            updateMVPMatrix(geometry);
            geometry.draw();
        }

        private void styleB(float angleInDegrees, Geometry geometry) {
            float[] modelMatrix = geometry.selfCoordinateSystem();

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, 4f, -4, 0);
            Matrix.rotateM(
                    modelMatrix, 0,
                    angleInDegrees,
                    0.0f, 0.0f, 1.0f
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

            Matrix.translateM(modelMatrix, 0, 6f, 0, 0);

            Matrix.rotateM(
                    modelMatrix, 0,
                    angleInDegrees,
                    0, 0, 1
            );

            updateMVPMatrix(geometry);

            geometry.draw();
        }

        private void updateMVPMatrix(Geometry geometry) {

            CoordinateSystem coordinateSystem = geometry.getCoordinateSystem();

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);

            program.getVertexShader().uploadUniform("aModelViewProjectMatrix", M_V_P_MATRIX);
        }

    }

}
