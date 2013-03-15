package com.ice.test.light.directional_light;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import com.ice.engine.AbstractRenderer;
import com.ice.engine.TestCase;
import com.ice.graphics.geometry.*;
import com.ice.graphics.shader.FragmentShader;
import com.ice.graphics.shader.Program;
import com.ice.graphics.shader.VertexShader;
import com.ice.graphics.texture.BitmapTexture;
import com.ice.model.ObjLoader;
import com.ice.test.R;

import javax.microedition.khronos.egl.EGLConfig;

import static android.graphics.Color.WHITE;
import static android.opengl.GLES20.*;
import static android.opengl.Matrix.multiplyMV;
import static com.ice.engine.Res.*;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_MATRIX;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_P_MATRIX;
import static com.ice.graphics.geometry.GeometryDataFactory.createPointData;
import static com.ice.graphics.texture.Texture.Params.LINEAR_REPEAT;

/**
 * User: jason
 * Date: 13-2-22
 */
public class DirectionalLightTest extends TestCase {
    private static final String VERTEX_SRC = "directional_light/per_fragment/vertex.glsl";
    private static final String FRAGMENT_SRC = "directional_light/per_fragment/fragment.glsl";

    private static final String POINT_VERTEX_SRC = "point/vertex.glsl";
    private static final String POINT_FRAGMENT_SRC = "point/fragment.glsl";

    @Override
    protected GLSurfaceView.Renderer buildRenderer() {
        return new Renderer();
    }

    private class Renderer extends AbstractRenderer {
        Program program;
        Geometry plane;
        Geometry geometryA;
        Geometry geometryB;
        Geometry light;
        float[] lightPosInSelfSpace = {0, 2, 1, 1};
        float[] lightVectorInWorldSpace = new float[4];

        @Override
        protected void onCreated(EGLConfig config) {

            glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

            glEnable(GL_DEPTH_TEST);
            //glEnable(GL_CULL_FACE);

            VertexShader vsh = new VertexShader(assetSting(VERTEX_SRC));
            FragmentShader fsh = new FragmentShader(assetSting(FRAGMENT_SRC));

            program = new Program();
            program.attachShader(vsh, fsh);
            program.link();

            GeometryData geometryData = GeometryDataFactory.createCubeData(1);
            geometryA = new VBOGeometry(geometryData, vsh);
            geometryA.setTexture(
                    new BitmapTexture(bitmap(R.drawable.freshfruit2))
            );

            geometryData = ObjLoader.loadObj(openRaw(R.raw.teaport));
            geometryB = new VBOGeometry(geometryData, vsh);
            Bitmap bitmap = bitmap(R.drawable.mask1);
            geometryB.setTexture(
                    new BitmapTexture(bitmap, LINEAR_REPEAT)
            );

            geometryData = GeometryDataFactory.createStripGridData(5, 5, 1, 1);
            plane = new VBOGeometry(geometryData, vsh);
            plane.setTexture(
                    new BitmapTexture(bitmap(R.drawable.poker_back))
            );

            lightGeometry();
        }

        private void lightGeometry() {
            VertexShader vsh = new VertexShader(assetSting(POINT_VERTEX_SRC));
            FragmentShader fsh = new FragmentShader(assetSting(POINT_FRAGMENT_SRC));

            Program program = new Program();
            program.attachShader(vsh, fsh);
            program.link();

            GeometryData pointData = createPointData(lightPosInSelfSpace, WHITE, 10);
            light = new VBOGeometry(pointData, vsh);
        }

        @Override
        protected void onChanged(int width, int height) {
            glViewport(0, 0, width, height);

            CoordinateSystem.Global global = CoordinateSystem.global();

            if (global == null) {
                global = new CoordinateSystem.SimpleGlobal();
                CoordinateSystem.buildGlobal(global);
            }

            CoordinateSystem.SimpleGlobal simpleGlobal = (CoordinateSystem.SimpleGlobal) global;
            simpleGlobal.eye(6);
            simpleGlobal.perspective(45, width / (float) height, 1, 10);


//            float[] viewMatrix = simpleGlobal.viewMatrix();
//
//            Matrix.rotateM(
//                    viewMatrix, 0,
//                    -60,
//                    1.0f, 0, 0
//            );
        }

