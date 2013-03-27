package com.ice.test.shadow_test;

import android.opengl.GLSurfaceView;
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
import static android.opengl.Matrix.*;
import static com.ice.engine.Res.*;
import static com.ice.graphics.GlUtil.checkError;
import static com.ice.graphics.GlUtil.checkFramebufferStatus;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_MATRIX;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_P_MATRIX;
import static com.ice.graphics.geometry.CoordinateSystem.SimpleGlobal;
import static com.ice.graphics.geometry.GeometryDataFactory.createPointData;
import static com.ice.graphics.geometry.GeometryDataFactory.createStripGridData;
import static com.ice.graphics.shader.ShaderBinder.POSITION;
import static com.ice.graphics.texture.Texture.Params.LINEAR_REPEAT;
import static com.ice.model.ObjLoader.loadObj;

/**
 * User: jason
 * Date: 13-2-22
 */
public class ShadowTest extends TestCase {
    private static final String SHADOW_MAP_VERTEX_SRC = "shadow_map/shadow_map_vertex.glsl";
    private static final String SHADOW_MAP_FRAGMENT_SRC = "shadow_map/shadow_map_fragment.glsl";

    private static final String DEPTH_VERTEX_SRC = "shadow_map/depth_vertex.glsl";
    private static final String DEPTH_FRAGMENT_SRC = "shadow_map/depth_fragment.glsl";

    private static final String NORMAL_VERTEX_SRC = "shadow_map/normal_vertex.glsl";
    private static final String NORMAL_FRAGMENT_SRC = "shadow_map/normal_fragment.glsl";

    private static final String POINT_VERTEX_SRC = "shadow_map/point_vertex.glsl";
    private static final String POINT_FRAGMENT_SRC = "shadow_map/point_fragment.glsl";

    private static final String BLUR_VERTEX_SRC = "shadow_map/blur_vertex.glsl";
    private static final String BLUR_FRAGMENT_SRC = "shadow_map/blur_fragment.glsl";

    private CoordinateSystem coordinateSystem = new CoordinateSystem();

    @Override
    protected GLSurfaceView.Renderer buildRenderer() {
        return new Renderer();
    }

    private class Renderer extends AbstractRenderer {
        VBO vbo, plane, light;
        Program normalProgram, pointProgram, depthProgram, shadowMapProgram, blurProgram;

        FBO fbo;
        FboTexture depthMap;
        Texture textureA, textureB;

        float angle = 45;
        float zRate = (float) Math.tan(Math.toRadians(angle));
        float y = 1.5f;
        float[] lightPosInSelfSpace = {0, y, y * zRate, 1};
        float[] lightVectorInWorldSpace = new float[4];
        float[] lightVectorInViewSpace = new float[4];
        GeometryData vboData, planeData;
        VBOGeometry.EasyBinder vboBinder, planeBinder;
        SimpleGlobal lightGlobal;
        private float[] lightModelMatrix = new float[16];
        private float[] lightMVPInLightSpace = new float[16];
        private float[] lightMVPInViewSpace = new float[16];
        private float[] vboModelMatrix = new float[16];
        private float[] vboMVPMatrix = new float[16];
        private float[] vboMVMatrix = new float[16];
        private FboTexture bluredDepthMap;
        private IndexedGeometryData blurGridData;
        private VBO blurGrid;
        private VBOGeometry.EasyBinder blurBinder;
        private float[] blurMVPMatrix = new float[16];
        private boolean blur;

