package io.carlol.android.ticketview;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.graphics.Bitmap.Config.ALPHA_8;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.TRANSPARENT;
import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.PorterDuff.Mode.SRC_IN;

/**
 * Created by carlol on 14/11/2019.
 */

public class TicketView extends FrameLayout {
    public static final String TAG = TicketView.class.getName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Orientation.HORIZONTAL, Orientation.VERTICAL})
    public @interface Orientation {
        int HORIZONTAL = 0;
        int VERTICAL = 1;
    }

    private static final int DEFAULT_RADIUS = 0;
    private static final int NO_VALUE = -1;

    private Paint backgroundPaint = new Paint();
    private Paint borderPaint = new Paint();
    private final Paint mShadowPaint = new Paint(ANTI_ALIAS_FLAG);

    private int backgroundColor = Color.WHITE;
    private boolean showBorder = false;
    private int borderWidth = 0;
    private int borderColor = Color.GRAY;

    private Bitmap mShadow;
    private float shadowBlurRadius = 15f;

    private RectF borderRect;

    private int orientation;
    private int anchorViewId;

    private float holePosition;
    private float holeRadius;

    private int cornerRadiusTopLeft;
    private int cornerRadiusTopRight;
    private int cornerRadiusBottomRight;
    private int cornerRadiusBottomLeft;

    private Path ticketPath = new Path();

    private RectF scallopArc = new RectF();
    private RectF cornerArc = new RectF();

    private AtomicBoolean isDirty = new AtomicBoolean(true);


    public TicketView(Context context) {
        super(context);
        init(null);
    }

    public TicketView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public TicketView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {

        setLayerType(LAYER_TYPE_HARDWARE, null);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TicketView);
        try {
            orientation = a.getInt(R.styleable.TicketView_tv_orientation, Orientation.VERTICAL);
            holeRadius = a.getFloat(R.styleable.TicketView_tv_holeRadius, DEFAULT_RADIUS);
            anchorViewId = a.getResourceId(R.styleable.TicketView_tv_anchor, NO_VALUE);

            backgroundColor = a.getColor(R.styleable.TicketView_tv_backgroundColor, Color.WHITE);
            showBorder = a.getBoolean(R.styleable.TicketView_tv_showBorder, false);
            borderColor = a.getColor(R.styleable.TicketView_tv_borderColor, Color.GRAY);
            borderWidth = a.getDimensionPixelSize(R.styleable.TicketView_tv_borderWidth, 0);

            float elevation = 0f;
            if (a.hasValue(R.styleable.TicketView_tv_elevation)) {
                elevation = a.getDimension(R.styleable.TicketView_tv_elevation, elevation);
            } else if (a.hasValue(R.styleable.TicketView_android_elevation)) {
                elevation = a.getDimension(R.styleable.TicketView_android_elevation, elevation);
            }
            if (elevation > 0f) {
                setShadowBlurRadius(elevation);
            }

            int defCornerRadius = a.getDimensionPixelSize(R.styleable.TicketView_tv_cornerRadius, 0);
            cornerRadiusTopLeft = a.getDimensionPixelSize(R.styleable.TicketView_tv_topLeftCornerRadius, defCornerRadius);
            cornerRadiusTopRight = a.getDimensionPixelSize(R.styleable.TicketView_tv_topRightCornerRadius, defCornerRadius);
            cornerRadiusBottomRight = a.getDimensionPixelSize(R.styleable.TicketView_tv_bottomRightCornerRadius, defCornerRadius);
            cornerRadiusBottomLeft = a.getDimensionPixelSize(R.styleable.TicketView_tv_bottomLeftCornerRadius, defCornerRadius);

        } finally {
            a.recycle();
        }

        isDirty.set(true);

        updateBorderPaint();
        updateBackgroundPaint();
        updateShadowPaint();
    }


    /*
     * Public methods
     */

    public void setRadius(float radius) {
        this.holeRadius = radius;
        postInvalidate();
    }

    public void setOrientation(@Orientation int orientation) {
        this.orientation = orientation;
        postInvalidate();
    }

    public void setAnchor(View view) {
        Rect rect = new Rect();
        //get view's visible bounds
        view.getDrawingRect(rect);
        //calculate the relative coordinates to parent
        offsetDescendantRectToMyCoords(view, rect);
        // center of the anchor view
        if (orientation == Orientation.HORIZONTAL) {
            holePosition = rect.left
                    + (rect.width() > 0 ? rect.width() / 2 : 0)
                    - holeRadius;
        } else {
            holePosition = rect.bottom
                    - (rect.height() > 0 ? rect.height() / 2 : 0)
                    + holeRadius;
        }
        postInvalidate();
    }

    public void setBackgroundColor(@ColorInt int color) {
        this.backgroundColor = color;
        postInvalidate();
    }

    public void setShowBorder(boolean show) {
        this.showBorder = show;
        postInvalidate();
    }

    public void setBorderColor(@ColorInt int color) {
        this.backgroundColor = color;
        postInvalidate();
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        postInvalidate();
    }


    /*
     * Lifecycle methods
     */

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (anchorViewId != NO_VALUE) {
            final View anchorView = findViewById(anchorViewId);

            getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                    setAnchor(anchorView);
                }
            });
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
            borderRect = new RectF(
                    shadowBlurRadius
                    , shadowBlurRadius
                    , w - shadowBlurRadius
                    , h - shadowBlurRadius
            );
            isDirty.set(true);
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isDirty.getAndSet(false)) {
            updateBorderPath();
            generateShadow();
        }

        // shadow if needed
        if (shadowBlurRadius > 0f && !isInEditMode()) {
            canvas.drawBitmap(mShadow, 0f, shadowBlurRadius / 2f, null);
        }

        // background
        canvas.drawPath(ticketPath, backgroundPaint);

        // border if needed
        if (showBorder) {
            canvas.drawPath(ticketPath, borderPaint);
        }

        super.onDraw(canvas);
    }


    /*
     * Internal methods
     */

    private void updateBackgroundPaint() {
        backgroundPaint.setAlpha(0);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL);
    }

    private void updateBorderPaint() {
        borderPaint.setAlpha(0);
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(borderWidth);
        borderPaint.setStyle(Paint.Style.STROKE);
    }

    private void updateShadowPaint() {
        mShadowPaint.setColorFilter(new PorterDuffColorFilter(BLACK, SRC_IN));
        mShadowPaint.setAlpha(51); // 20%
    }

    private void setShadowBlurRadius(float elevation) {
        if (!isJellyBeanAndAbove()) {
            Log.w(TAG, "Ticket elevation only works with Android Jelly Bean and above");
            return;
        }
        float maxElevation = dpToPx(24f, getContext());
        shadowBlurRadius = Math.min(25f * (elevation / maxElevation), 25f);
    }

    private void updateBorderPath() {
        ticketPath.reset();

        if (orientation == Orientation.VERTICAL) {
            applyBorderPathVertical(ticketPath);
        } else if (orientation == Orientation.HORIZONTAL) {
            applyBorderPathHorizontal(ticketPath);
        }
    }

    private void applyBorderPathVertical(Path path) {

        // start
        path.moveTo(borderRect.left, borderRect.top + cornerRadiusTopLeft);

        // top left corner
        path.arcTo(getTopLeftCornerRect(), 180, 90, false);

        // top
        path.lineTo(borderRect.right - cornerRadiusTopRight, borderRect.top);

        // top right corner
        path.arcTo(getTopRightCornerRect(), 270, 90, false);

        // right
        path.lineTo(borderRect.right, holePosition - (holeRadius * 2));
        path.arcTo(getRightScallopRect(borderRect.right, holePosition), 270, -180, false);
        path.lineTo(borderRect.right, borderRect.bottom - cornerRadiusBottomRight);

        // bottom right corner
        path.arcTo(getBottomRightCornerRect(), 0, 90, false);

        // bottom
        path.lineTo(borderRect.left + cornerRadiusBottomLeft, borderRect.bottom);

        // bottom left corner
        path.arcTo(getBottomLeftCornerRect(), 90, 90, false);

        // left
        path.lineTo(borderRect.left, holePosition + (holeRadius * 2));
        path.arcTo(getLeftScallopRect(borderRect.left, holePosition), 90, -180, false);
        path.lineTo(borderRect.left, borderRect.top);
    }

    private void applyBorderPathHorizontal(Path path) {
        // start
        path.moveTo(borderRect.left, borderRect.top + cornerRadiusTopLeft);

        // top left corner
        path.arcTo(getTopLeftCornerRect(), 180, 90, false);

        // top
        path.lineTo(holePosition + (holeRadius * 2), borderRect.top);
        path.arcTo(getTopScallopRect(holePosition, borderRect.top), 180, -180, false);
        path.lineTo(borderRect.right - cornerRadiusTopRight, borderRect.top);

        // top right corner
        path.arcTo(getTopRightCornerRect(), 270, 90, false);

        // right
        path.lineTo(borderRect.right, borderRect.bottom - cornerRadiusBottomRight);

        // bottom right corner
        path.arcTo(getBottomRightCornerRect(), 0, 90, false);

        // bottom
        path.lineTo(holePosition + holeRadius, borderRect.bottom);
        path.arcTo(getBottomScallopRect(holePosition, borderRect.bottom), 0, -180, false);
        path.lineTo(borderRect.left + cornerRadiusBottomLeft, borderRect.bottom);

        // bottom left corner
        path.arcTo(getBottomLeftCornerRect(), 90, 90, false);

        // left
        path.lineTo(borderRect.left, borderRect.top);
    }


    /*
     * Corner rect
     */

    private RectF getTopLeftCornerRect() {
        cornerArc.set(
                borderRect.left
                , borderRect.top
                , borderRect.left + cornerRadiusTopLeft * 2
                , borderRect.top + cornerRadiusTopLeft * 2
        );
        return cornerArc;
    }

    private RectF getTopRightCornerRect() {
        cornerArc.set(
                borderRect.right - cornerRadiusTopRight * 2
                , borderRect.top
                , borderRect.right
                , borderRect.top + cornerRadiusTopRight * 2
        );
        return cornerArc;
    }

    private RectF getBottomRightCornerRect() {
        cornerArc.set(
                borderRect.right - cornerRadiusBottomRight * 2
                , borderRect.bottom - cornerRadiusBottomRight * 2
                , borderRect.right
                , borderRect.bottom
        );
        return cornerArc;
    }

    private RectF getBottomLeftCornerRect() {
        cornerArc.set(
                borderRect.left
                , borderRect.bottom - cornerRadiusBottomLeft * 2
                , borderRect.left + cornerRadiusBottomLeft * 2
                , borderRect.bottom
        );
        return cornerArc;
    }


    /*
     * Vertical orientation scallop rect
     */

    private RectF getRightScallopRect(float right, float top) {
        scallopArc.set(
                right - holeRadius
                , top - holeRadius * 2
                , right + holeRadius
                , top);
        return scallopArc;
    }

    private RectF getLeftScallopRect(float left, float top) {
        scallopArc.set(
                left - holeRadius
                , top - holeRadius * 2
                , left + holeRadius
                , top);
        return scallopArc;
    }


    /*
     * Horizontal orientation scallops
     */

    private RectF getTopScallopRect(float right, float top) {
        scallopArc.set(
                right
                , top - holeRadius
                , right + holeRadius * 2
                , top + holeRadius);
        return scallopArc;
    }

    private RectF getBottomScallopRect(float left, float top) {
        scallopArc.set(
                left
                , top - holeRadius
                , left + holeRadius * 2
                , top + holeRadius);
        return scallopArc;
    }


    private void generateShadow() {
        if (isJellyBeanAndAbove() && !isInEditMode()) {
            if (shadowBlurRadius == 0f) return;

            if (mShadow == null) {
                mShadow = Bitmap.createBitmap(getWidth(), getHeight(), ALPHA_8);
            } else {
                mShadow.eraseColor(TRANSPARENT);
            }
            Canvas c = new Canvas(mShadow);
            c.drawPath(ticketPath, mShadowPaint);
            if (showBorder) {
                c.drawPath(ticketPath, mShadowPaint);
            }
            RenderScript rs = RenderScript.create(getContext());
            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8(rs));
            Allocation input = Allocation.createFromBitmap(rs, mShadow);
            Allocation output = Allocation.createTyped(rs, input.getType());
            blur.setRadius(shadowBlurRadius);
            blur.setInput(input);
            blur.forEach(output);
            output.copyTo(mShadow);
            input.destroy();
            output.destroy();
            blur.destroy();
        }
    }


    /*
     * Utility methods
     */

    private static boolean isJellyBeanAndAbove() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    private static int dpToPx(float dp, Context context) {
        return dpToPx(dp, context.getResources());
    }

    private static int dpToPx(float dp, Resources resources) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
    }

}
