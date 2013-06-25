#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform samplerExternalOES u_Texture;
uniform vec2 u_TextureScale;

varying vec2 v_TexCoordinate;
  
// The entry point for our fragment shader.
void main()                    		
{
     vec2 coord=vec2(v_TexCoordinate.x*u_TextureScale.x,v_TexCoordinate.y*u_TextureScale.y);
     gl_FragColor = texture2D(u_Texture, coord);
}

