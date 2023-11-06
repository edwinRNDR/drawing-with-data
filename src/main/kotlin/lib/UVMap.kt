package lib

import org.openrndr.draw.Filter2to1
import org.openrndr.draw.filterShaderFromCode

class UVMap: Filter2to1(filterShaderFromCode("""
uniform sampler2D tex0; // image
uniform sampler2D tex1; // uvmap
uniform float uShift;
uniform float vShift;
in vec2 v_texCoord0;
out vec4 o_color;
void main() {
    vec4 uv = texture(tex1, v_texCoord0);
    vec2 muv = mod(uv.xy + vec2(uShift, vShift), vec2(1.0));
    muv.y = 1.0 - muv.y;
    
    o_color = texture(tex0, muv.xy) * uv.a; 
}
    
    
""".trimIndent(),"uvmap")) {

    var uShift: Double by parameters
    var vShift: Double by parameters
}