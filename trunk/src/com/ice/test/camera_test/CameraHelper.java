package com.ice.test.camera_test;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import java.util.List;

import static android.hardware.Camera.CameraInfo;

/**
 * User: jason
 */
public class CameraHelper {

    // Fields from android.hardware.Camera.Parameters
    private static final String AUTO_EXPOSURE_LOCK_SUPPORTED = "auto-exposure-lock-supported";
    private static final String AUTO_WHITE_BALANCE_LOCK_SUPPORTED = "auto-whitebalance-lock-supported";
    private static final String VIDEO_SNAPSHOT_SUPPORTED = "video-snapshot-supported";
    public static final String SCENE_MODE_HDR = "hdr";
    public static final String TRUE = "true";
    private static final String TAG = "CameraHelper";

    public static int getJpegRotation(CameraInfo info, int displayOrientation) {
        // See android.hardware.Camera.Parameters.setRotation for
        // documentation.
        int rotation = 0;
        if (displayOrientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - displayOrientation + 360) % 360;
            } else {  // back-facing camera
                rotation = (info.orientation + displayOrientation) % 360;
            }
        }
        return rotation;
    }

    public static int rightDisplayOrientation(Context context, CameraInfo info) {

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        int rotation = windowManager.getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    public static void prepareJpgMatrix(Matrix matrix, boolean mirror, int displayOrientation,
                                        int viewWidth, int viewHeight) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation,
                                     int viewWidth, int viewHeight) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    public static Camera.Size getOptimalPreviewSize(Context context,
                                                    List<Camera.Size> sizes, double targetRatio) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.001;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        Display defaultDisplay = windowManager.getDefaultDisplay();

        Point point = new Point();
        defaultDisplay.getSize(point);

        int targetHeight = Math.min(point.x, point.y);
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            Log.w(TAG, "No preview size match the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * Check if this device has a camera
     */
    public static boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public static void setCameraDisplayOrientation(Context context, int cameraId, android.hardware.Camera camera) {
        CameraInfo info = new CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public static void init(Context context, Camera camera, double targetRatio) {

        Camera.Parameters parameters = camera.getParameters();

        List<String> supportedFocusModes = parameters.getSupportedFocusModes();

        boolean autoFocusSupported = isSupported(Camera.Parameters.FOCUS_MODE_AUTO, supportedFocusModes);

        if (autoFocusSupported) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        Log.d(TAG, "auto focus enabled ? " + autoFocusSupported);

        camera.setParameters(parameters);
    }

    public static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    public static boolean isAutoExposureLockSupported(Camera.Parameters params) {
        return TRUE.equals(params.get(AUTO_EXPOSURE_LOCK_SUPPORTED));
    }

    public static boolean isAutoWhiteBalanceLockSupported(Camera.Parameters params) {
        return TRUE.equals(params.get(AUTO_WHITE_BALANCE_LOCK_SUPPORTED));
    }

    public static boolean isCameraHdrSupported(Camera.Parameters params) {
        List<String> supported = params.getSupportedSceneModes();
        return (supported != null) && supported.contains(SCENE_MODE_HDR);
    }

    public static boolean isMeteringAreaSupported(Camera.Parameters params) {
        return params.getMaxNumMeteringAreas() > 0;
    }

    public static boolean isFocusAreaSupported(Camera.Parameters params) {
        return (params.getMaxNumFocusAreas() > 0
                && isSupported(Camera.Parameters.FOCUS_MODE_AUTO,
                params.getSupportedFocusModes()));
    }

}
