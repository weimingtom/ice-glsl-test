#version 120

uniform mat4 projectionMatrix;
uniform float test;

attribute vec3 vertex;
attribute vec3 vertexColor;

varying vec3 color;

void main(void)
{
    gl_Position = projectionMatrix*vec4(vertex,1.0);
    color = vertexColor;
}