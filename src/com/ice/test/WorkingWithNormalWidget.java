package com.ice.test;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.RelativeLayout;
import com.ice.engine.GlslSurfaceView;
import com.ice.engine.Res;
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

        Res.build(this);

        setContentView(R.layout.main);

        glSurfaceView = new GlslSurfaceView(this);
        glSurfaceView.setRenderer(new PerFragmentLighting.Renderer(this));

        RelativeLayout rootView = (RelativeLayout) findViewById(R.id.root);

        rootView.addView(glSurfaceView);
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

    @Override
    protected void onDestroy() {
        Res.release();
        super.onDestroy();
    }

}
