#version 120

uniform mat4 u_MVPMatrix;
uniform mat4 u_MVMatrix;

attribute vec3 a_Position;
attribute vec3 a_Normal;
attribute vec2 a_Texture;

varying vec3 v_Normal;
varying vec2 v_TexCoordinate;

void main()
{
    v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

    v_TexCoordinate= a_Texture;

    gl_Position = u_MVPMatrix * vec4(a_Position,1.0);
}
