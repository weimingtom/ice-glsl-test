package com.ice.test.camera_test;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
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

    private Texture previewTexture;
    private SurfaceTexture surfaceTexture;
    private CameraProxy camera;

    private PointF displaySize = new PointF(2.0f, 1.0f);
    private Rect cameraPreviewBounds = new Rect(0, 0, 1024, 768);

    private float[] textureScaleVect = new float[2];

    @Override
    protected void onPause() {
        super.onPause();

        camera.release();

    }

    private void updateTextureVec() {
        PointF srcTextureSize = new PointF();

        float displayAspect = displaySize.y / displaySize.x;
        float previewAspect = cameraPreviewBounds.height() / (float) cameraPreviewBounds.width();

        if (displayAspect > previewAspect) {
            srcTextureSize.y = cameraPreviewBounds.height();
            srcTextureSize.x = srcTextureSize.y / displayAspect;
        } else {
            srcTextureSize.x = cameraPreviewBounds.width();
            srcTextureSize.y = srcTextureSize.x * displayAspect;
        }

        textureScaleVect[0] = srcTextureSize.x / cameraPreviewBounds.width();
        textureScaleVect[1] = srcTextureSize.y / cameraPreviewBounds.height();
    }

    @Override
    protected GLSurfaceView.Renderer buildRenderer() {
        return new Renderer();
    }

    private void startCamera(int texture) {
        surfaceTexture = new SurfaceTexture(texture);

        camera = buildCamera();
        camera.startPreview();
    }

    private CameraProxy buildCamera() {
        Cameras cameras = Cameras.getInstance();
        CameraFace face = CameraFace.Back;
        int id = cameras.getCameraId(face);
        Camera.CameraInfo cameraInfo = cameras.getCameraInfo(face);

        Camera opened = Camera.open(id);

        try {
            Camera.Parameters parameters = opened.getParameters();
            parameters.setPreviewSize(cameraPreviewBounds.width(), cameraPreviewBounds.height());
            opened.setParameters(parameters);

            opened.setPreviewTexture(surfaceTexture);

        } catch (IOException e) {
            e.printStackTrace();
        }

        CameraProxy camera = new CameraProxy(id, opened, cameraInfo);

        //opened.setDisplayOrientation(90);
        //CameraHelper.setCameraDisplayOrientation(this, id, opened);

        CameraHelper.init(this, opened, 1.0);

        return camera;
    }

    private class Renderer extends AbstractRenderer {
        Program program;
        Geometry panel;
        private float[] textureMatrix = new float[16];

        @Override
        protected void onCreated(EGLConfig config) {
            glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

            glEnable(GL_CULL_FACE);

            VertexShader vsh = new VertexShader(assetSting(VERTEX_SRC));
            FragmentShader fsh = new FragmentShader(assetSting(FRAGMENT_SRC));

            program = new Program();
            program.attachShader(vsh, fsh);
            program.link();

            Map<String, String> nameMap = new HashMap<String, String>();
            nameMap.put(ShaderBinder.POSITION, "a_Position");
            nameMap.put(ShaderBinder.TEXTURE_COORD, "a_TexCoordinate");

            IndexedGeometryData indexedGeometryData =
                    GeometryDataFactory.createStripGridData(displaySize.x, displaySize.y, 1, 1);
            indexedGeometryData.getFormatDescriptor().namespace(nameMap);
            panel = new IBOGeometry(indexedGeometryData, vsh);

            previewTexture = new Texture(GL_TEXTURE_EXTERNAL_OES) {
                @Override
                protected void onLoadTextureData() {
                }
            };
            previewTexture.prepare();

            panel.setTexture(previewTexture);

            CoordinateSystem coordinateSystem = panel.getCoordinateSystem();
            float[] matrix = coordinateSystem.modelMatrix();
            Matrix.rotateM(matrix, 0, -90, 0, 0, 1);
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

            float left = -1.0f * 1.5f;
            float top = -left * height / (float) width;
            simpleGlobal.ortho(left, -left, -top, top, 0, 10);

            startCamera(previewTexture.glRes());
        }

        @Override
        protected void onFrame() {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            panel.attach();
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(textureMatrix);

            updateTextureVec();

            updateShaderParams();

            panel.draw();

            panel.detach();
        }

        private void updateShaderParams() {
            CoordinateSystem coordinateSystem = panel.getCoordinateSystem();

            coordinateSystem.modelViewProjectMatrix(M_V_P_MATRIX);

            program.getVertexShader().uploadUniform("u_MVPMatrix", M_V_P_MATRIX);

            FragmentShader fsh = program.getFragmentShader();
            fsh.uploadUniform("u_TextureScale", textureScaleVect);
        }

    }

}
