package com.ice.test.blur;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import com.ice.engine.AbstractRenderer;
import com.ice.engine.TestCase;
import com.ice.graphics.FBO;
import com.ice.graphics.geometry.CoordinateSystem;
import com.ice.graphics.geometry.Geometry;
import com.ice.graphics.geometry.IBOGeometry;
import com.ice.graphics.shader.FragmentShader;
import com.ice.graphics.shader.Program;
import com.ice.graphics.shader.VertexShader;
import com.ice.graphics.texture.BitmapTexture;
import com.ice.graphics.texture.FboTexture;
import com.ice.graphics.texture.Texture;
import com.ice.test.R;

import javax.microedition.khronos.egl.EGLConfig;

import static android.opengl.GLES20.*;
import static com.ice.engine.Res.bitmap;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_P_MATRIX;
import static com.ice.graphics.geometry.GeometryDataFactory.createStripGridData;
import static com.ice.test.Util.assetProgram;

/**
 * User: Jason
 * Date: 13-2-12
 */
public class BlurTest extends TestCase {
    private static final String VSH = "blur/normal.vsh";
    private static final String FSH = "blur/normal.fsh";

    private static final String BLUR_VSH = "blur/blur.vsh";
    private static final String BLUR_FSH = "blur/blur.fsh";

    @Override
    protected GLSurfaceView.Renderer buildRenderer() {
        return new Renderer();
    }

    private class Renderer extends AbstractRenderer {
        private int width, height;
        private int fboWidth, fboHeight;

        Program program, blurProgram;
        Geometry panle;
        FBO fboA, fboB;
        private FboTexture fboTextureA;
        private BitmapTexture bitmapTexture;
        private Geometry panleLarge;
        private FboTexture fboTextureB;

        @Override
        protected void onCreated(EGLConfig config) {
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glEnable(GL_DEPTH_TEST);

            program = assetProgram(VSH, FSH);
            blurProgram = assetProgram(BLUR_VSH, BLUR_FSH);

            Bitmap bitmap = bitmap(R.drawable.freshfruit2);

            panle = new IBOGeometry(
                    createStripGridData(1.0f, 1.0f * bitmap.getHeight() / (float) bitmap.getWidth(), 1, 1),
                    program.getVertexShader()
            );

            panleLarge = new IBOGeometry(
                    createStripGridData(2.0f, 2.0f * bitmap.getHeight() / (float) bitmap.getWidth(), 1, 1),
                    program.getVertexShader()
            );

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

            fboWidth = Math.round(width / 3.0f);
            fboHeight = Math.round(height / 3.0f);
//            fboWidth = width;
//            fboHeight = height;

            fboTextures();

            CoordinateSystem.Global global = CoordinateSystem.global();

            if (global == null) {
                global = new CoordinateSystem.SimpleGlobal();
                CoordinateSystem.buildGlobal(global);
            }

            CoordinateSystem.SimpleGlobal simpleGlobal = (CoordinateSystem.SimpleGlobal) global;
            simpleGlobal.eye(1);
            simpleGlobal.frustum(-1, height / (float) width, 1.0f, 3.0f);

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
            fboB.attach();
            fboTextureB.prepare();
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTextureB.glRes(), 0);
            fboB.detach();
        }

        @Override
        protected void onFrame() {
            program.attach();
            renderToTexture(fboA);

            blurProgram.attach();

            VertexShader vsh = blurProgram.getVertexShader();
            vsh.uploadUniform("u_MVPMatrix", M_V_P_MATRIX);
            FragmentShader fsh = blurProgram.getFragmentShader();
            fsh.uploadUniform("TexelSize", (float) fboWidth, (float) fboHeight);

            fsh.uploadUniform("Orientation", 0);
            renderToTextureAndBlur(fboTextureA, fboB, true);
            fsh.uploadUniform("Orientation", 1);
            renderToTextureAndBlur(fboTextureB, fboA, false);

            program.attach();
            showBlurResult(fboTextureA);

        }

        private void renderToTexture(FBO fbo) {
            fbo.attach();
            glClear(GL_COLOR_BUFFER_BIT);
            glViewport(0, 0, fboWidth, fboHeight);

            VertexShader vsh = program.getVertexShader();
            vsh.uploadUniform("u_MVPMatrix", M_V_P_MATRIX);

            panle.setVertexShader(vsh);
            bitmapTexture.attach();
            panle.attach();
            panle.draw();
            panle.detach();
        }

        private void showBlurResult(Texture texture) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glViewport(0, 0, width, height);

            texture.attach();

            VertexShader vsh = program.getVertexShader();
            vsh.uploadUniform("u_MVPMatrix", M_V_P_MATRIX);

            panleLarge.setVertexShader(vsh);
            panleLarge.attach();
            panleLarge.draw();
            panleLarge.detach();
        }

        private void blurV(FBO fbo) {

        }

        private void renderToTextureAndBlur(Texture textureToBlur, FBO fbo, boolean horizontal) {
            fbo.attach();
            glClear(GL_COLOR_BUFFER_BIT);
            glViewport(0, 0, fboWidth, fboHeight);

            VertexShader vsh = blurProgram.getVertexShader();
            panleLarge.setVertexShader(vsh);
            textureToBlur.attach();
            panleLarge.attach();
            panleLarge.draw();
            panleLarge.detach();
            textureToBlur.detach();

            fbo.detach();
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
