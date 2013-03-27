package com.ice.test;

import com.ice.graphics.shader.FragmentShader;
import com.ice.graphics.shader.Program;
import com.ice.graphics.shader.VertexShader;

import static com.ice.engine.Res.assetSting;

/**
 * User: Jason
 * Date: 13-3-23
 */
public class Util {

    public static Program assetProgram(String vertexAsset, String fragmentAsset) {
        VertexShader vsh = new VertexShader(assetSting(vertexAsset));
        FragmentShader fsh = new FragmentShader(assetSting(fragmentAsset));

        Program program = new Program();
        program.attachShader(vsh, fsh);
        program.link();

        return program;
    }

}
