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

import static android.graphics.Color.WHITE;
import static android.opengl.GLES20.*;
import static android.opengl.Matrix.multiplyMV;
import static com.ice.graphics.CoordinateSystem.*;
import static com.ice.graphics.geometry.GeometryDataFactory.createPointData;
import static com.ice.graphics.shader.ShaderFactory.fragmentShader;
import static com.ice.graphics.shader.ShaderFactory.vertexShader;

/**
 * User: Jason
 * Date: 13-2-12
 */
public class PerVertexLighting extends TestCase {
    private static final String VERTEX_SRC = "per_vertex_lighting/vertex.glsl";
    private static final String FRAGMENT_SRC = "per_vertex_lighting/fragment.glsl";

    private static final String POINT_VERTEX_SRC = "point/vertex.glsl";
    private static final String POINT_FRAGMENT_SRC = "point/fragment.glsl";

    @Override
    protected GLSurfaceView.Renderer buildRenderer() {
        return new Renderer();
    }

    private class Renderer extends AbstractRenderer {
        Program program;
        Geometry geometryA;
        Geometry geometryB;

        Geometry light;
        float[] lightPosInWorldSpace = {3, 0, 0, 0};
        float[] lightPosInEyeSpace = new float[4];

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

            lightGeometry();
        }

        private void lightGeometry() {
            VertexShader vertexShader = vertexShader(getAssets(), POINT_VERTEX_SRC);
            FragmentShader fragmentShader = fragmentShader(getAssets(), POINT_FRAGMENT_SRC);

            Program program = new Program();
            program.attachShader(vertexShader, fragmentShader);
            program.link();
            GeometryData pointData = createPointData(lightPosInWorldSpace, WHITE, 10);
            light = new VBOGeometry(pointData, vertexShader);
        }

        @Override
        protected void onChanged(GL10 glUnused, int width, int height) {
            glViewport(0, 0, width, height);

            CoordinateSystem.Global global = global();

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

            drawLight(angleInDegrees);

            geometryA.attach();
            styleA(angleInDegrees, geometryA);
            styleB(angleInDegrees, geometryA);
            geometryA.detach();

            geometryB.attach();
            styleC(angleInDegrees, geometryB);
            geometryB.detach();


        }

        private void drawLight(float angleInDegrees) {
            light.attach();

            float[] modelMatrix = light.selfCoordinateSystem();
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.rotateM(modelMatrix, 0, angleInDegrees, 0, 0, 1);

            CoordinateSystem coordinateSystem = light.getCoordinateSystem();

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);

            light.getVertexShader().uploadUniform("u_MVPMatrix", M_V_P_MATRIX);

            coordinateSystem.modelViewMatrix(M_V_MATRIX);

            multiplyMV(lightPosInEyeSpace, 0, M_V_MATRIX, 0, lightPosInWorldSpace, 0);

            light.draw();

            light.detach();
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

            vertexShader.uploadUniform(
                    "u_LightPos",
                    lightPosInEyeSpace[0],
                    lightPosInEyeSpace[1],
                    lightPosInEyeSpace[2]
            );

            CoordinateSystem coordinateSystem = geometry.getCoordinateSystem();

            coordinateSystem.modelViewMatrix(M_V_MATRIX);
            vertexShader.uploadUniform("u_MVMatrix", M_V_MATRIX);

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);
            vertexShader.uploadUniform("u_MVPMatrix", M_V_P_MATRIX);
        }

    }

}