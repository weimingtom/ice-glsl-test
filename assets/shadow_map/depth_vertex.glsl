#version 120

uniform mat4 u_LightMVPMatrix;
uniform mat4 u_ModelMatrix;

attribute vec3 a_Position;

void main()
{

    gl_Position = u_LightMVPMatrix *(u_ModelMatrix* vec4(a_Position,1.0));

}
