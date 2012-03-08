package com.aragaer.jtt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.TextView;

public class JTTClockView extends TextView {
    private final static int step = 360 / 12;
    private final static float gap = 1.5f;
    private final Paint mStrokePaint = new Paint();
    private final Paint mStrokePaint2 = new Paint();
    private final Paint mSolidPaint = new Paint();
    private final Paint mSolidPaint2 = new Paint();
    private Bitmap ch, cv;
    private JTTHour hour = new JTTHour(0);
    private JTTHour.StringsHelper hs;

    public JTTClockView(Context context) {
        this(context, null, 0);
    }

    public JTTClockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JTTClockView(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        hs = new JTTHour.StringsHelper(ctx);
        setupPaint(ctx);

        /* make them non-null */
        cv = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        cv.recycle();
        ch = cv;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (hour == null)
            return;

        final int w = getWidth();
        final int h = getHeight();
        final Boolean v = h > w;
        final int size = v ? w / 2 : h / 2;

        if (v && cv.isRecycled())
            cv = drawBitmap(hour.num, size);

        if (!v && ch.isRecycled())
            ch = drawBitmap(hour.num, size);

        final Bitmap clock = v ? cv : ch;
        final Matrix m = new Matrix();
        m.setTranslate(v ? 0 : 3 * w / 5 - size, v ? 3 * h / 5 - size : 0);
        m.preRotate(step * (0.5f - hour.num) - gap - (step - gap * 2)
                * hour.fraction / 100.0f, size, size);
        canvas.drawBitmap(clock, m, mStrokePaint2);

        mStrokePaint2.setTextSize(v ? w / 20 : w / 15);

        final String s = v ? hs.getHrOf(hour.num) : hs.getHour(hour.num);
        canvas.drawText(s, v ? size : w / 5, v ? h / 10 : size,
                mStrokePaint2);
    }

    private final void setupPaint(Context ctx) {
        LightingColorFilter f = new LightingColorFilter(0xFFFFFFFF, 0xFFCCCCCC);
        mStrokePaint.setAntiAlias(true);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setTextAlign(Paint.Align.CENTER);
        mStrokePaint.setColor(Color.parseColor(ctx.getString(R.color.stroke)));

        mStrokePaint2.setAntiAlias(true);
        mStrokePaint2.setFilterBitmap(true);
        mStrokePaint2.setDither(true);
        mStrokePaint2.setTextAlign(Paint.Align.CENTER);
        mStrokePaint2.setStyle(Paint.Style.STROKE);
        mStrokePaint2.setColor(Color.parseColor(ctx
                .getString(R.color.tab_active)));

        mSolidPaint.setAntiAlias(true);
        mSolidPaint.setStyle(Paint.Style.FILL);
        mSolidPaint.setTextAlign(Paint.Align.CENTER);
        mSolidPaint.setColor(Color.parseColor(ctx.getString(R.color.fill)));
        mSolidPaint.setColorFilter(f);

        mSolidPaint2.setAntiAlias(true);
        mSolidPaint2.setStyle(Paint.Style.FILL);
        mSolidPaint2.setTextAlign(Paint.Align.CENTER);
        mSolidPaint2.setColor(Color.parseColor(ctx.getString(R.color.night)));
        mSolidPaint2.setStrokeWidth(1.3f);
    }

    private Bitmap drawBitmap(int num, int c) {
        Bitmap result = Bitmap.createBitmap(c * 2, c * 2,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        final int iR = 2 * c / 5;
        final int thick = 2 * c / 5;
        final int oR = iR + thick;
        final int selR = oR + thick / 4;
        final float sR = iR * 0.2f;

        mStrokePaint.setTextSize(thick / 3);
        mSolidPaint2.setTextSize(thick / 3);

        final RectF outer = new RectF(c - oR, c - oR, c + oR, c + oR);
        final RectF inner = new RectF(c - iR, c - iR, c + iR, c + iR);
        final RectF sel = new RectF(c - selR, c - selR, c + selR, c + selR);
        final RectF sun = new RectF(c - sR, c - sR, c + sR, c + sR);

        canvas.rotate(-step / 2, c, c);
        canvas.translate(-iR * 0.75f, 0);
        canvas.drawArc(sun, 0, 360, false, mSolidPaint);
        canvas.drawArc(sun, 0, 360, false, mStrokePaint);
        canvas.rotate(90, c + iR * 0.75f, c);
        canvas.drawArc(sun, 0, 180, false, mSolidPaint);
        canvas.drawArc(sun, 180, 180, false, mSolidPaint2);
        canvas.drawArc(sun, 0, 360, false, mStrokePaint);
        canvas.drawLine(c - sR, c, c + sR, c, mStrokePaint);
        canvas.rotate(90, c + iR * 0.75f, c);
        canvas.drawArc(sun, 0, 360, false, mSolidPaint2);
        canvas.drawArc(sun, 0, 360, false, mStrokePaint);
        canvas.rotate(90, c + iR * 0.75f, c);
        canvas.drawArc(sun, 180, 180, false, mSolidPaint);
        canvas.drawArc(sun, 0, 180, false, mSolidPaint2);
        canvas.drawArc(sun, 0, 360, false, mStrokePaint);
        canvas.drawLine(c - sR, c, c + sR, c, mStrokePaint);
        canvas.translate(iR * 0.75f, 0);
        canvas.rotate(90 + step / 2, c, c);

        final int arc_start = -90 - Math.round(step / 2 - gap);
        final int arc_end = -90 + Math.round(step / 2 - gap);
        final int arc_len = arc_end - arc_start;

        final double start = Math.toRadians(arc_start);
        final double end = Math.toRadians(arc_end);
        final float l2x = c + (float) Math.cos(start) * iR;
        final float l2y = c + (float) Math.sin(start) * iR;
        final float l1x = c + (float) Math.cos(end) * oR;
        final float l1y = c + (float) Math.sin(end) * oR;
        final float l1xs = c + (float) Math.cos(end) * selR;
        final float l1ys = c + (float) Math.sin(end) * selR;

        for (int hr = 0; hr < 12; hr++) {
            final Path path = new Path();
            final Boolean current = hr == num;

            path.addArc(inner, arc_start, arc_len);
            path.lineTo(current ? l1xs : l1x, current ? l1ys : l1y);
            path.addArc(current ? sel : outer, arc_end, -arc_len);
            path.lineTo(l2x, l2y);
            canvas.drawPath(path, mSolidPaint);
            canvas.drawPath(path, mStrokePaint);

            final float glyph_y = c - iR - (current ? 5 : 4) * thick / 9;
            canvas.drawText(JTTHour.Glyphs[hr], c, glyph_y, mSolidPaint2);
            canvas.drawText(JTTHour.Glyphs[hr], c, glyph_y, mStrokePaint);
            canvas.rotate(step, c, c);
        }

        return result;
    }

    public void setJTTHour(JTTHour new_hour) {
        if (hour.num != new_hour.num) {
            cv.recycle();
            ch.recycle();
        }
        hour = new_hour;
        invalidate();
    }
}
