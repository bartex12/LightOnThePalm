package com.bartex.adresslentawifi.staticcolor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Цветовой круг: по окружности меняется цветовой тон, от центра к краю - насыщенность
 * (в центре белый, на краю чистый цвет). Яркость круг не задаёт - для неё есть ползунок.
 *
 * Круг рисуется двумя градиентами, а цвет в точке нажатия вычисляется по формуле
 * (угол -> тон, расстояние от центра -> насыщенность), а не считывается с картинки.
 * Поэтому цвет получается точным при любом размере экрана.
 */
class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /**
     * Вызывается при выборе цвета.
     * color - цвет вида 0xFFRRGGBB; finished = true, когда палец отпущен.
     */
    var onColorChanged: ((color: Int, finished: Boolean) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val markerRadius = 11f * density

    private val huePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val saturationPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = Color.WHITE
    }
    private val markerOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.BLACK
    }

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    private var hue = 0f          //тон, 0..360
    private var saturation = 0f   //насыщенность, 0..1
    private var hasColor = false  //выбран ли цвет (рисовать ли метку)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val contentW = w - paddingLeft - paddingRight
        val contentH = h - paddingTop - paddingBottom
        centerX = paddingLeft + contentW / 2f
        centerY = paddingTop + contentH / 2f
        //оставляем место, чтобы метка на краю круга не обрезалась
        radius = (min(contentW, contentH) / 2f - markerRadius).coerceAtLeast(1f)

        //тон по окружности: красный, жёлтый, зелёный, голубой, синий, малиновый, красный
        huePaint.shader = SweepGradient(
            centerX, centerY,
            intArrayOf(
                0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(), 0xFF00FFFF.toInt(),
                0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF0000.toInt()
            ),
            null
        )
        //насыщенность: белый в центре плавно становится прозрачным к краю
        saturationPaint.shader = RadialGradient(
            centerX, centerY, radius,
            Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(centerX, centerY, radius, huePaint)
        canvas.drawCircle(centerX, centerY, radius, saturationPaint)

        if (hasColor) {
            val angle = Math.toRadians(hue.toDouble())
            val x = centerX + (cos(angle) * saturation * radius).toFloat()
            val y = centerY + (sin(angle) * saturation * radius).toFloat()
            markerFillPaint.color = currentColor()
            canvas.drawCircle(x, y, markerRadius, markerFillPaint)
            canvas.drawCircle(x, y, markerRadius, markerRingPaint)
            canvas.drawCircle(x, y, markerRadius + markerRingPaint.strokeWidth / 2, markerOutlinePaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                //чтобы экран не прокручивался, пока палец ведут по кругу
                parent?.requestDisallowInterceptTouchEvent(true)
                pick(event.x, event.y, finished = false)
            }
            MotionEvent.ACTION_MOVE -> pick(event.x, event.y, finished = false)
            MotionEvent.ACTION_UP -> pick(event.x, event.y, finished = true)
            MotionEvent.ACTION_CANCEL -> onColorChanged?.invoke(currentColor(), true)
            else -> return super.onTouchEvent(event)
        }
        return true
    }

    //вычисляем цвет в точке нажатия
    private fun pick(x: Float, y: Float, finished: Boolean) {
        val dx = x - centerX
        val dy = y - centerY
        //угол отсчитывается от направления "вправо" по часовой стрелке - как у SweepGradient
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angle < 0) angle += 360f
        hue = angle
        //за пределами круга берём цвет на его краю
        saturation = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
        hasColor = true
        invalidate()
        onColorChanged?.invoke(currentColor(), finished)
    }

    private fun currentColor(): Int =
        Color.HSVToColor(floatArrayOf(hue, saturation, 1f))

    /** Поставить метку на заданный цвет (без вызова onColorChanged). */
    fun setColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        hasColor = true
        invalidate()
    }
}
