package com.skintracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skintracker.R
import com.skintracker.data.BodyZone
import com.skintracker.data.nextSeverity
import com.skintracker.ui.theme.KetoTheme

// ── Severity overlay colours ──────────────────────────────────────────────────
private val SEV_MILD     = Color(0xFFFFE566)
private val SEV_MODERATE = Color(0xFFFF9933)
private val SEV_SEVERE   = Color(0xFFFF3838)
private const val OVERLAY_ALPHA = 0.55f

private fun Int.toFillColor() = when (this) {
    1    -> SEV_MILD
    2    -> SEV_MODERATE
    else -> SEV_SEVERE
}

private fun Int.toTextColor() = when (this) {
    1    -> Color(0xFFA07800)
    2    -> Color(0xFFB85000)
    else -> Color(0xFFBB1111)
}

// ── Zone polygons ─────────────────────────────────────────────────────────────
// Coordinates are normalised (0.0–1.0) to match body_map.png.
// Back view occupies the LEFT half (x ≈ 0.11–0.48); front is the RIGHT half (x ≈ 0.53–0.88).
private data class ZonePoly(val zone: BodyZone, val pts: List<Pair<Float, Float>>)

private val ZONE_POLYGONS: List<ZonePoly> = listOf(
    // ── Front ─────────────────────────────────────────────────────────────────
    ZonePoly(BodyZone.HEAD_FACE, listOf(
        0.725f to 0.061f, 0.714f to 0.060f, 0.703f to 0.058f, 0.694f to 0.058f,
        0.687f to 0.060f, 0.677f to 0.063f, 0.666f to 0.066f, 0.663f to 0.069f,
        0.658f to 0.075f, 0.650f to 0.083f, 0.648f to 0.092f, 0.645f to 0.101f,
        0.645f to 0.108f, 0.641f to 0.125f, 0.641f to 0.127f, 0.642f to 0.137f,
        0.645f to 0.146f, 0.648f to 0.152f, 0.660f to 0.167f, 0.662f to 0.177f,
        0.662f to 0.183f, 0.664f to 0.188f, 0.674f to 0.190f, 0.684f to 0.192f,
        0.694f to 0.192f, 0.705f to 0.193f, 0.716f to 0.193f, 0.726f to 0.192f,
        0.737f to 0.191f, 0.744f to 0.189f, 0.744f to 0.186f, 0.745f to 0.182f,
        0.748f to 0.167f, 0.758f to 0.153f, 0.763f to 0.147f, 0.766f to 0.138f,
        0.767f to 0.132f, 0.767f to 0.126f, 0.764f to 0.107f, 0.765f to 0.098f,
        0.762f to 0.090f, 0.761f to 0.088f, 0.752f to 0.078f,
    )),
    ZonePoly(BodyZone.NECK, listOf(
        0.663f to 0.187f, 0.664f to 0.196f, 0.598f to 0.224f, 0.688f to 0.233f,
        0.703f to 0.243f, 0.719f to 0.234f, 0.807f to 0.225f, 0.745f to 0.197f,
        0.744f to 0.189f, 0.704f to 0.194f, 0.666f to 0.188f,
    )),
    ZonePoly(BodyZone.R_SHOULDER, listOf(
        0.597f to 0.225f, 0.587f to 0.228f, 0.577f to 0.232f, 0.569f to 0.235f,
        0.560f to 0.241f, 0.554f to 0.249f, 0.549f to 0.258f, 0.545f to 0.267f,
        0.543f to 0.277f, 0.543f to 0.287f, 0.543f to 0.298f, 0.544f to 0.308f,
        0.544f to 0.312f, 0.589f to 0.284f, 0.633f to 0.229f,
    )),
    ZonePoly(BodyZone.L_SHOULDER, listOf(
        0.810f to 0.225f, 0.820f to 0.228f, 0.829f to 0.231f, 0.838f to 0.235f,
        0.846f to 0.240f, 0.852f to 0.249f, 0.856f to 0.259f, 0.859f to 0.268f,
        0.860f to 0.278f, 0.861f to 0.288f, 0.861f to 0.298f, 0.861f to 0.308f,
        0.860f to 0.313f, 0.817f to 0.283f, 0.777f to 0.229f,
    )),
    ZonePoly(BodyZone.R_UPPER_ARM, listOf(
        0.590f to 0.284f, 0.598f to 0.351f, 0.588f to 0.386f, 0.566f to 0.391f,
        0.537f to 0.356f, 0.538f to 0.334f, 0.545f to 0.312f,
    )),
    ZonePoly(BodyZone.L_UPPER_ARM, listOf(
        0.818f to 0.284f, 0.862f to 0.314f, 0.870f to 0.337f, 0.869f to 0.357f,
        0.839f to 0.391f, 0.816f to 0.385f, 0.807f to 0.351f, 0.815f to 0.301f,
    )),
    ZonePoly(BodyZone.R_FOREARM, listOf(
        0.538f to 0.357f, 0.538f to 0.373f, 0.525f to 0.406f, 0.527f to 0.504f,
        0.558f to 0.498f, 0.563f to 0.476f, 0.588f to 0.421f, 0.590f to 0.386f,
        0.565f to 0.392f,
    )),
    ZonePoly(BodyZone.L_FOREARM, listOf(
        0.818f to 0.386f, 0.818f to 0.417f, 0.844f to 0.479f, 0.846f to 0.497f,
        0.878f to 0.499f, 0.881f to 0.409f, 0.870f to 0.359f, 0.840f to 0.392f,
    )),
    ZonePoly(BodyZone.R_HAND, listOf(
        0.529f to 0.505f, 0.528f to 0.540f, 0.531f to 0.550f, 0.535f to 0.560f,
        0.548f to 0.571f, 0.561f to 0.577f, 0.566f to 0.580f, 0.569f to 0.579f,
        0.570f to 0.572f, 0.576f to 0.546f, 0.575f to 0.535f, 0.572f to 0.526f,
        0.572f to 0.516f, 0.564f to 0.504f, 0.559f to 0.499f, 0.538f to 0.503f,
    )),
    ZonePoly(BodyZone.L_HAND, listOf(
        0.878f to 0.501f, 0.878f to 0.506f, 0.877f to 0.524f, 0.879f to 0.540f,
        0.872f to 0.554f, 0.869f to 0.560f, 0.860f to 0.568f, 0.853f to 0.572f,
        0.843f to 0.578f, 0.838f to 0.579f, 0.836f to 0.575f, 0.834f to 0.572f,
        0.829f to 0.546f, 0.830f to 0.538f, 0.836f to 0.523f, 0.834f to 0.515f,
        0.843f to 0.503f, 0.848f to 0.497f, 0.863f to 0.500f,
    )),
    ZonePoly(BodyZone.CHEST, listOf(
        0.704f to 0.244f, 0.721f to 0.235f, 0.776f to 0.229f, 0.818f to 0.284f,
        0.805f to 0.350f, 0.779f to 0.333f, 0.718f to 0.320f, 0.703f to 0.309f,
        0.688f to 0.320f, 0.627f to 0.330f, 0.598f to 0.347f, 0.591f to 0.283f,
        0.634f to 0.228f, 0.688f to 0.234f, 0.702f to 0.243f,
    )),
    ZonePoly(BodyZone.BELLY, listOf(
        0.719f to 0.321f, 0.778f to 0.333f, 0.806f to 0.352f, 0.794f to 0.391f,
        0.800f to 0.451f, 0.776f to 0.461f, 0.721f to 0.528f, 0.702f to 0.533f,
        0.681f to 0.526f, 0.631f to 0.461f, 0.605f to 0.451f, 0.611f to 0.390f,
        0.599f to 0.348f, 0.629f to 0.330f, 0.691f to 0.319f, 0.704f to 0.309f,
        0.715f to 0.318f,
    )),
    ZonePoly(BodyZone.R_THIGH, listOf(
        0.605f to 0.451f, 0.631f to 0.461f, 0.676f to 0.523f, 0.697f to 0.534f,
        0.699f to 0.560f, 0.683f to 0.689f, 0.643f to 0.710f, 0.623f to 0.703f,
        0.611f to 0.692f, 0.608f to 0.647f, 0.589f to 0.583f, 0.586f to 0.534f,
        0.604f to 0.455f,
    )),
    ZonePoly(BodyZone.L_THIGH, listOf(
        0.709f to 0.532f, 0.727f to 0.523f, 0.776f to 0.462f, 0.800f to 0.452f,
        0.820f to 0.547f, 0.813f to 0.606f, 0.795f to 0.651f, 0.792f to 0.693f,
        0.762f to 0.711f, 0.730f to 0.692f, 0.720f to 0.678f, 0.709f to 0.535f,
    )),
    ZonePoly(BodyZone.R_CALF, listOf(
        0.624f to 0.704f, 0.644f to 0.710f, 0.683f to 0.690f, 0.680f to 0.711f,
        0.687f to 0.751f, 0.669f to 0.861f, 0.676f to 0.896f, 0.640f to 0.895f,
        0.629f to 0.904f, 0.634f to 0.884f, 0.631f to 0.859f, 0.605f to 0.749f,
        0.614f to 0.694f, 0.622f to 0.702f,
    )),
    ZonePoly(BodyZone.L_CALF, listOf(
        0.723f to 0.682f, 0.729f to 0.692f, 0.764f to 0.711f, 0.793f to 0.694f,
        0.801f to 0.741f, 0.770f to 0.885f, 0.774f to 0.905f, 0.764f to 0.895f,
        0.728f to 0.896f, 0.734f to 0.873f, 0.731f to 0.821f, 0.720f to 0.750f,
        0.727f to 0.712f, 0.723f to 0.685f,
    )),
    ZonePoly(BodyZone.R_FOOT, listOf(
        0.630f to 0.906f, 0.639f to 0.896f, 0.677f to 0.897f, 0.672f to 0.912f,
        0.676f to 0.931f, 0.667f to 0.937f, 0.663f to 0.951f, 0.652f to 0.959f,
        0.620f to 0.958f, 0.593f to 0.948f, 0.598f to 0.937f, 0.629f to 0.908f,
    )),
    ZonePoly(BodyZone.L_FOOT, listOf(
        0.728f to 0.896f, 0.766f to 0.896f, 0.782f to 0.920f, 0.805f to 0.938f,
        0.810f to 0.949f, 0.782f to 0.960f, 0.760f to 0.960f, 0.745f to 0.954f,
        0.738f to 0.937f, 0.728f to 0.930f, 0.735f to 0.910f, 0.729f to 0.898f,
    )),
    // ── Back ──────────────────────────────────────────────────────────────────
    ZonePoly(BodyZone.BACK_HEAD, listOf(
        0.252f to 0.173f, 0.252f to 0.171f, 0.260f to 0.169f, 0.271f to 0.168f,
        0.281f to 0.167f, 0.291f to 0.167f, 0.301f to 0.167f, 0.312f to 0.167f,
        0.322f to 0.168f, 0.332f to 0.170f, 0.337f to 0.172f, 0.338f to 0.170f,
        0.337f to 0.175f, 0.339f to 0.168f, 0.349f to 0.153f, 0.353f to 0.149f,
        0.354f to 0.149f, 0.357f to 0.139f, 0.359f to 0.133f, 0.359f to 0.129f,
        0.355f to 0.108f, 0.355f to 0.105f, 0.354f to 0.099f, 0.351f to 0.089f,
        0.348f to 0.081f, 0.347f to 0.079f, 0.340f to 0.071f, 0.335f to 0.068f,
        0.327f to 0.064f, 0.317f to 0.061f, 0.308f to 0.059f, 0.297f to 0.059f,
        0.287f to 0.060f, 0.277f to 0.061f, 0.267f to 0.063f, 0.260f to 0.066f,
        0.254f to 0.070f, 0.251f to 0.072f, 0.242f to 0.085f, 0.237f to 0.090f,
        0.234f to 0.098f, 0.235f to 0.107f, 0.232f to 0.123f, 0.230f to 0.128f,
        0.231f to 0.136f, 0.235f to 0.147f,
    )),
    ZonePoly(BodyZone.BACK_NECK, listOf(
        0.253f to 0.171f, 0.303f to 0.168f, 0.333f to 0.171f, 0.336f to 0.197f,
        0.253f to 0.195f,
    )),
    ZonePoly(BodyZone.UPPER_BACK, listOf(
        0.338f to 0.198f, 0.402f to 0.228f, 0.357f to 0.241f, 0.414f to 0.281f,
        0.407f to 0.316f, 0.339f to 0.313f, 0.292f to 0.367f, 0.247f to 0.313f,
        0.177f to 0.315f, 0.172f to 0.279f, 0.230f to 0.238f, 0.185f to 0.225f,
        0.253f to 0.195f, 0.321f to 0.198f,
    )),
    ZonePoly(BodyZone.LOWER_BACK, listOf(
        0.293f to 0.369f, 0.341f to 0.314f, 0.408f to 0.317f, 0.387f to 0.392f,
        0.391f to 0.446f, 0.347f to 0.438f, 0.294f to 0.474f, 0.238f to 0.438f,
        0.196f to 0.445f, 0.199f to 0.390f, 0.176f to 0.315f, 0.227f to 0.315f,
        0.246f to 0.314f, 0.265f to 0.335f, 0.288f to 0.363f,
    )),
    ZonePoly(BodyZone.GLUTES, listOf(
        0.194f to 0.447f, 0.238f to 0.440f, 0.295f to 0.476f, 0.348f to 0.440f,
        0.392f to 0.449f, 0.407f to 0.505f, 0.409f to 0.520f, 0.351f to 0.542f,
        0.336f to 0.544f, 0.287f to 0.541f, 0.249f to 0.542f, 0.232f to 0.541f,
        0.203f to 0.527f, 0.177f to 0.517f, 0.184f to 0.481f, 0.192f to 0.456f,
    )),
    ZonePoly(BodyZone.R_UPPER_ARM_BACK, listOf(
        0.402f to 0.228f, 0.441f to 0.243f, 0.455f to 0.268f, 0.455f to 0.309f,
        0.462f to 0.335f, 0.464f to 0.374f, 0.475f to 0.405f, 0.409f to 0.407f,
        0.410f to 0.387f, 0.398f to 0.351f, 0.416f to 0.280f, 0.360f to 0.240f,
        0.399f to 0.228f,
    )),
    ZonePoly(BodyZone.L_UPPER_ARM_BACK, listOf(
        0.172f to 0.279f, 0.174f to 0.314f, 0.186f to 0.353f, 0.174f to 0.386f,
        0.173f to 0.406f, 0.110f to 0.405f, 0.121f to 0.372f, 0.121f to 0.332f,
        0.131f to 0.310f, 0.132f to 0.267f, 0.145f to 0.242f, 0.184f to 0.226f,
        0.227f to 0.239f, 0.173f to 0.278f,
    )),
    ZonePoly(BodyZone.R_FOREARM_BACK, listOf(
        0.410f to 0.408f, 0.476f to 0.405f, 0.477f to 0.412f, 0.472f to 0.502f,
        0.468f to 0.508f, 0.439f to 0.498f, 0.436f to 0.479f, 0.412f to 0.419f,
    )),
    ZonePoly(BodyZone.L_FOREARM_BACK, listOf(
        0.110f to 0.405f, 0.175f to 0.407f, 0.174f to 0.419f, 0.166f to 0.439f,
        0.149f to 0.475f, 0.146f to 0.481f, 0.143f to 0.500f, 0.111f to 0.501f,
        0.111f to 0.483f, 0.110f to 0.453f, 0.110f to 0.420f,
    )),
    ZonePoly(BodyZone.R_THIGH_BACK, listOf(
        0.300f to 0.543f, 0.352f to 0.544f, 0.409f to 0.521f, 0.412f to 0.545f,
        0.410f to 0.580f, 0.397f to 0.628f, 0.387f to 0.651f, 0.384f to 0.695f,
        0.317f to 0.714f, 0.307f to 0.666f, 0.308f to 0.639f, 0.299f to 0.574f,
        0.297f to 0.558f,
    )),
    ZonePoly(BodyZone.L_THIGH_BACK, listOf(
        0.176f to 0.518f, 0.232f to 0.543f, 0.288f to 0.543f, 0.289f to 0.564f,
        0.283f to 0.595f, 0.275f to 0.639f, 0.276f to 0.665f, 0.265f to 0.715f,
        0.198f to 0.696f, 0.195f to 0.646f, 0.175f to 0.577f, 0.174f to 0.547f,
    )),
    ZonePoly(BodyZone.R_CALF_BACK, listOf(
        0.320f to 0.715f, 0.386f to 0.696f, 0.394f to 0.748f, 0.386f to 0.792f,
        0.368f to 0.853f, 0.363f to 0.892f, 0.369f to 0.912f, 0.368f to 0.918f,
        0.324f to 0.919f, 0.320f to 0.904f, 0.327f to 0.882f, 0.324f to 0.825f,
        0.310f to 0.755f, 0.318f to 0.726f,
    )),
    ZonePoly(BodyZone.L_CALF_BACK, listOf(
        0.199f to 0.698f, 0.266f to 0.717f, 0.272f to 0.762f, 0.259f to 0.820f,
        0.253f to 0.855f, 0.252f to 0.879f, 0.259f to 0.905f, 0.255f to 0.920f,
        0.210f to 0.913f, 0.210f to 0.907f, 0.215f to 0.892f, 0.209f to 0.840f,
        0.190f to 0.753f, 0.196f to 0.710f,
    )),
    ZonePoly(BodyZone.R_FOOT_SOLE, listOf(
        0.324f to 0.919f, 0.327f to 0.920f, 0.339f to 0.920f, 0.352f to 0.920f,
        0.365f to 0.920f, 0.373f to 0.921f, 0.377f to 0.926f, 0.383f to 0.932f,
        0.397f to 0.942f, 0.396f to 0.943f, 0.398f to 0.947f, 0.398f to 0.949f,
        0.397f to 0.951f, 0.392f to 0.952f, 0.390f to 0.953f, 0.373f to 0.959f,
        0.367f to 0.961f, 0.361f to 0.961f, 0.357f to 0.962f, 0.344f to 0.962f,
        0.333f to 0.961f, 0.330f to 0.962f, 0.327f to 0.960f, 0.323f to 0.956f,
        0.321f to 0.951f, 0.320f to 0.944f, 0.320f to 0.939f, 0.322f to 0.930f,
    )),
    ZonePoly(BodyZone.L_FOOT_SOLE, listOf(
        0.211f to 0.914f, 0.221f to 0.915f, 0.232f to 0.917f, 0.243f to 0.919f,
        0.254f to 0.921f, 0.257f to 0.924f, 0.259f to 0.933f, 0.261f to 0.943f,
        0.260f to 0.952f, 0.257f to 0.957f, 0.251f to 0.961f, 0.249f to 0.962f,
        0.235f to 0.961f, 0.222f to 0.962f, 0.218f to 0.961f, 0.207f to 0.959f,
        0.194f to 0.954f, 0.185f to 0.952f, 0.183f to 0.951f, 0.180f to 0.947f,
        0.180f to 0.945f, 0.182f to 0.943f, 0.184f to 0.940f, 0.204f to 0.923f,
    )),
)

