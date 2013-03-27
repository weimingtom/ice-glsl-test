package com.ice.test.blur;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.Display;
import android.view.WindowManager;
import com.ice.engine.AbstractRenderer;
import com.ice.engine.TestCase;
import com.ice.graphics.FBO;
import com.ice.graphics.geometry.*;
import com.ice.graphics.shader.Program;
import com.ice.graphics.texture.BitmapTexture;
import com.ice.graphics.texture.FboTexture;
import com.ice.test.R;
import com.ice.test.Util;

import javax.microedition.khronos.egl.EGLConfig;

import static android.opengl.GLES20.*;
import static com.ice.engine.Res.bitmap;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_P_MATRIX;
import static com.ice.graphics.geometry.GeometryDataFactory.createStripGridData;

/**
 * User: Jason
 * Date: 13-2-12
 */
public class BlurTest extends TestCase {
    private static final String VERTEX_SRC = "blur/normal.vsh";
    private static final String FRAGMENT_SRC = "blur/normal.fsh";

    @Override
    protected GLSurfaceView.Renderer buildRenderer() {
        return new Renderer();
    }

    private class Renderer extends AbstractRenderer {
        private int width, height;
        private int fboWidth, fboHeight;

        Program program;
        Geometry panle;
        FBO fboA, fboB;
        private FboTexture fboTextureA;
        private BitmapTexture bitmapTexture;
        private VBOGeometry panleScreen;
        private Geometry panleLarge;
        private FboTexture fboTextureB;

        @Override
        protected void onCreated(EGLConfig config) {

            WindowManager windowManager = getWindow().getWindowManager();
            Display defaultDisplay = windowManager.getDefaultDisplay();

            int defaultDisplayWidth = defaultDisplay.getWidth();
            int defaultDisplayHeight = defaultDisplay.getHeight();

            System.out.println("defaultDisplayWidth = " + defaultDisplayWidth);
            System.out.println("defaultDisplayHeight = " + defaultDisplayHeight);

            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glEnable(GL_DEPTH_TEST);

            program = Util.assetProgram(VERTEX_SRC, FRAGMENT_SRC);

            Bitmap bitmap = bitmap(R.drawable.freshfruit2);

            panle = new IBOGeometry(
                    createStripGridData(1.0f, 1.0f * bitmap.getHeight() / (float) bitmap.getWidth(), 1, 1),
                    program.getVertexShader()
            );

            panleLarge = new IBOGeometry(
                    createStripGridData(2.0f, 2.0f * bitmap.getHeight() / (float) bitmap.getWidth(), 1, 1),
                    program.getVertexShader()
            );

            GeometryData geometryData = createStripGridData(1f, bitmap.getHeight() / (float) bitmap.getWidth(), 1, 1);
            panleScreen = new VBOGeometry(geometryData, program.getVertexShader());

            bitmapTexture = new BitmapTexture(bitmap);

            fboA = new FBO();
            fboA.prepare();

            fboB = new FBO();
            fboB.prepare();
        }

        @Override
        protected void onChanged(int width, int height) {
            this.width = width;
            this.height = height;
            fboWidth = Math.round(width / 2.0f);
            fboHeight = Math.round(height / 2.0f);

            fboTextures();

            CoordinateSystem.Global global = CoordinateSystem.global();

            if (global == null) {
                global = new CoordinateSystem.SimpleGlobal();
                CoordinateSystem.buildGlobal(global);
            }

            CoordinateSystem.SimpleGlobal simpleGlobal = (CoordinateSystem.SimpleGlobal) global;
            simpleGlobal.eye(1);
            simpleGlobal.frustum(-1, height / (float) width, 1, 3);

            CoordinateSystem coordinateSystem = panle.getCoordinateSystem();

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);

        }

        private void fboTextures() {
            if (fboTextureA != null) {
                fboTextureA.release();
            }

            fboTextureA = new FboTexture(fboWidth, fboHeight);
            fboTextureA.prepare();
            fboA.attach();
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTextureA.glRes(), 0);
            fboA.detach();


            if (fboTextureB != null) {
                fboTextureB.release();
            }

            fboTextureB = new FboTexture(fboWidth, fboHeight);
            fboTextureB.prepare();
            fboB.attach();
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTextureB.glRes(), 0);
            fboB.detach();
        }

        @Override
        protected void onFrame() {
            glViewport(0, 0, width, height);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            renderToTexture();

            blur();

            showBlurResult();

        }

        private void showBlurResult() {
            glClear(GL_COLOR_BUFFER_BIT);
            glViewport(0, 0, width, height);

            fboTextureA.attach();

            panleLarge.attach();
            program.attach();
            program.getVertexShader().uploadUniform("u_MVPMatrix", M_V_P_MATRIX);
            panleLarge.draw();
            panleLarge.detach();
        }

        private void blur() {

        }

        private void renderToTexture() {
            fboA.attach();
            glClear(GL_COLOR_BUFFER_BIT);
            glViewport(0, 0, fboWidth, fboHeight);

            program.attach();

            bitmapTexture.attach();

            panle.attach();
            program.getVertexShader().uploadUniform("u_MVPMatrix", M_V_P_MATRIX);
            panle.draw();
            panle.detach();

            fboA.detach();
        }

        private void styleA(float angleInDegrees, Geometry geometry) {
            float[] modelMatrix = geometry.selfCoordinateSystem();

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(
                    modelMatrix, 0,
                    0, 0, 0.5f
            );
            Matrix.rotateM(
                    modelMatrix, 0,
                    angleInDegrees,
                    0f, 0f, 1.0f
            );
        }

    }

}
