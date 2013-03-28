precision highp float;

uniform vec2 TexelSize;
uniform sampler2D sampler;

uniform int Orientation;
const int BlurAmount=5;

varying vec2 v_Texture;

/// Gets the Gaussian value in the first dimension.
/// <param name="x">Distance from origin on the x-axis.</param>
/// <param name="deviation">Standard deviation.</param>
/// <returns>The gaussian value on the x-axis.</returns>
float Gaussian (float x, float deviation)
{
    return (1.0 / sqrt(2.0 * 3.141592 * deviation)) * exp(-((x * x) / (2.0 * deviation)));
}

void main ()
{
    float halfBlur = float(BlurAmount) * 0.5;
    //float deviation = halfBlur * 0.5;
    vec4 color;

    if ( Orientation == 0 ){// Blur horizontal
        for (int i = 0; i < BlurAmount; ++i){
            float offset = float(i) - halfBlur;
            color += texture2D(sampler, v_Texture + vec2(offset/TexelSize.x, 0.0));
            /* Gaussian(offset, deviation)*/
        }
    }
    else{// Blur vertical
        for (int i = 0; i < BlurAmount; ++i){
            float offset = float(i) - halfBlur;
            color += texture2D(sampler, v_Texture + vec2(0.0, offset/TexelSize.y));
            /* Gaussian(offset, deviation)*/
        }
    }

    gl_FragColor = color / float(BlurAmount);
}
