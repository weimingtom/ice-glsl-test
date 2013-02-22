package com.ice.test;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.RelativeLayout;
import com.ice.common.GlslSurfaceView;
import com.ice.test.light.diffuse_lighting.PerFragmentLighting;

/**
 * User: jason
 * Date: 13-2-22
 */
public class WorkingWithNormalWidget extends Activity {

    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        glSurfaceView = new GlslSurfaceView(this);
        glSurfaceView.setRenderer(new PerFragmentLighting.Renderer(this));

        RelativeLayout rootView = (RelativeLayout) findViewById(R.id.root);

        rootView.addView(glSurfaceView, 0);
    }

    @Override
    protected void onResume() {
        glSurfaceView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        glSurfaceView.onPause();
        super.onPause();
    }

}
