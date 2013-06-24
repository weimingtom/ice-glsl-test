package com.ice.test.camera_test;

import android.hardware.Camera;

/**
 * User: jason
 */
public class Cameras {

    private static Cameras instance = new Cameras();

    public static Cameras getInstance() {
        return instance;
    }

    private int backCameraId = -1;
    private Camera.CameraInfo backCameraInfo;

    private int frontCameraId = -1;
    private Camera.CameraInfo frontCameraInfo;

    private Cameras() {
        int mNumberOfCameras = Camera.getNumberOfCameras();

        System.out.println("mNumberOfCameras = " + mNumberOfCameras);

        Camera.CameraInfo[] mInfo = new Camera.CameraInfo[mNumberOfCameras];
        for (int i = 0; i < mNumberOfCameras; i++) {
            mInfo[i] = new Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(i, mInfo[i]);
        }

        // get the first (smallest) back and first front camera id
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.CameraInfo cameraInfo = mInfo[i];

            if (backCameraId == -1 && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backCameraId = i;
                backCameraInfo = cameraInfo;
            } else if (frontCameraId == -1 && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i;
                frontCameraInfo = cameraInfo;
            }
        }
    }

    public int getCameraId(CameraFace face) {
        if (face == null) throw new IllegalArgumentException();

        return face == CameraFace.Back ? backCameraId : frontCameraId;
    }

    public Camera.CameraInfo getCameraInfo(CameraFace face) {
        if (face == null) throw new IllegalArgumentException();
        return face == CameraFace.Back ? backCameraInfo : frontCameraInfo;
    }

}
