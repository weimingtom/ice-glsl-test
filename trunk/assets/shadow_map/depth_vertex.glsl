#version 120

uniform mat4 u_LightMVPMatrix;
uniform mat4 u_ModelMatrix;

attribute vec4 a_Position;

void main()
{

    gl_Position = u_MVPMatrix *(u_ModelMatrix* vect4(a_Position,1.0));

}
