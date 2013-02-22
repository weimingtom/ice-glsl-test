package com.ice.test.light.diffuse_lighting;

import android.content.Context;
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
import java.util.HashMap;
import java.util.Map;

import static android.graphics.BitmapFactory.decodeResource;
import static android.graphics.Color.WHITE;
import static android.opengl.GLES20.*;
import static android.opengl.Matrix.multiplyMV;
import static com.ice.engine.Res.assetSting;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_MATRIX;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_P_MATRIX;
import static com.ice.graphics.geometry.GeometryDataFactory.createPointData;
import static com.ice.graphics.shader.ShaderBinder.*;
import static com.ice.graphics.texture.Texture.Params.LINEAR_REPEAT;

/**
 * User: Jason
 * Date: 13-2-12
 */
public class PerFragmentLighting extends TestCase {
    private static final String VERTEX_SRC = "per_fragment_lighting/vertex.glsl";
    private static final String FRAGMENT_SRC = "per_fragment_lighting/fragment.glsl";

    private static final String POINT_VERTEX_SRC = "point/vertex.glsl";
    private static final String POINT_FRAGMENT_SRC = "point/fragment.glsl";

    @Override
    protected GLSurfaceView.Renderer buildRenderer() {
        return new Renderer(this);
    }

    public static class Renderer extends AbstractRenderer {
        Context context;
        Program program;
        Geometry geometryA;
        Geometry geometryB;

        Geometry light;
        //齐次坐标 lightPosInWorldSpace[3]取1
        float[] lightPosInWorldSpace = {2f, 0, 0, 1};
        float[] lightPosInEyeSpace = new float[4];

        public Renderer(Context context) {
            this.context = context;
        }

        @Override
        protected void onCreated(EGLConfig config) {
            glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);

            VertexShader vsh = new VertexShader(assetSting(VERTEX_SRC));
            FragmentShader fsh = new FragmentShader(assetSting(FRAGMENT_SRC));

            program = new Program();
            program.attachShader(vsh, fsh);
            program.link();

            GeometryData geometryData = GeometryDataFactory.createCubeData(1);

            Map<String, String> nameMap = new HashMap<String, String>();
            nameMap.put(POSITION, "a_Position");
            nameMap.put(NORMAL, "a_Normal");
            nameMap.put(TEXTURE_COORD, "a_TexCoordinate");

            geometryData.getFormatDescriptor().namespace(nameMap);

            geometryA = new VBOGeometry(geometryData, vsh, fsh);
            geometryA.setTexture(
                    new BitmapTexture(decodeResource(context.getResources(), R.drawable.freshfruit2))
            );

            geometryData = ObjLoader.loadObj(
                    context.getResources().openRawResource(R.raw.teaport)
            );
            geometryData.getFormatDescriptor().namespace(nameMap);

            geometryB = new VBOGeometry(geometryData, vsh, fsh);
            Bitmap bitmap = decodeResource(context.getResources(), R.drawable.mask1);
            geometryB.setTexture(
                    new BitmapTexture(bitmap, LINEAR_REPEAT)
            );

            lightGeometry();
        }

        private void lightGeometry() {
            VertexShader vsh = new VertexShader(assetSting(POINT_VERTEX_SRC));
            FragmentShader fsh = new FragmentShader(assetSting(POINT_FRAGMENT_SRC));

            Program program = new Program();
            program.attachShader(vsh, fsh);
            program.link();

            GeometryData pointData = createPointData(lightPosInWorldSpace, WHITE, 10);
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
        }

        @Override
        protected void onFrame() {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Do a complete rotation every 10 seconds.
            long time = System.currentTimeMillis() % 10000L;
            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

            updateLight(angleInDegrees);

            geometryA.attach();
            styleA(angleInDegrees, geometryA);
            styleB(angleInDegrees, geometryA);
            geometryA.detach();

            geometryB.attach();
            styleC(angleInDegrees, geometryB);
            geometryB.detach();
        }

        private void updateLight(float angleInDegrees) {
            light.attach();

            float[] modelMatrix = light.selfCoordinateSystem();
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.rotateM(modelMatrix, 0, angleInDegrees, 1, 1, 1);

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

            FragmentShader fragmentShader = program.getFragmentShader();

            fragmentShader.uploadUniform(
                    "u_LightPos",
                    lightPosInEyeSpace[0],
                    lightPosInEyeSpace[1],
                    lightPosInEyeSpace[2]
            );

            fragmentShader.uploadUniform("u_Texture", 0);

            CoordinateSystem coordinateSystem = geometry.getCoordinateSystem();
            VertexShader vertexShader = program.getVertexShader();

            coordinateSystem.modelViewMatrix(M_V_MATRIX);
            vertexShader.uploadUniform("u_MVMatrix", M_V_MATRIX);

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);
            vertexShader.uploadUniform("u_MVPMatrix", M_V_P_MATRIX);
        }

    }

}
