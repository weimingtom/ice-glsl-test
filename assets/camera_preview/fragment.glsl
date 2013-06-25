#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform samplerExternalOES u_Texture;

uniform mat4 u_TextureMatrix;
uniform vec2 u_TextureScale;

varying vec2 v_TexCoordinate;
  
// The entry point for our fragment shader.
void main()                    		
{
    //vec4 texCoord=u_TextureMatrix * vec4(v_TexCoordinate,1,1);
    //vec2 coord=vec2(texCoord.x,texCoord.y);
    //gl_FragColor = texture2D(u_Texture, coord);

     vec2 coord=vec2(v_TexCoordinate.x*u_TextureScale.x,v_TexCoordinate.y*u_TextureScale.y);
     gl_FragColor = texture2D(u_Texture, coord);
}

