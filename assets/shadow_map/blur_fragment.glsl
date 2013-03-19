/// <summary>
/// Fragment shader for performing a seperable blur on the specified texture.
/// </summary>


#ifdef GL_ES
    precision highp float;
#endif


uniform vec2 TexelSize;
uniform sampler2D Sample0;

uniform int Orientation;
uniform int BlurAmount;


/// <summary>
/// Varying variables.
/// <summary>
varying vec2 vUv;


/// <summary>
/// Gets the Gaussian value in the first dimension.
/// </summary>
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
    vec4 colour;
    
    if ( Orientation == 0 )
    {
        // Blur horizontal
        for (int i = 0; i < 10; ++i)
        {
            if ( i >= BlurAmount )
                break;
            
            float offset = float(i) - halfBlur;
            colour += texture2D(Sample0, vUv + vec2(offset * TexelSize.x, 0.0)) /* Gaussian(offset, deviation)*/;
        }
    }
    else
    {
        // Blur vertical
        for (int i = 0; i < 10; ++i)
        {
            if ( i >= BlurAmount )
                break;
            
            float offset = float(i) - halfBlur;
            colour += texture2D(Sample0, vUv + vec2(0.0, offset * TexelSize.y)) /* Gaussian(offset, deviation)*/;
        }
    }
    
    // Calculate average
    colour = colour / float(BlurAmount);
    
    // Apply colour
    gl_FragColor = colour;
}