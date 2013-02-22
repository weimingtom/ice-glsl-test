package com.ice.test.light;

import com.ice.graphics.geometry.GeometryData;
import com.ice.graphics.geometry.VBOGeometry;
import com.ice.graphics.shader.FragmentShader;
import com.ice.graphics.shader.VertexShader;
import com.ice.model.light.DirectionalLight;
import com.ice.model.light.Material;

/**
 * User: jason
 * Date: 13-2-22
 */
public class DirectionalLightBinder extends VBOGeometry.EasyBinder {

    private Material material;
    private DirectionalLight directionalLight;

    public DirectionalLightBinder(VBOGeometry vboGeometry) {
        super(vboGeometry.getGeometryData().getFormatDescriptor());

        material = Material.createNormal();
        directionalLight = new DirectionalLight();
    }

    @Override
    public void bind(GeometryData data, VertexShader vsh, FragmentShader fsh) {

    }

    @Override
    public void unbind(GeometryData data, VertexShader vsh, FragmentShader fsh) {

    }

}
