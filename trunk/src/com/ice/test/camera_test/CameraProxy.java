package com.ice.test.camera_test;

import android.hardware.Camera;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

/**
 * User: jason
 */
public class CameraProxy {

    private final int id;
    private int degrees;
    private final Camera camera;
    private final CameraFace face;
    private final boolean autoFocusSupported;
    private final Camera.CameraInfo info;

    private boolean released;
    private int jpegRotation;

    public CameraProxy(int id, Camera camera, Camera.CameraInfo info) {
        if (id < 0 || camera == null || info == null) {
            throw new IllegalArgumentException();
        }

        this.id = id;
        this.camera = camera;
        this.info = info;

        boolean back = info.facing == Camera.CameraInfo.CAMERA_FACING_BACK;
        face = back ? CameraFace.Back : CameraFace.Front;

        Camera.Parameters parameters = camera.getParameters();
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        autoFocusSupported = supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
    }

    public boolean isAutoFocusSupported() {
        return autoFocusSupported;
    }

    public int getId() {
        return id;
    }

    public CameraFace getFace() {
        return face;
    }

    public Camera.CameraInfo getInfo() {
        return info;
    }

    public void release() {
        if (released) throw new IllegalStateException();

        released = true;
        camera.release();
    }

    public void stopPreview() {
        camera.stopPreview();
    }

    public void setPreviewDisplay(SurfaceHolder holder) throws IOException {
        camera.setPreviewDisplay(holder);
    }

    public void startPreview() {
        camera.startPreview();
    }

    public void autoFocus(Camera.AutoFocusCallback autoFocusCallback) {
        camera.autoFocus(autoFocusCallback);
    }

    public final void takePicture(Camera.ShutterCallback shutter, Camera.PictureCallback raw,
                                  Camera.PictureCallback postview, Camera.PictureCallback jpeg) {
        camera.takePicture(shutter, raw, postview, jpeg);
    }

    public void setDisplayOrientation(int degrees) {
        this.degrees = degrees;
        camera.setDisplayOrientation(degrees);
        jpegRotation = CameraHelper.getJpegRotation(info, degrees);
    }

    public int getDisplayOrientation() {
        return degrees;
    }

    public int getJpegRotation() {
        return jpegRotation;
    }

    public boolean isFront() {
        return face == CameraFace.Front;
    }

    @Override
    public String toString() {
        return "CameraProxy{" +
                "id=" + id +
                ", face=" + face +
                ", autoFocusSupported=" + autoFocusSupported +
                '}';
    }

}
