package com.sohu.kuaizhan.sitebuilder.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.sohu.kuaizhan.sitebuilder.utils.DisplayUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by zhulijun on 6/2/15.
 */
public class DrawerNav extends ViewGroup {
    private static final String TAG = "DrawerLayout";

    @IntDef({STATE_IDLE, STATE_DRAGGING, STATE_SETTLING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    @IntDef({LOCK_MODE_UNLOCKED, LOCK_MODE_LOCKED_CLOSED, LOCK_MODE_LOCKED_OPEN})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LockMode {
    }

    public static final int LOCK_MODE_UNLOCKED = 0;
    public static final int LOCK_MODE_LOCKED_CLOSED = 1;
    public static final int LOCK_MODE_LOCKED_OPEN = 2;

    @IntDef({Gravity.LEFT, Gravity.RIGHT, GravityCompat.START, GravityCompat.END})
    @Retention(RetentionPolicy.SOURCE)
    private @interface EdgeGravity {
    }


    private static final int MIN_DRAWER_MARGIN = 64; // dp

    private static final int DEFAULT_SCRIM_COLOR = 0x99000000;

    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    /**
     * Experimental feature.
     */
    private static final boolean ALLOW_EDGE_LOCK = false;

    private static final boolean CHILDREN_DISALLOW_INTERCEPT = true;

    private static final float TOUCH_SLOP_SENSITIVITY = 1.f;
    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.layout_gravity
    };

    private int mMinDrawerMargin;

    private int mScrimColor = DEFAULT_SCRIM_COLOR;
    private float mScrimOpacity;
    private Paint mScrimPaint = new Paint();

    private int mLockMode;
    private boolean mDisallowInterceptRequested;
    private boolean mChildrenCanceledTouch;
    private final ViewDragHelper mDragHelper;
    private final ViewDragCallback mDragCallback;
    private Drawable mShadow;
    private int mDrawerState;
    private boolean mInLayout;
    private boolean mFirstLayout = true;

    private DrawerListener mListener;
    private int mMenuWidth;

    public interface DrawerListener {
        /**
         * Called when a drawer's position changes.
         *
         * @param drawerView  The child view that was moved
         * @param slideOffset The new offset of this drawer within its range, from 0-1
         */
        public void onDrawerSlide(View drawerView, float slideOffset);

        /**
         * Called when a drawer has settled in a completely open state.
         * The drawer is interactive at this point.
         *
         * @param drawerView Drawer view that is now open
         */
        public void onDrawerOpened(View drawerView);

        /**
         * Called when a drawer has settled in a completely closed state.
         *
         * @param drawerView Drawer view that is now closed
         */
        public void onDrawerClosed(View drawerView);

        /**
         * Called when the drawer motion state changes. The new state will
         * be one of {@link #STATE_IDLE}, {@link #STATE_DRAGGING} or {@link #STATE_SETTLING}.
         *
         * @param newState The new drawer motion state
         */
        public void onDrawerStateChanged(@State int newState);
    }

    public DrawerNav(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMinDrawerMargin = (int) (MIN_DRAWER_MARGIN * DisplayUtil.getDensity(context) + 0.5f);

        mDragCallback = new ViewDragCallback();
        mDragHelper = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mDragCallback);
        mDragCallback.setDragger(mDragHelper);
    }

    public void setDrawerShadow(Drawable shadowDrawable) {
        mShadow = shadowDrawable;
    }

    /**
     * Set a simple drawable used for DrawerView shadow.
     * The drawable provided must have a nonzero intrinsic width.
     *
     * @param resId Resource id of a shadow drawable to use at the edge of a drawer
     */
    public void setDrawerShadow(@DrawableRes int resId) {
        setDrawerShadow(getResources().getDrawable(resId));
    }

    /**
     * Set a color to use for the scrim that obscures menu content while a drawer is open.
     *
     * @param color Color to use in 0xAARRGGBB format.
     */
    public void setScrimColor(int color) {
        mScrimColor = color;
        invalidate();
    }

    /**
     * Set a listener to be notified of drawer events.
     *
     * @param listener Listener to notify when drawer events occur
     * @see DrawerListener
     */
    public void setDrawerListener(DrawerListener listener) {
        mListener = listener;
    }