        @Override
        protected void onCreated(EGLConfig config) {
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            glEnable(GL_DEPTH_TEST);

            programs();

            vboData = loadObj(openRaw(R.raw.teaport));
            vbo = new VBO(vboData.getVertexData());
            vboBinder = new VBOGeometry.EasyBinder(vboData.getFormatDescriptor());

            planeData = createStripGridData(5, 5, 1, 1);
            plane = new VBO(planeData.getVertexData());
            planeBinder = new VBOGeometry.EasyBinder(planeData.getFormatDescriptor());

            blurGridData = createStripGridData(2.0f, 2.0f, 1, 1);
            blurGrid = new VBO(blurGridData.getVertexData());
            blurBinder = new VBOGeometry.EasyBinder(blurGridData.getFormatDescriptor());

            light = new VBO(
                    createPointData(lightPosInSelfSpace, WHITE, 10).getVertexData()
            );

            textureA = new BitmapTexture(bitmap(R.drawable.poker_back));

            textureB = new BitmapTexture(
                    bitmap(R.drawable.mask1),
                    LINEAR_REPEAT
            );

            depthMap = new FboTexture(768, 920);
            depthMap.setDataStorage(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT);

            bluredDepthMap = new FboTexture(768, 920);

            fbo();
        }

        @Override
        protected void onChanged(int width, int height) {
            glViewport(0, 0, width, height);

            CoordinateSystem.Global global = CoordinateSystem.global();

            if (global == null) {
                global = new SimpleGlobal();
                CoordinateSystem.buildGlobal(global);
            }

            SimpleGlobal simpleGlobal = (SimpleGlobal) global;

            //simpleGlobal.eye(6);
            simpleGlobal.eye(
                    0, -4, 4,
                    0, 0, 0,
                    0, 1, 0
            );

            float aspect = width / (float) height;
            simpleGlobal.perspective(45, aspect, 1, 10);
//            simpleGlobal.ortho(
//                    -aspect, aspect, -1.0f, 1.0f,
//                    0.1f, 10.0f
//            );
//            Matrix.rotateM(
//                    simpleGlobal.viewMatrix(), 0,
//                    -60,
//                    1.0f, 0, 0
//            );

            lightGlobal = new SimpleGlobal();

            lightGlobal.perspective(45, aspect, 1f, 10);
//            float bottom = 2.5f;
//            lightGlobal.ortho(
//                    -aspect * bottom, aspect * bottom, -bottom, bottom,
//                    0.1f, 10.0f
//            );

            SimpleGlobal blurGlobal = new SimpleGlobal();
            blurGlobal.eye(1);
            blurGlobal.ortho(
                    -aspect, aspect, -1.0f, 1.0f,
                    0.5f, 5
            );
            multiplyMM(blurMVPMatrix, 0, blurGlobal.projectMatrix(), 0, blurGlobal.viewMatrix(), 0);
        }

        @Override
        protected void onFrame() {
            updateModels();

            fbo.attach();
            depthProgram.attach();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glEnable(GL_CULL_FACE);

            glColorMask(false, false, false, false);
            drawDepth();

            glDisable(GL_CULL_FACE);
            glColorMask(true, true, true, true);
            glDepthMask(false);

            if (blur) {
                blurProgram.attach();
                blurDepth();
            }

            glDepthMask(true);
            fbo.detach();

            checkError();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            pointProgram.attach();
            drawLight();
            checkError();

            normalProgram.attach();
            glActiveTexture(GL_TEXTURE0);
            textureB.attach();
            drawVbo();
            textureB.detach();
            checkError();

            boolean showShadow = true;

            if (showShadow) {
                shadowMapProgram.attach();
                drawPlaneWithShadow();
            } else {
                drawPlaneWithoutShadow();
            }

        }

        private void blurDepth() {
            glActiveTexture(GL_TEXTURE0);
            depthMap.attach();

            blurGrid.attach();

            VertexShader vsh = blurProgram.getVertexShader();

            blurBinder.bind(null, vsh, null);

            vsh.uploadUniform("u_MVPMatrix", blurMVPMatrix);

            FragmentShader fsh = blurProgram.getFragmentShader();

            fsh.uploadUniform("TexelSize", 768.0f, 920.f);
            fsh.uploadUniform("Orientation", 0);
            fsh.uploadUniform("BlurAmount", 3);

            glDrawArrays(GL_TRIANGLE_STRIP, 0, blurGridData.getFormatDescriptor().getCount());

            blurGrid.detach();
            depthMap.detach();
        }

