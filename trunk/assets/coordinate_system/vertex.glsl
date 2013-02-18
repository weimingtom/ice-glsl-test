#version 120

attribute vec4 aPosition;
uniform mat4 aModelViewProjectMatrix;

attribute vec4 aColor;
varying vec4 vColor;

void main() {
        gl_Position = aModelViewProjectMatrix*aPosition;
        vColor=aColor;
}