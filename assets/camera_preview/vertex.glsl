#version 120

uniform mat4 u_MVPMatrix;
uniform vec4 u_TextureCrop;

attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;

varying vec2 v_TexCoordinate;

void main()
{
    float croppedS=u_TextureCrop.x+ a_TexCoordinate.x*(u_TextureCrop.y-u_TextureCrop.x) ;
    float croppedT=u_TextureCrop.z+ a_TexCoordinate.y*(u_TextureCrop.w-u_TextureCrop.z) ;

    v_TexCoordinate= vec2(croppedS,croppedT);
    gl_Position = u_MVPMatrix * a_Position;
}
