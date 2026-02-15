package org.example.documenteditordesktop.ClassesViewModels

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp


object AppIcons {
    val ArrowBack: ImageVector
        get() = _arrowBack

    val Check: ImageVector
        get() = _check

    val Menu: ImageVector
        get() = _menu

    val SettingsImage: ImageVector
        get() = _settings

    val PlusImage: ImageVector
        get() = _add

    private val _arrowBack: ImageVector = Builder(
        name = "ArrowBack",
        defaultWidth = 24.0.dp,
        defaultHeight = 24.0.dp,
        viewportWidth = 960.0f,
        viewportHeight = 960.0f
    ).apply {
        path(
            fill = SolidColor(Color(0xFFE3E3E3)),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(313f, 520f)
            lineToRelative(224f, 224f)
            lineToRelative(-57f, 56f)
            lineToRelative(-320f, -320f)
            lineToRelative(320f, -320f)
            lineToRelative(57f, 56f)
            lineToRelative(-224f, 224f)
            horizontalLineToRelative(487f)
            verticalLineToRelative(80f)
            horizontalLineTo(313f)
            close()
        }
    }.build()

    private val _check = Builder(
        name = "CheckImageVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(
            fill = SolidColor(Color(0xFFE3E3E3)),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ){
            moveTo(382f, 720f)
            lineTo(154f, 492f)
            lineToRelative(57f, -57f)
            lineToRelative(171f, 171f)
            lineToRelative(367f, -367f)
            lineToRelative(57f, 57f)
            lineToRelative(-424f, 424f)
            close()
        }
    }.build()
    private val _settings = Builder(
        name = "SettingsImageVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(
            fill = SolidColor(Color(0xFFE3E3E3)),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ){
            moveTo(370f, 880f)
            lineToRelative(-16f, -128f)
            quadToRelative(-13f, -5f, -24.5f, -12f)
            reflectiveQuadTo(307f, 725f)
            lineToRelative(-119f, 50f)
            lineTo(78f, 585f)
            lineToRelative(103f, -78f)
            quadToRelative(-1f, -7f, -1f, -13.5f)
            verticalLineToRelative(-27f)
            quadToRelative(0f, -6.5f, 1f, -13.5f)
            lineTo(78f, 375f)
            lineToRelative(110f, -190f)
            lineToRelative(119f, 50f)
            quadToRelative(11f, -8f, 23f, -15f)
            reflectiveQuadToRelative(24f, -12f)
            lineToRelative(16f, -128f)
            horizontalLineToRelative(220f)
            lineToRelative(16f, 128f)
            quadToRelative(13f, 5f, 24.5f, 12f)
            reflectiveQuadToRelative(22.5f, 15f)
            lineToRelative(119f, -50f)
            lineToRelative(110f, 190f)
            lineToRelative(-103f, 78f)
            quadToRelative(1f, 7f, 1f, 13.5f)
            verticalLineToRelative(27f)
            quadToRelative(0f, 6.5f, -2f, 13.5f)
            lineToRelative(103f, 78f)
            lineToRelative(-110f, 190f)
            lineToRelative(-118f, -50f)
            quadToRelative(-11f, 8f, -23f, 15f)
            reflectiveQuadToRelative(-24f, 12f)
            lineTo(590f, 880f)
            horizontalLineTo(370f)
            close()
            moveToRelative(70f, -80f)
            horizontalLineToRelative(79f)
            lineToRelative(14f, -106f)
            quadToRelative(31f, -8f, 57.5f, -23.5f)
            reflectiveQuadTo(639f, 633f)
            lineToRelative(99f, 41f)
            lineToRelative(39f, -68f)
            lineToRelative(-86f, -65f)
            quadToRelative(5f, -14f, 7f, -29.5f)
            reflectiveQuadToRelative(2f, -31.5f)
            quadToRelative(0f, -16f, -2f, -31.5f)
            reflectiveQuadToRelative(-7f, -29.5f)
            lineToRelative(86f, -65f)
            lineToRelative(-39f, -68f)
            lineToRelative(-99f, 42f)
            quadToRelative(-22f, -23f, -48.5f, -38.5f)
            reflectiveQuadTo(533f, 266f)
            lineToRelative(-13f, -106f)
            horizontalLineToRelative(-79f)
            lineToRelative(-14f, 106f)
            quadToRelative(-31f, 8f, -57.5f, 23.5f)
            reflectiveQuadTo(321f, 327f)
            lineToRelative(-99f, -41f)
            lineToRelative(-39f, 68f)
            lineToRelative(86f, 64f)
            quadToRelative(-5f, 15f, -7f, 30f)
            reflectiveQuadToRelative(-2f, 32f)
            quadToRelative(0f, 16f, 2f, 31f)
            reflectiveQuadToRelative(7f, 30f)
            lineToRelative(-86f, 65f)
            lineToRelative(39f, 68f)
            lineToRelative(99f, -42f)
            quadToRelative(22f, 23f, 48.5f, 38.5f)
            reflectiveQuadTo(427f, 694f)
            lineToRelative(13f, 106f)
            close()
            moveToRelative(42f, -180f)
            quadToRelative(58f, 0f, 99f, -41f)
            reflectiveQuadToRelative(41f, -99f)
            quadToRelative(0f, -58f, -41f, -99f)
            reflectiveQuadToRelative(-99f, -41f)
            quadToRelative(-59f, 0f, -99.5f, 41f)
            reflectiveQuadTo(342f, 480f)
            quadToRelative(0f, 58f, 40.5f, 99f)
            reflectiveQuadToRelative(99.5f, 41f)
            close()
            moveToRelative(-2f, -140f)
            close()
        }
    }.build()
    private val _menu = Builder(
        name = "MenuImageVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).apply {
        path(
            fill = SolidColor(Color(0xFFE3E3E3)),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ){
            moveTo(120f, 720f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(720f)
            verticalLineToRelative(80f)
            horizontalLineTo(120f)
            close()
            moveToRelative(0f, -200f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(720f)
            verticalLineToRelative(80f)
            horizontalLineTo(120f)
            close()
            moveToRelative(0f, -200f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(720f)
            verticalLineToRelative(80f)
            horizontalLineTo(120f)
            close()
        }
    }.build()

    private val _add: ImageVector = Builder(
        name = "Add",
        defaultWidth = 24.0.dp,
        defaultHeight = 24.0.dp,
        viewportWidth = 960.0f,
        viewportHeight = 960.0f
    ).apply {
        path(
            fill = SolidColor(Color(0xFFE3E3E3)),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(440f, 440f)
            verticalLineTo(200f)
            horizontalLineTo(520f)
            verticalLineTo(440f)
            horizontalLineTo(760f)
            verticalLineTo(520f)
            horizontalLineTo(520f)
            verticalLineTo(760f)
            horizontalLineTo(440f)
            verticalLineTo(520f)
            horizontalLineTo(200f)
            verticalLineTo(440f)
            horizontalLineTo(440f)
            close()
        }
    }.build()
}