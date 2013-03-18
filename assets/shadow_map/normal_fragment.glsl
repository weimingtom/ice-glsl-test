precision mediump float;

uniform vec3 u_LightVector;
uniform sampler2D u_Texture;
  
varying vec3 v_Normal;
varying vec2 v_TexCoordinate;
  
void main()
{                              

   vec3 normalLightDir= normalize(u_LightVector);

    float diffuse = max(dot(v_Normal, normalLightDir),0.0);

    diffuse = diffuse + 0.1;

    gl_FragColor = diffuse * texture2D(u_Texture, v_TexCoordinate);
}                                                                     	

