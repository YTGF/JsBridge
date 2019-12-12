package com.github.jsbridge.x5;

import android.content.Context;
import android.support.v4.view.NestedScrollingChild2;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import com.tencent.smtt.export.external.extension.interfaces.IX5WebViewExtension;
import com.tencent.smtt.sdk.WebView;

/**
 * X5NestedScrollWebView
 *
 * @author Edward
 * @email miansheng.zheng@inin88.com
 * @company 深圳市盈通数据服务股份有限公司
 * <p>
 * @description 针对 X5 WebView 的 NestedScroll 包装类, 跟 SystemNestedScrollWebView 基本一致,
 * 仅 super.onTouchEvent 与 super.super_onTouchEvent 不同, 需结合 {@link X5WebViewCallbackClient} 使用.
 * <p>
 * @date 2019-09-17
 */
public class X5NestedScrollWebView extends WebView implements NestedScrollingChild2 {

    public static final String TAG = "X5WebView";

    private static final int INVALID_POINTER = -1;

    private int mLastMotionY;
    private boolean mIsBeingDragged = false;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    private int mActivePointerId = INVALID_POINTER;

    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];

    private int mNestedYOffset;
    private int mLastScrollY;

    private final NestedScrollingChildHelper mChildHelper;

    public X5NestedScrollWebView(Context context) {
        this(context, null);
    }

    public X5NestedScrollWebView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.webViewStyle);
    }

    public X5NestedScrollWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mChildHelper = new NestedScrollingChildHelper(this);

        mScroller = new Scroller(getContext());

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        // slop缩小一点，需要提前比WebView感知滚动
        mTouchSlop = (int) (configuration.getScaledTouchSlop() * 0.9f);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);

        if (enabled) settingNestedScroll();
    }

    /**
     * 设置处理 NestedScroll 滚动事件
     */
    public void settingNestedScroll() {
        // 重写 TBS WebView 的屏幕事件: https://x5.tencent.com/tbs/technical.html#/detail/sdk/1/8fcbfe60-2034-4e0a-ac58-74c47494f870
        X5WebViewCallbackClient x5WebViewCallbackClient = new X5WebViewCallbackClient(this);
        this.setWebViewCallbackClient(x5WebViewCallbackClient);

        IX5WebViewExtension x5WebViewExtension = this.getX5WebViewExtension();
        if (x5WebViewExtension != null) {
            x5WebViewExtension.setWebViewClientExtension(new X5WebViewClientExtension(x5WebViewCallbackClient));
        }
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return startNestedScroll(axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void stopNestedScroll() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
                                        int dxUnconsumed, int dyUnconsumed,
                                        int[] offsetInWindow) {
        return dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed,
                offsetInWindow, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    // NestedScrollingChild2

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return mChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        mChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return mChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
                                        int dxUnconsumed, int dyUnconsumed,
                                        int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed,
                offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow, int type) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    // Touch

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isNestedScrollingEnabled()) {
            return super_onTouchEvent(ev);
        }

        initVelocityTrackerIfNotExists();

        final int actionMasked = ev.getActionMasked();

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }

        MotionEvent vtev = MotionEvent.obtain(ev);
        vtev.offsetLocation(0, mNestedYOffset);

        boolean callSuper = true;

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                if ((mIsBeingDragged = !mScroller.isFinished())) {
                    abortAnimatedScroll();
                }

                mLastMotionY = (int) (ev.getY() + 0.5f);
                mActivePointerId = ev.getPointerId(0);
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                break;
            }

            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    break;
                }

                // nested scroll parent先滚动
                final int y = (int) (ev.getY(activePointerIndex) + 0.5f);
                int deltaY = mLastMotionY - y;
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset,
                        ViewCompat.TYPE_TOUCH)) {
                    deltaY -= mScrollConsumed[1];
                    vtev.offsetLocation(0, mScrollOffset[1]);
                    // Updated the nested offsets
                    mNestedYOffset += mScrollOffset[1];
                }

                // parent没有消耗的滚动，再用来判断自己能不能滚动
                if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
                    mIsBeingDragged = true;
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                    // 因为这里实际上会比WebView原来的更早触发drag，所以需要手动取消click和long click事件
                    cancelSuperTouchEvent(ev);
                }

                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    mLastMotionY = y - mScrollOffset[1];

                    final int oldY = getWebScrollY();

                    scroll(deltaY);

                    final int scrolledDeltaY = getWebScrollY() - oldY;
                    final int unconsumedY = deltaY - scrolledDeltaY;

                    mScrollConsumed[1] = 0;

                    dispatchNestedScroll(0, scrolledDeltaY,
                            0, unconsumedY, mScrollOffset,
                            ViewCompat.TYPE_TOUCH);

                    vtev.offsetLocation(0, mScrollOffset[1]);

                    mLastMotionY -= mScrollOffset[1];
                    mNestedYOffset += mScrollOffset[1];

                    callSuper = false;
                }
                break;

            case MotionEvent.ACTION_UP:
                mVelocityTracker.addMovement(vtev);
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);
                if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                    if (!dispatchNestedPreFling(0, -initialVelocity)) {
                        dispatchNestedFling(0, -initialVelocity, true);
                        mScroller.fling(getWebScrollX(), getWebScrollY(),
                                0, -initialVelocity,
                                0, 0,
                                Integer.MIN_VALUE, Integer.MAX_VALUE);
                        runAnimatedScroll(true);
                        // 因为这里实际上会比WebView原来的更早触发drag，所以需要手动取消click和long click事件
                        cancelSuperTouchEvent(ev);
                    }
                }
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;

            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER;
                endDrag();
                break;

            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mLastMotionY = (int) (ev.getY(index) + 0.5f);
                mActivePointerId = ev.getPointerId(index);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(vtev);
        }

        vtev.recycle();

        return (callSuper ? super_onTouchEvent(ev) : true);
    }

    @Override
    public void computeScroll() {
        if (!isNestedScrollingEnabled()) {
            super_computeScroll();
            return;
        }

        if (mScroller.isFinished()) {
            return;
        }

        mScroller.computeScrollOffset();
        final int y = mScroller.getCurrY();
        int unconsumed = y - mLastScrollY;
        mLastScrollY = y;

        mScrollConsumed[1] = 0;
        if (dispatchNestedPreScroll(0, unconsumed,
                mScrollConsumed, null,
                ViewCompat.TYPE_NON_TOUCH)) {
            unconsumed -= mScrollConsumed[1];
        }

        if (unconsumed != 0) {
            final int oldY = getWebScrollY();

            scroll(unconsumed);

            final int scrolledByMe = getWebScrollY() - oldY;
            unconsumed -= scrolledByMe;

            // Nested Scrolling Post Pass
            mScrollConsumed[1] = 0;
            if (!dispatchNestedScroll(0, scrolledByMe,
                    0, unconsumed, mScrollOffset,
                    ViewCompat.TYPE_NON_TOUCH)) {
                if (mScroller.getFinalY() == 0) {
                    abortAnimatedScroll();
                }
            }
        }

        if (!mScroller.isFinished()) {
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            abortAnimatedScroll();
        }
    }

    // 触发滚动，内部嵌套div的网页不适用
    private void scroll(int deltaY) {
        final int range = getVerticalScrollRange();
        final int oldY = getWebScrollY();

        if (oldY + deltaY < 0) {
            getView().scrollTo(getWebScrollX(), 0);
        } else if (oldY + deltaY > range) {
            getView().scrollTo(getWebScrollX(), range);
        } else {
            getView().scrollBy(getWebScrollX(), deltaY);
        }
    }

    // 滚动距离，内部嵌套div的网页不适用
    private int getVerticalScrollRange() {
        return computeVerticalScrollRange() - getHeight();
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private void runAnimatedScroll(boolean participateInNestedScrolling) {
        if (participateInNestedScrolling) {
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
        }
        mLastScrollY = getWebScrollY();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void abortAnimatedScroll() {
        mScroller.abortAnimation();
        stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;

        recycleVelocityTracker();
        stopNestedScroll(ViewCompat.TYPE_TOUCH);
    }

    private void cancelSuperTouchEvent(MotionEvent origin) {
        final MotionEvent cancel = MotionEvent.obtain(origin);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super_onTouchEvent(cancel);
        cancel.recycle();
    }
}
