#version 120

uniform mat4 u_MVPMatrix;
uniform mat4 u_MVMatrix;

attribute vec4 a_Position;
attribute vec3 a_Normal;
attribute vec2 a_Texture;

varying vec3 v_Normal;
varying vec2 v_TexCoordinate;

// The entry point for our vertex shader.
void main()
{
    v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

    v_TexCoordinate= a_Texture;

    // gl_Position is a special variable used to store the final position.
    // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
    gl_Position = u_MVPMatrix * a_Position;
}
