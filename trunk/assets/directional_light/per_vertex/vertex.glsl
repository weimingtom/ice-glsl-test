#version 120
# include <light/directional_light.glsl>

uniform mat4 u_MVPMatrix;
uniform mat4 u_MVMatrix;
uniform vec3 u_LightPos;

attribute vec4 a_Position;
attribute vec4 a_Color;
attribute vec3 a_Normal;

varying vec4 v_Color;

void main(){

  vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

  v_Color = a_Color * directional_light(modelViewNormal);

  gl_Position = u_MVPMatrix * a_Position;
}

