package io.github.monitbisht.iterack;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.renderer.BarChartRenderer;

public class RoundedBarChartRenderer extends BarChartRenderer {

    private final float radius;

    public RoundedBarChartRenderer(BarChart chart, float radius) {
        super(chart, chart.getAnimator(), chart.getViewPortHandler());
        this.radius = radius;
        mRenderPaint.setAntiAlias(true);
    }


    protected void drawBar(Canvas c, RectF barRect, int index) {
        // Shrink rect a bit to make room for radius effect
        RectF adjusted = new RectF(
                barRect.left + 5f,
                barRect.top,
                barRect.right - 5f,
                barRect.bottom
        );

        Path path = new Path();

        // Round TOP corners only
        float[] radii = new float[]{
                radius, radius,   // top-left
                radius, radius,   // top-right
                0f, 0f,           // bottom-right
                0f, 0f            // bottom-left
        };

        path.addRoundRect(adjusted, radii, Path.Direction.CW);

        c.drawPath(path, mRenderPaint);
    }
}
