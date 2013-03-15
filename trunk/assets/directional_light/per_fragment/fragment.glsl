precision mediump float;

uniform vec3 u_LightVector;
uniform sampler2D u_Texture;
  
varying vec3 v_Normal;
varying vec2 v_TexCoordinate;
  
// The entry point for our fragment shader.
void main()                    		
{                              

   vec3 normalLightDir= normalize(u_LightVector);

	// Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
	// pointing in the same direction then it will get max illumination.
    float diffuse = max(dot(v_Normal, normalLightDir),0.0);

    // Add ambient lighting
    diffuse = diffuse + 0.2;

	// Multiply the color by the diffuse illumination level and texture value to get final output color.
    gl_FragColor = diffuse * texture2D(u_Texture, v_TexCoordinate);
}                                                                     	

