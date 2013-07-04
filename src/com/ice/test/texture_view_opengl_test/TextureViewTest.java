package com.ice.test.texture_view_opengl_test;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Display;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.ice.test.R;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: jason
 */
public class TextureViewTest extends Activity implements TextureView.SurfaceTextureListener {

    private Thread mProducerThread;
    private GLRendererImpl mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.texture_view_test);

        Point screenSize = new Point();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display defaultDisplay = windowManager.getDefaultDisplay();
        defaultDisplay.getSize(screenSize);

        TextureView textureView = (TextureView) findViewById(R.id.texture_view);
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        layoutParams.width = screenSize.x / 2;
        layoutParams.height = screenSize.y / 2;
        textureView.setLayoutParams(layoutParams);

        textureView.setSurfaceTextureListener(this);

        mRenderer = new GLRendererImpl(this);
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mRenderer.setViewport(width, height);
        mProducerThread = new GLProducerThread(surface, mRenderer, new AtomicBoolean(true));
        mProducerThread.start();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mProducerThread = null;
        return true;
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mRenderer.resize(width, height);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

}
