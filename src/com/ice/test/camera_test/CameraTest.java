package com.ice.test.camera_test;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import com.ice.engine.AbstractRenderer;
import com.ice.engine.TestCase;
import com.ice.graphics.geometry.*;
import com.ice.graphics.shader.FragmentShader;
import com.ice.graphics.shader.Program;
import com.ice.graphics.shader.ShaderBinder;
import com.ice.graphics.shader.VertexShader;
import com.ice.graphics.texture.Texture;

import javax.microedition.khronos.egl.EGLConfig;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.*;
import static com.ice.engine.Res.assetSting;
import static com.ice.graphics.geometry.CoordinateSystem.M_V_P_MATRIX;

/**
 * User: Jason
 * Date: 13-2-12
 */
public class CameraTest extends TestCase {
    private static final String VERTEX_SRC = "camera_preview/vertex.glsl";
    private static final String FRAGMENT_SRC = "camera_preview/fragment.glsl";

    private Camera mCamera;
    private SurfaceTexture surface;
    private Texture previewTexture;

    @Override
    protected void onPause() {
        super.onPause();

        mCamera.release();
    }

    @Override
    protected GLSurfaceView.Renderer buildRenderer() {
        return new Renderer();
    }

    private void startCamera(int texture) {
        surface = new SurfaceTexture(texture);

        mCamera = Camera.open();

        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(1024, 768);
            surface.setDefaultBufferSize(1024, 768);
            mCamera.setParameters(parameters);

            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private class Renderer extends AbstractRenderer {
        Program program;
        Geometry panel;

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

            Map<String, String> nameMap = new HashMap<String, String>();
            nameMap.put(ShaderBinder.POSITION, "a_Position");
            nameMap.put(ShaderBinder.TEXTURE_COORD, "a_TexCoordinate");

            IndexedGeometryData indexedGeometryData = GeometryDataFactory.createStripGridData(5, 5, 1, 1);
            indexedGeometryData.getFormatDescriptor().namespace(nameMap);
            panel = new IBOGeometry(indexedGeometryData, vsh);

            previewTexture = new Texture(GL_TEXTURE_EXTERNAL_OES) {
                @Override
                protected void onLoadTextureData() {
                }
            };
            previewTexture.prepare();

            panel.setTexture(previewTexture);
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


            startCamera(previewTexture.glRes());
        }

        @Override
        protected void onFrame() {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            panel.attach();
            surface.updateTexImage();

            animation(panel);
            updateMVPMatrix(panel);

            panel.draw();

            panel.detach();
        }

        private void updateMVPMatrix(Geometry geometry) {
            CoordinateSystem coordinateSystem = geometry.getCoordinateSystem();

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);

            program.getVertexShader().uploadUniform("u_MVPMatrix", M_V_P_MATRIX);
        }

        private void animation(Geometry geometry) {
            // Do a complete rotation every 10 seconds.
            long time = SystemClock.uptimeMillis() % 10000L;
            float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

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

    }

}
