precision mediump float;

uniform vec3 u_LightVector;
uniform sampler2D u_Texture;
uniform sampler2D u_DepthMap;
  
varying vec3 v_Normal;
varying vec2 v_TexCoordinate;
varying vec4 v_PositionInLightSpace;
  
void main()
{                              

  vec3 depth = v_PositionInLightSpace.xyz/v_PositionInLightSpace.w;

  vec3 normalLightDir= normalize(u_LightVector);

  float diffuse = max(dot(v_Normal, normalLightDir),0.0);

  diffuse = diffuse + 0.1;

  //gl_FragColor = diffuse * texture2D(u_DepthMap, v_TexCoordinate);
  gl_FragColor = diffuse * texture2D(u_Texture, v_TexCoordinate);

  if(depth.z> texture2D(u_DepthMap, depth.xy)){
        gl_FragColor=0.5*gl_FragColor;
  }

}                                                                     	

