/// Vertex shader for performing a seperable blur on the specified texture.

attribute vec3 a_Position;
attribute vec2 a_Texture;

uniform mat4 u_MVPMatrix;

varying vec2 vTexCoord;

void main ()
{
    vTexCoord = a_Texture;
    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
}