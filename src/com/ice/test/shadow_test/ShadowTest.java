package com.ice.test.shadow_test;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import com.ice.engine.AbstractRenderer;
import com.ice.engine.TestCase;
import com.ice.graphics.FBO;
import com.ice.graphics.VBO;
import com.ice.graphics.geometry.CoordinateSystem;
import com.ice.graphics.geometry.GeometryData;
import com.ice.graphics.geometry.IndexedGeometryData;
import com.ice.graphics.geometry.VBOGeometry;
import com.ice.graphics.shader.FragmentShader;
import com.ice.graphics.shader.Program;
import com.ice.graphics.shader.VertexShader;
import com.ice.graphics.texture.BitmapTexture;
import com.ice.graphics.texture.FboTexture;
import com.ice.graphics.texture.Texture;
import com.ice.test.R;

import javax.microedition.khronos.egl.EGLConfig;

import static android.graphics.Color.WHITE;
import static android.opengl.GLES20.*;
import static android.opengl.Matrix.multiplyMV;
import static com.ice.engine.Res.*;
import static com.ice.graphics.GlUtil.checkError;
import static com.ice.graphics.GlUtil.checkFramebufferStatus;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_MATRIX;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_P_MATRIX;
import static com.ice.graphics.geometry.GeometryDataFactory.createPointData;
import static com.ice.graphics.geometry.GeometryDataFactory.createStripGridData;
import static com.ice.graphics.texture.Texture.Params.LINEAR_REPEAT;
import static com.ice.model.ObjLoader.loadObj;

/**
 * User: jason
 * Date: 13-2-22
 */
public class ShadowTest extends TestCase {
    private static final String DEPTH_VERTEX_SRC = "shadow_map/depth_vertex.glsl";
    private static final String DEPTH_FRAGMENT_SRC = "shadow_map/depth_fragment.glsl";

    private static final String VERTEX_SRC = "shadow_map/normal_vertex.glsl";
    private static final String FRAGMENT_SRC = "shadow_map/normal_fragment.glsl";

    private static final String POINT_VERTEX_SRC = "shadow_map/point_vertex.glsl";
    private static final String POINT_FRAGMENT_SRC = "shadow_map/point_fragment.glsl";

    private CoordinateSystem coordinateSystem = new CoordinateSystem();

    @Override
    protected GLSurfaceView.Renderer buildRenderer() {
        return new Renderer();
    }

    private class Renderer extends AbstractRenderer {
        VBO vbo, plane, light;
        Program normalProgram, pointProgram;

        private FBO fbo;
        private FboTexture fboTexture;
        private Texture textureA, textureB;

        float[] lightPosInSelfSpace = {0, 1.5f, 1.2f, 1};
        float[] lightVectorInViewSpace = new float[4];
        private GeometryData vboData;
        private IndexedGeometryData planeData;
        private VBOGeometry.EasyBinder vboBinder, planeBinder;

        @Override
        protected void onCreated(EGLConfig config) {
            glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

            glEnable(GL_DEPTH_TEST);

            programs();

            vboData = loadObj(openRaw(R.raw.teaport));
            vbo = new VBO(vboData.getVertexData());
            vboBinder = new VBOGeometry.EasyBinder(vboData.getFormatDescriptor());

            planeData = createStripGridData(5, 5, 1, 1);
            plane = new VBO(planeData.getVertexData());
            planeBinder = new VBOGeometry.EasyBinder(planeData.getFormatDescriptor());

            light = new VBO(
                    createPointData(lightPosInSelfSpace, WHITE, 10).getVertexData()
            );

            textureA = new BitmapTexture(bitmap(R.drawable.poker_back));

            textureB = new BitmapTexture(
                    bitmap(R.drawable.mask1),
                    LINEAR_REPEAT
            );

            fboTexture = new FboTexture(768, 920);

            checkError();
            //fbo();
        }

