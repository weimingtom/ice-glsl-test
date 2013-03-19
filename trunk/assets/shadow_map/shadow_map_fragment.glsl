precision mediump float;

uniform vec3 u_LightVector;
uniform sampler2D u_Texture;
uniform sampler2D u_DepthMap;
  
varying vec3 v_Normal;
varying vec2 v_TexCoordinate;
varying vec4 v_PositionInLightSpace;

const vec4 bitShifts = vec4(1.0,1.0 / 255.0,1.0 / (255.0 * 255.0),1.0 / (255.0 * 255.0 * 255.0));

void main()
{                              

  vec3 normalLightDir= normalize(u_LightVector);

  float diffuse = max(dot(v_Normal, normalLightDir),0.0);

  diffuse = diffuse + 0.1;

  gl_FragColor = diffuse * texture2D(u_Texture, v_TexCoordinate);

  vec3 depth = v_PositionInLightSpace.xyz/v_PositionInLightSpace.w;

  // Exponential shadow map algorithm
  float c = 4.0;
  vec4 texel = texture2D(u_DepthMap, depth.xy);
  float z=dot(texel, bitShifts);
  float shadow = clamp(exp(-c * (depth.z - z)), 0.0, 1.0);

  // Apply colour and shadow
  gl_FragColor = clamp(gl_FragColor * shadow, 0.0, 1.0);

}                                                                     	