        private void drawPlaneWithoutShadow() {
            FragmentShader fragmentShader = normalProgram.getFragmentShader();

            fragmentShader.uploadUniform(
                    "u_LightVector",
                    lightVectorInViewSpace[0],
                    lightVectorInViewSpace[1],
                    lightVectorInViewSpace[2]
            );

            glActiveTexture(GL_TEXTURE0);
            bluredDepthMap.attach();

            drawPanel(false);

            bluredDepthMap.detach();

        }

        private void updateModels() {
            long time = System.currentTimeMillis() % 10000L;
            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

            CoordinateSystem.Global global = CoordinateSystem.global();

            //*******************light
            setIdentityM(lightModelMatrix, 0);
            rotateM(lightModelMatrix, 0, angleInDegrees, 0, 0, 1);

            multiplyMM(M_V_MATRIX, 0, global.viewMatrix(), 0, lightModelMatrix, 0);
            multiplyMM(lightMVPInViewSpace, 0, global.projectMatrix(), 0, M_V_MATRIX, 0);

            float[] dir = new float[]{
                    lightPosInSelfSpace[0],
                    lightPosInSelfSpace[1],
                    lightPosInSelfSpace[2],
                    0
            };
            multiplyMV(lightVectorInWorldSpace, 0, lightModelMatrix, 0, dir, 0);
            multiplyMV(lightVectorInViewSpace, 0, M_V_MATRIX, 0, dir, 0);

            float rate = 3.0f;
            float eyeX = lightVectorInWorldSpace[0] * rate;
            float eyeY = lightVectorInWorldSpace[1] * rate;
            float eyeZ = lightVectorInWorldSpace[2] * rate;
            lightGlobal.eye(
                    eyeX, eyeY, eyeZ,
                    0, 0, 0,
                    0, 1, 0
            );

//            double sqrt = MathUtil.sqrt(eyeX * eyeX + eyeY * eyeY + eyeZ * eyeZ);
//            System.out.println("sqrt = " + sqrt);

            multiplyMM(lightMVPInLightSpace, 0, lightGlobal.projectMatrix(), 0, lightGlobal.viewMatrix(), 0);
            //*******************light

            //**********************vbo
            setIdentityM(vboModelMatrix, 0);
            rotateM(vboModelMatrix, 0, 90, 1, 0.0f, 0);
            rotateM(vboModelMatrix, 0, angleInDegrees, 0, 1.0f, 0);
            translateM(vboModelMatrix, 0, -1.0f, 0, 0);
            rotateM(vboModelMatrix, 0, angleInDegrees, 0, 1, 0);

            multiplyMM(vboMVMatrix, 0, global.viewMatrix(), 0, vboModelMatrix, 0);
            multiplyMM(vboMVPMatrix, 0, global.projectMatrix(), 0, vboMVMatrix, 0);
            //**********************vbo
        }

        private void drawPlaneWithShadow() {
            VertexShader vertexShader = shadowMapProgram.getVertexShader();
            vertexShader.uploadUniform("u_LightMVPMatrix", lightMVPInLightSpace);

            FragmentShader fragmentShader = shadowMapProgram.getFragmentShader();

            fragmentShader.uploadUniform(
                    "u_LightVector",
                    lightVectorInViewSpace[0],
                    lightVectorInViewSpace[1],
                    lightVectorInViewSpace[2]
            );

            glActiveTexture(GL_TEXTURE0);
            textureA.attach();

            glActiveTexture(GL_TEXTURE1);
            Texture depthTexture = blur ? bluredDepthMap : depthMap;
            depthTexture.attach();

            fragmentShader.uploadUniform("u_DepthMap", 1);

            drawPanel(true);

            depthTexture.detach();
            textureA.detach();
        }

