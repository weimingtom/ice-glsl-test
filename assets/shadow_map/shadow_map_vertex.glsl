#version 120

uniform mat4 u_MMatrix;
uniform mat4 u_MVMatrix;
uniform mat4 u_MVPMatrix;
uniform mat4 u_LightMVPMatrix;

attribute vec3 a_Position;
attribute vec3 a_Normal;
attribute vec2 a_Texture;

varying vec3 v_Normal;
varying vec2 v_TexCoordinate;
varying vec4 v_PositionInLightSpace;

/// <summary>
/// The scale matrix is used to push the projected vertex into the 0.0 - 1.0 region.
/// Similar in role to a * 0.5 + 0.5, where -1.0 < a < 1.0.
/// <summary>
const mat4 ScaleMatrix = mat4(
0.5, 0.0, 0.0, 0.0,
0.0, 0.5, 0.0, 0.0,
0.0, 0.0, 0.5, 0.0,
0.5, 0.5, 0.5, 1.0
);


void main()
{
    v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

    v_TexCoordinate= a_Texture;

    //v_PositionInLightSpace=u_MVPMatrix * vec4(a_Position,1.0);
    v_PositionInLightSpace=ScaleMatrix *u_LightMVPMatrix*(u_MMatrix *vec4(a_Position,1.0));

    gl_Position = u_MVPMatrix * vec4(a_Position,1.0);
}