        private void programs() {
            normalProgram = new Program();
            normalProgram.attachShader(
                    new VertexShader(assetSting(VERTEX_SRC)),
                    new FragmentShader(assetSting(FRAGMENT_SRC))
            );
            normalProgram.link();

            pointProgram = new Program();
            VertexShader vsh = new VertexShader(assetSting(POINT_VERTEX_SRC));
            pointProgram.attachShader(
                    vsh,
                    new FragmentShader(assetSting(POINT_FRAGMENT_SRC))
            );
            pointProgram.link();
        }

        private void fbo() {
            fbo = new FBO();
            fbo.attach();
            fboTexture.attach();
            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    GL_TEXTURE_2D,
                    fboTexture.glRes(),
                    0
            );
            checkFramebufferStatus();

            fbo.detach();
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

            float[] viewMatrix = simpleGlobal.viewMatrix();

            Matrix.rotateM(
                    viewMatrix, 0,
                    -60,
                    1.0f, 0, 0
            );
        }

        @Override
        protected void onFrame() {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            long time = System.currentTimeMillis() % 10000L;
            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

            pointProgram.attach();
            drawLight(angleInDegrees);
            checkError();

            normalProgram.attach();

            bindLight();

            textureB.attach();
            drawVbo(angleInDegrees);
            textureB.detach();

            textureA.attach();
            drawPanel();
            textureA.detach();
        }

        private void bindLight() {
            coordinateSystem.modelViewMatrix(M_V_MATRIX);

            float[] dir = new float[]{
                    lightPosInSelfSpace[0],
                    lightPosInSelfSpace[1],
                    lightPosInSelfSpace[2],
                    0
            };

            multiplyMV(lightVectorInViewSpace, 0, M_V_MATRIX, 0, dir, 0);

            normalProgram.getFragmentShader().uploadUniform(
                    "u_LightVector",
                    lightVectorInViewSpace[0],
                    lightVectorInViewSpace[1],
                    lightVectorInViewSpace[2]
            );
        }

        private void drawLight(float angleInDegrees) {
            light.attach();

            float[] modelMatrix = coordinateSystem.modelMatrix();

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.rotateM(modelMatrix, 0, angleInDegrees, 0, 0, 1);

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);

            VertexShader vertexShader = pointProgram.getVertexShader();
            vertexShader.uploadUniform("u_MVPMatrix", M_V_P_MATRIX);

            vertexShader.findAttribute("a_Position").pointer(
                    3,
                    GL_FLOAT,
                    false,
                    0,
                    0
            );


            glDrawArrays(GL_POINTS, 0, 1);
            light.detach();
        }

        private void drawPanel() {
            plane.attach();

            float[] modelMatrix = coordinateSystem.modelMatrix();

            Matrix.setIdentityM(modelMatrix, 0);

            VertexShader vertexShader = normalProgram.getVertexShader();
            coordinateSystem.modelViewMatrix(M_V_MATRIX);
            vertexShader.uploadUniform("u_MVMatrix", M_V_MATRIX);

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);
            vertexShader.uploadUniform("u_MVPMatrix", M_V_P_MATRIX);

            planeBinder.bind(null, vertexShader, null);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, planeData.getFormatDescriptor().getCount());

            plane.detach();
        }

        private void drawVbo(float angleInDegrees) {
            vbo.attach();

            float[] modelMatrix = coordinateSystem.modelMatrix();

            Matrix.setIdentityM(modelMatrix, 0);

            Matrix.rotateM(
                    modelMatrix, 0,
                    90,
                    1, 0.0f, 0
            );

            Matrix.rotateM(
                    modelMatrix, 0,
                    angleInDegrees,
                    0, 1.0f, 0
            );

            Matrix.translateM(modelMatrix, 0, -1.5f, 0, 0);

            Matrix.rotateM(
                    modelMatrix, 0,
                    angleInDegrees,
                    0, 1, 0
            );

            VertexShader vertexShader = normalProgram.getVertexShader();

            coordinateSystem.modelViewMatrix(M_V_MATRIX);
            vertexShader.uploadUniform("u_MVMatrix", M_V_MATRIX);

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);
            vertexShader.uploadUniform("u_MVPMatrix", M_V_P_MATRIX);

            vboBinder.bind(null, vertexShader, null);
            glDrawArrays(GL_TRIANGLES, 0, vboData.getFormatDescriptor().getCount());
        }

    }

}
