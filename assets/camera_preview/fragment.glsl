#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform samplerExternalOES u_Texture;

varying vec2 v_TexCoordinate;
  
// The entry point for our fragment shader.
void main()                    		
{                              
    gl_FragColor = texture2D(u_Texture, v_TexCoordinate);
}

