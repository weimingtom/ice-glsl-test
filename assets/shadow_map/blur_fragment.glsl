/// Fragment shader for performing a seperable blur on the specified texture.

precision highp float;

const float blurSize = 1.0/768.0;

uniform vec2 TexelSize;
uniform sampler2D Sample0;

uniform int Orientation;
uniform int BlurAmount;

varying vec2 vTexCoord;

void main ()
{
   vec4 sum = vec4(0.0);

      // blur in y (vertical)
      // take nine samples, with the distance blurSize between them
      sum += texture2D(Sample0, vec2(vTexCoord.x - 4.0*blurSize, vTexCoord.y)) * 0.05;
      sum += texture2D(Sample0, vec2(vTexCoord.x - 3.0*blurSize, vTexCoord.y)) * 0.09;
      sum += texture2D(Sample0, vec2(vTexCoord.x - 2.0*blurSize, vTexCoord.y)) * 0.12;
      sum += texture2D(Sample0, vec2(vTexCoord.x - blurSize, vTexCoord.y)) * 0.15;
      sum += texture2D(Sample0, vec2(vTexCoord.x, vTexCoord.y)) * 0.16;
      sum += texture2D(Sample0, vec2(vTexCoord.x + blurSize, vTexCoord.y)) * 0.15;
      sum += texture2D(Sample0, vec2(vTexCoord.x + 2.0*blurSize, vTexCoord.y)) * 0.12;
      sum += texture2D(Sample0, vec2(vTexCoord.x + 3.0*blurSize, vTexCoord.y)) * 0.09;
      sum += texture2D(Sample0, vec2(vTexCoord.x + 4.0*blurSize, vTexCoord.y)) * 0.05;

      gl_FragColor = sum;
}