    /**
     * Enable or disable interaction with drawer.
     * <p/>
     * <p>This allows the application to restrict the user's ability to open or close
     * drawer within this layout. DrawerLayout will still respond to calls to
     * {@link #openDrawer()}, {@link #closeDrawer()} and friends if a drawer is locked.</p>
     * <p/>
     * <p>Locking drawers open or closed will implicitly open or close
     * any drawers as appropriate.</p>
     *
     * @param lockMode The new lock mode for the given drawer. One of {@link #LOCK_MODE_UNLOCKED},
     *                 {@link #LOCK_MODE_LOCKED_CLOSED} or {@link #LOCK_MODE_LOCKED_OPEN}.
     */
    public void setDrawerLockMode(@LockMode int lockMode) {
        mLockMode = lockMode;
        if (lockMode != LOCK_MODE_UNLOCKED) {
            // Cancel interaction in progress
            mDragHelper.cancel();
        }
        switch (lockMode) {
            case LOCK_MODE_LOCKED_OPEN:
                openDrawer();
                break;
            case LOCK_MODE_LOCKED_CLOSED:
                closeDrawer();
                break;
            // default: do nothing
        }
    }

    @LockMode
    public int getDrawerLockMode() {
        return mLockMode;
    }

    private static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        if (bg != null) {
            return bg.getOpacity() == PixelFormat.OPAQUE;
        }
        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams
                ? new LayoutParams((LayoutParams) p)
                : p instanceof ViewGroup.MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            if (isInEditMode()) {
                // Don't crash the layout editor. Consume all of the space if specified
                // or pick a magic number from thin air otherwise.
                // TODO Better communication with tools of this bogus state.
                // It will crash on a real device.
                if (widthMode == MeasureSpec.AT_MOST) {
                    widthMode = MeasureSpec.EXACTLY;
                } else if (widthMode == MeasureSpec.UNSPECIFIED) {
                    widthMode = MeasureSpec.EXACTLY;
                    widthSize = 300;
                }
                if (heightMode == MeasureSpec.AT_MOST) {
                    heightMode = MeasureSpec.EXACTLY;
                } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                    heightMode = MeasureSpec.EXACTLY;
                    heightSize = 300;
                }
            } else {
                throw new IllegalArgumentException(
                        "DrawerLayout must be measured with MeasureSpec.EXACTLY.");
            }
        }

        setMeasuredDimension(widthSize, heightSize);


        // Gravity value for each drawer we've seen. Only one of each permitted.
        int foundDrawers = 0;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (isDrawerView(child)) {
                // Content views get measured at exactly the layout's size.
                final int contentWidthSpec = MeasureSpec.makeMeasureSpec(
                        widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                final int contentHeightSpec = MeasureSpec.makeMeasureSpec(
                        heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                child.measure(contentWidthSpec, contentHeightSpec);
            } else if (isMenuView(child)) {
                final int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                        mMinDrawerMargin + lp.leftMargin + lp.rightMargin,
                        lp.width);
                final int drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                        lp.topMargin + lp.bottomMargin,
                        lp.height);
                child.measure(drawerWidthSpec, drawerHeightSpec);
                mMenuWidth = child.getMeasuredWidth();
            } else {
                throw new IllegalStateException("Child " + child + " at index " + i +
                        " does not have a valid layout_gravity - must be Gravity.LEFT, " +
                        "Gravity.RIGHT or Gravity.CENTER");
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (isMenuView(child)) {
                child.layout(lp.leftMargin, lp.topMargin,
                        lp.leftMargin + child.getMeasuredWidth(),
                        lp.topMargin + child.getMeasuredHeight());
            } else { //Menu View
                final int childWidth = child.getMeasuredWidth();
                final int childHeight = child.getMeasuredHeight();
                int childLeft;

                final float newOffset;
                childLeft = (int) (childWidth * lp.offSet);
                newOffset = (float) (childLeft) / childWidth;

                final boolean changeOffset = newOffset != lp.offSet;

                final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (vgrav) {
                    default:
                    case Gravity.TOP: {
                        child.layout(childLeft, lp.topMargin, childLeft + childWidth,
                                lp.topMargin + childHeight);
                        break;
                    }

                    case Gravity.BOTTOM: {
                        final int height = b - t;
                        child.layout(childLeft,
                                height - lp.bottomMargin - child.getMeasuredHeight(),
                                childLeft + childWidth,
                                height - lp.bottomMargin);
                        break;
                    }

                    case Gravity.CENTER_VERTICAL: {
                        final int height = b - t;
                        int childTop = (height - childHeight) / 2;

                        // Offset for margins. If things don't fit right because of
                        // bad measurement before, oh well.
                        if (childTop < lp.topMargin) {
                            childTop = lp.topMargin;
                        } else if (childTop + childHeight > height - lp.bottomMargin) {
                            childTop = height - lp.bottomMargin - childHeight;
                        }
                        child.layout(childLeft, childTop, childLeft + childWidth,
                                childTop + childHeight);
                        break;
                    }
                }

                if (changeOffset) {
                    setDrawerViewOffset(child, newOffset);
                }
            }
        }
        mInLayout = false;
        mFirstLayout = false;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final int height = getHeight();
        final boolean drawingMenu = isMenuView(child);
        int clipLeft = 0, clipRight = getRight();

        final int restoreCount = canvas.save();
        if (drawingMenu) {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View v = getChildAt(i);
                if (v == child || !isDrawerView(v) ||
                        v.getHeight() < height) {
                    continue;
                }

                final int vleft = v.getLeft();
                if (vleft < clipRight) clipRight = vleft;
            }
            canvas.clipRect(clipLeft, 0, clipRight, getHeight());
        }
        final boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(restoreCount);

        if (mScrimOpacity > 0 && drawingMenu) {
            final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
            final int imag = (int) (baseAlpha * mScrimOpacity);
            final int color = imag << 24 | (mScrimColor & 0xffffff);
            mScrimPaint.setColor(color);

            canvas.drawRect(clipLeft, 0, clipRight, getHeight(), mScrimPaint);
        } else if (mShadow != null) {
            final int shadowWidth = mShadow.getIntrinsicWidth();
            final int childLeft = child.getLeft();
            final int drawerPeekDistance = mDragHelper.getEdgeSize();
            final float alpha =
                    Math.max(0, Math.min((float) childLeft / drawerPeekDistance, 1.f));
            mShadow.setBounds(childLeft - shadowWidth, child.getTop(),
                    childLeft, child.getBottom());
            mShadow.setAlpha((int) (0xff * alpha));
            mShadow.draw(canvas);
        }
        return result;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mDragHelper.processTouchEvent(ev);
        return true;
    }

    boolean isMenuView(View child) {
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        return params.gravity != Gravity.CENTER;
    }

    boolean isDrawerView(View child) {
        return ((LayoutParams) child.getLayoutParams()).gravity == Gravity.CENTER;
    }

    View findDrawerView() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (((LayoutParams) child.getLayoutParams()).gravity == Gravity.CENTER) {
                return child;
            }
        }
        return null;
    }

    @LockMode
    public int getDrawerLockMode(View content) {
        return mLockMode;
    }

    void cancelChildViewTouch() {
        // Cancel child touches
        if (!mChildrenCanceledTouch) {
            final long now = SystemClock.uptimeMillis();
            final MotionEvent cancelEvent = MotionEvent.obtain(now, now,
                    MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).dispatchTouchEvent(cancelEvent);
            }
            cancelEvent.recycle();
            mChildrenCanceledTouch = true;
        }
    }

    public void openDrawer() {
        View drawerView = findDrawerView();
        View menuView = findMenuView();
        int width = menuView != null ? menuView.getWidth() : mMenuWidth;
        if (drawerView == null) {
            return;
        }

        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
        }
        if (mFirstLayout) {
            final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
            lp.offSet = 1.f;
            lp.knownOpen = false;
        } else {
            mDragHelper.smoothSlideViewTo(drawerView, width,
                    drawerView.getTop());
        }
        invalidate();
    }

    public boolean isDrawerOpen() {
        View drawerView = findDrawerView();
        if (drawerView == null) {
            return false;
        }
        return ((LayoutParams) drawerView.getLayoutParams()).knownOpen;
    }

    public void closeDrawer() {
        View drawerView = findDrawerView();
        if (drawerView == null) {
            return;
        }

        closeDrawer(drawerView);
        invalidate();
    }


    public void closeDrawer(View drawerView) {
        if (!isDrawerView(drawerView)) {
            throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
        }

        if (mFirstLayout) {
            final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
            lp.offSet = 0.f;
            lp.knownOpen = false;
        } else {
            mDragHelper.smoothSlideViewTo(drawerView, 0,
                    drawerView.getTop());
        }
        invalidate();
    }

    void updateDrawerState(@State int activeState, View drawer) {
        final int drawerState = mDragHelper.getViewDragState();

        final int state;
        if (drawerState == STATE_DRAGGING) {
            state = STATE_DRAGGING;
        } else if (drawerState == STATE_SETTLING) {
            state = STATE_SETTLING;
        } else {
            state = STATE_IDLE;
        }

        if (drawer != null && activeState == STATE_IDLE) {
            final LayoutParams lp = (LayoutParams) drawer.getLayoutParams();
            if (lp.offSet == 0) {
                dispatchOnDrawerClosed(drawer);
            } else if (lp.offSet == 1) {
                dispatchOnDrawerOpened(drawer);
            }
        }

        if (state != mDrawerState) {
            mDrawerState = state;

            if (mListener != null) {
                mListener.onDrawerStateChanged(state);
            }
        }
    }

    void dispatchOnDrawerClosed(View doorView) {
        final LayoutParams lp = (LayoutParams) doorView.getLayoutParams();
        if (lp.knownOpen) {
            lp.knownOpen = false;
            if (mListener != null) {
                mListener.onDrawerClosed(doorView);
            }
        }
    }

    void dispatchOnDrawerOpened(View doorView) {
        final LayoutParams lp = (LayoutParams) doorView.getLayoutParams();
        if (!lp.knownOpen) {
            lp.knownOpen = true;
            if (mListener != null) {
                mListener.onDrawerClosed(doorView);
            }
        }
    }

    void setDrawerViewOffset(View drawerView, float slideOffset) {
        final LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (slideOffset == lp.offSet) {
            return;
        }

        lp.offSet = slideOffset;
        dispatchOnDrawerSlide(drawerView, slideOffset);
    }

    float getDrawerViewOffset(View drawerView) {
        return ((LayoutParams) drawerView.getLayoutParams()).offSet;
    }

    void dispatchOnDrawerSlide(View drawerView, float slideOffset) {
        if (mListener != null) {
            mListener.onDrawerSlide(drawerView, slideOffset);
        }
    }

    void moveDrawerToOffset(View drawerView, float slideOffset) {
        final float oldOffset = getDrawerViewOffset(drawerView);
        final int width = drawerView.getWidth();
        final int oldPos = (int) (width * oldOffset);
        final int newPos = (int) (width * slideOffset);
        final int dx = newPos - oldPos;

        drawerView.offsetLeftAndRight(dx);
        setDrawerViewOffset(drawerView, slideOffset);
    }

    public View findMenuView() {
        View child;
        for (int i = 0; i < getChildCount(); i++) {
            child = getChildAt(i);
            LayoutParams params = (LayoutParams) (child.getLayoutParams());
            if (params.gravity != Gravity.CENTER) {
                return child;
            }
        }
        return null;
    }


    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    public void computeScroll() {
        final int childCount = getChildCount();
        float scrimOpacity = 0;
        for (int i = 0; i < childCount; i++) {
            final float onscreen = ((LayoutParams) getChildAt(i).getLayoutParams()).offSet;
            scrimOpacity = Math.max(scrimOpacity, onscreen);
        }
        mScrimOpacity = 1.0f - scrimOpacity;

        Log.d("Shader:",String.valueOf(scrimOpacity));

        // "|" used on purpose; both need to run.
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private class ViewDragCallback extends ViewDragHelper.Callback {
        private ViewDragHelper mDragger;

        public void setDragger(ViewDragHelper dragger) {
            mDragger = dragger;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            updateDrawerState(state, mDragger.getCapturedView());
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            float offset;
            View menu = findMenuView();
            if (menu == null) {
                offset = (float) left / mMenuWidth;
            } else {
                final int menuWidth = findMenuView().getWidth();
                offset = (float) (left) / menuWidth;
            }

            setDrawerViewOffset(changedView, offset);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final float offset = getDrawerViewOffset(releasedChild);
            View menuView = findMenuView();

            int menuWidth = menuView == null ? mMenuWidth : menuView.getWidth();

            int left = ((xvel > 0 || xvel == 0 && offset > 0.5f) ? menuWidth : 0);

            mDragHelper.settleCapturedViewAt(left, releasedChild.getTop());

            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            View menuView = findMenuView();
            int menuWidth = menuView == null ? mMenuWidth : menuView.getWidth();
            return isDrawerView(child) ? menuWidth : 0;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            View menuView = findMenuView();
            int menuWidth = menuView == null ? mMenuWidth : menuView.getWidth();
            return Math.max(0, Math.min(left, menuWidth));
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return child.getTop();
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return isDrawerView(child) && getDrawerLockMode(child) == LOCK_MODE_UNLOCKED;
        }

    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        public int gravity = Gravity.NO_GRAVITY;
        float offSet;
        boolean knownOpen;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            this.gravity = a.getInt(0, Gravity.NO_GRAVITY);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            this(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.gravity = source.gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }
    }
}