        private void drawLight() {
            light.attach();

            VertexShader vertexShader = pointProgram.getVertexShader();
            vertexShader.uploadUniform("u_MVPMatrix", lightMVPInViewSpace);

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

        private void drawPanel(boolean showShadow) {
            plane.attach();

            float[] modelMatrix = coordinateSystem.modelMatrix();

            setIdentityM(modelMatrix, 0);

            VertexShader vertexShader;

            if (showShadow) {
                vertexShader = shadowMapProgram.getVertexShader();
                vertexShader.uploadUniform("u_MMatrix", modelMatrix);
            } else {
                vertexShader = normalProgram.getVertexShader();
            }

            coordinateSystem.modelViewMatrix(M_V_MATRIX);
            vertexShader.uploadUniform("u_MVMatrix", M_V_MATRIX);

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);
            vertexShader.uploadUniform("u_MVPMatrix", M_V_P_MATRIX);

            planeBinder.bind(null, vertexShader, null);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, planeData.getFormatDescriptor().getCount());

            plane.detach();
        }

        private void drawVbo() {
            vbo.attach();

            normalProgram.getFragmentShader().uploadUniform("u_LightVector",
                    lightVectorInViewSpace[0],
                    lightVectorInViewSpace[1],
                    lightVectorInViewSpace[2]
            );

            VertexShader vertexShader = normalProgram.getVertexShader();

            vertexShader.uploadUniform("u_MVMatrix", vboMVMatrix);
            vertexShader.uploadUniform("u_MVPMatrix", vboMVPMatrix);

            vboBinder.bind(null, vertexShader, null);
            glDrawArrays(GL_TRIANGLES, 0, vboData.getFormatDescriptor().getCount());
        }

        private void programs() {
            normalProgram = new Program();
            normalProgram.attachShader(
                    new VertexShader(assetSting(NORMAL_VERTEX_SRC)),
                    new FragmentShader(assetSting(NORMAL_FRAGMENT_SRC))
            );
            normalProgram.link();

            pointProgram = new Program();
            pointProgram.attachShader(
                    new VertexShader(assetSting(POINT_VERTEX_SRC)),
                    new FragmentShader(assetSting(POINT_FRAGMENT_SRC))
            );
            pointProgram.link();

            depthProgram = new Program();
            depthProgram.attachShader(
                    new VertexShader(assetSting(DEPTH_VERTEX_SRC)),
                    new FragmentShader(assetSting(DEPTH_FRAGMENT_SRC))
            );
            depthProgram.link();

            shadowMapProgram = new Program();
            shadowMapProgram.attachShader(
                    new VertexShader(assetSting(SHADOW_MAP_VERTEX_SRC)),
                    new FragmentShader(assetSting(SHADOW_MAP_FRAGMENT_SRC))
            );
            shadowMapProgram.link();


            blurProgram = new Program();
            blurProgram.attachShader(
                    new VertexShader(assetSting(BLUR_VERTEX_SRC)),
                    new FragmentShader(assetSting(BLUR_FRAGMENT_SRC))
            );
            blurProgram.link();
        }

        private void fbo() {
            fbo = new FBO();
            fbo.attach();
            depthMap.attach();
            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    GL_TEXTURE_2D,
                    depthMap.glRes(),
                    0
            );

            bluredDepthMap.attach();

            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D,
                    bluredDepthMap.glRes(),
                    0
            );

            checkFramebufferStatus();

            fbo.detach();
            checkError();
        }

        private void drawDepth() {
            vbo.attach();
            VertexShader vertexShader = depthProgram.getVertexShader();

            vertexShader.uploadUniform("u_LightMVPMatrix", lightMVPInLightSpace);
            vertexShader.uploadUniform("u_ModelMatrix", vboModelMatrix);

            GeometryData.Descriptor descriptor = vboData.getFormatDescriptor();
            GeometryData.Component component = descriptor.find(POSITION);
            vertexShader.findAttribute("a_Position").pointer(
                    component.dimension,
                    component.type,
                    component.normalized,
                    descriptor.getStride(),
                    component.offset
            );

            glDrawArrays(GL_TRIANGLES, 0, descriptor.getCount());
        }

    }

}