        @Override
        protected void onFrame() {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Do a complete rotation every 10 seconds.
            long time = System.currentTimeMillis() % 10000L;
            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

            updateLight(angleInDegrees);

            program.attach();
            FragmentShader fragmentShader = program.getFragmentShader();

            light.getCoordinateSystem().modelViewMatrix(M_V_MATRIX);

            float[] dir = new float[]{
                    lightPosInSelfSpace[0],
                    lightPosInSelfSpace[1],
                    lightPosInSelfSpace[2],
                    0
            };

            multiplyMV(lightVectorInWorldSpace, 0, light.getCoordinateSystem().modelMatrix(), 0, dir, 0);

            fragmentShader.uploadUniform(
                    "u_LightVector",
                    lightVectorInWorldSpace[0],
                    lightVectorInWorldSpace[1],
                    lightVectorInWorldSpace[2]
            );


            geometryA.attach();
            styleA(angleInDegrees, geometryA);
            styleB(angleInDegrees, geometryA);
            geometryA.detach();

            geometryB.attach();
            styleC(angleInDegrees, geometryB);
            geometryB.detach();

            drawPanel();
        }

        private void updateLight(float angleInDegrees) {
            light.attach();

            float[] modelMatrix = light.selfCoordinateSystem();
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.rotateM(modelMatrix, 0, angleInDegrees, 0, 0, 1);

            CoordinateSystem coordinateSystem = light.getCoordinateSystem();

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);

            light.getVertexShader().uploadUniform("u_MVPMatrix", M_V_P_MATRIX);

            light.draw();

            light.detach();
        }

        private void drawPanel() {
            plane.attach();

            updateMVPMatrix(plane);

            plane.draw();

            plane.detach();
        }

        private void styleA(float angleInDegrees, Geometry geometry) {
            float[] modelMatrix = geometry.selfCoordinateSystem();

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(
                    modelMatrix, 0,
                    0, 0, 0.5f
            );
//            Matrix.rotateM(
//                    modelMatrix, 0,
//                    angleInDegrees,
//                    0f, 0f, 1.0f
//            );
            updateMVPMatrix(geometry);
            geometry.draw();
        }

        private void styleB(float angleInDegrees, Geometry geometry) {
            float[] modelMatrix = geometry.selfCoordinateSystem();

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, 1.5f, -1, 0);
//            Matrix.rotateM(
//                    modelMatrix, 0,
//                    angleInDegrees,
//                    0f, 0f, 1.0f
//            );
            updateMVPMatrix(geometry);
            geometry.draw();
        }

        private void styleC(float angleInDegrees, Geometry geometry) {
            float[] modelMatrix = geometry.selfCoordinateSystem();

            Matrix.setIdentityM(modelMatrix, 0);

            Matrix.rotateM(
                    modelMatrix, 0,
                    90,
                    1, 0.0f, 0
            );

//            Matrix.rotateM(
//                    modelMatrix, 0,
//                    angleInDegrees,
//                    0, 1.0f, 0
//            );

            Matrix.translateM(modelMatrix, 0, -1.5f, 0, 0);

//            Matrix.rotateM(
//                    modelMatrix, 0,
//                    angleInDegrees,
//                    0, 1, 0
//            );

            updateMVPMatrix(geometry);
            geometry.draw();
        }

        private void updateMVPMatrix(Geometry geometry) {
            VertexShader vertexShader = program.getVertexShader();

            CoordinateSystem coordinateSystem = geometry.getCoordinateSystem();

            vertexShader.uploadUniform("u_MMatrix", coordinateSystem.modelMatrix());

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);

            vertexShader.uploadUniform("u_MVPMatrix", M_V_P_MATRIX);
        }

    }

}
