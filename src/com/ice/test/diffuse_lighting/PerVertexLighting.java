package com.ice.test.diffuse_lighting;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import com.ice.common.AbstractRenderer;
import com.ice.common.TestCase;
import com.ice.graphics.CoordinateSystem;
import com.ice.graphics.SimpleGlobal;
import com.ice.graphics.geometry.Geometry;
import com.ice.graphics.geometry.GeometryData;
import com.ice.graphics.geometry.GeometryDataFactory;
import com.ice.graphics.geometry.VBOGeometry;
import com.ice.graphics.shader.FragmentShader;
import com.ice.graphics.shader.Program;
import com.ice.graphics.shader.ShaderBinder;
import com.ice.graphics.shader.VertexShader;
import com.ice.model.ObjLoader;
import com.ice.test.R;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.HashMap;
import java.util.Map;

import static android.opengl.GLES20.*;
import static com.ice.graphics.CoordinateSystem.M_V_MATRIX;
import static com.ice.graphics.CoordinateSystem.M_V_P_MATRIX;
import static com.ice.graphics.shader.ShaderFactory.fragmentShader;
import static com.ice.graphics.shader.ShaderFactory.vertexShader;

/**
 * User: Jason
 * Date: 13-2-12
 */
public class PerVertexLighting extends TestCase {
    private static final String VERTEX_SRC = "per_vertex_lighting/vertex.glsl";
    private static final String FRAGMENT_SRC = "per_vertex_lighting/fragment.glsl";

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
            nameMap.put(ShaderBinder.COLOR, "a_Color");
            nameMap.put(ShaderBinder.NORMAL, "a_Normal");

            geometryA = new VBOGeometry(geometryData, vertexShader, nameMap);

            geometryData = ObjLoader.loadObj(
                    getResources().openRawResource(R.raw.teaport)
            );

            geometryB = new VBOGeometry(geometryData, vertexShader, nameMap);
        }

        @Override
        protected void onChanged(GL10 glUnused, int width, int height) {
            glViewport(0, 0, width, height);

            CoordinateSystem.Global global = CoordinateSystem.global();

            if (global == null) {
                global = new SimpleGlobal();
                CoordinateSystem.buildGlobal(global);
            }

            SimpleGlobal simpleGlobal = (SimpleGlobal) global;
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

            VertexShader vertexShader = program.getVertexShader();
            vertexShader.bindUniform("u_LightPos", 0f, 0f, 3f);

            CoordinateSystem coordinateSystem = geometry.getCoordinateSystem();

            coordinateSystem.modelViewMatrix(M_V_MATRIX);
            vertexShader.bindUniform("u_MVMatrix", M_V_MATRIX);

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);
            vertexShader.bindUniform("u_MVPMatrix", M_V_P_MATRIX);
        }

    }

}