// ── Hit testing (ray casting) ─────────────────────────────────────────────────

private fun pointInPolygon(x: Float, y: Float, pts: List<Pair<Float, Float>>): Boolean {
    var inside = false
    var j = pts.size - 1
    for (i in pts.indices) {
        val xi = pts[i].first; val yi = pts[i].second
        val xj = pts[j].first; val yj = pts[j].second
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
            inside = !inside
        }
        j = i
    }
    return inside
}

private fun hitTest(nx: Float, ny: Float): BodyZone? =
    ZONE_POLYGONS.firstOrNull { pointInPolygon(nx, ny, it.pts) }?.zone

// ── Public composable ─────────────────────────────────────────────────────────

/**
 * Interactive body diagram backed by body_map.png. Tapping a zone cycles its
 * swelling severity: null → 1 (mild) → 2 (moderate) → 3 (severe) → null (clear).
 * Both front and back views are always visible in the single image.
 */
@Composable
fun BodyMapSelector(
    swelling: Map<String, Int>,
    onSwellingChange: (zone: String, severity: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = KetoTheme.colors
    val painter = painterResource(R.drawable.body_map)
    val aspectRatio = remember(painter) {
        painter.intrinsicSize.let { if (it.height > 0f) it.width / it.height else 1f }
    }
    val currentSwelling by rememberUpdatedState(swelling)
    val currentOnChange by rememberUpdatedState(onSwellingChange)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Body image + severity overlay canvas
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w == 0f) return@detectTapGestures
                        val nx = offset.x / w
                        val ny = offset.y / h
                        hitTest(nx, ny)?.let { zone ->
                            val next = currentSwelling[zone.id].nextSeverity()
                            currentOnChange(zone.id, next ?: 0)
                        }
                    }
                },
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(Modifier.fillMaxSize()) {
                ZONE_POLYGONS.forEach { (zone, pts) ->
                    val sev = swelling[zone.id] ?: return@forEach
                    val path = Path().apply {
                        val first = pts.first()
                        moveTo(first.first * size.width, first.second * size.height)
                        pts.drop(1).forEach { (nx, ny) ->
                            lineTo(nx * size.width, ny * size.height)
                        }
                        close()
                    }
                    drawPath(path, sev.toFillColor().copy(alpha = OVERLAY_ALPHA))
                }
            }
        }

        // Severity legend
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        ) {
            listOf(1 to "Mild", 2 to "Moderate", 3 to "Severe").forEach { (sev, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        Modifier
                            .size(11.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(sev.toFillColor()),
                    )
                    KText(label, size = 11, color = c.txtM)
                }
            }
        }

        // Active zone list
        val active = swelling.entries
            .filter { it.value in 1..3 }
            .sortedBy { it.key }
        if (active.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                KText(
                    "Marked areas".uppercase(),
                    size = 11, color = c.txtM, weight = FontWeight.SemiBold,
                    letterSpacing = 0.7f,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                active.forEach { (id, sev) ->
                    val label = BodyZone.fromId(id)?.label ?: id
                    val sevLabel = when (sev) { 1 -> "Mild"; 2 -> "Moderate"; else -> "Severe" }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.inp)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KText(label, size = 14, color = c.txt)
                        KText(sevLabel, size = 13, color = sev.toTextColor())
                    }
                }
            }
        }
    }
}
