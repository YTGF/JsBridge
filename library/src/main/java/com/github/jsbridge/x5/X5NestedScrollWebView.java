package com.github.jsbridge.x5;

import android.content.Context;
import android.os.SystemClock;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import com.tencent.smtt.sdk.WebView;

import java.util.Arrays;

import static android.view.MotionEvent.INVALID_POINTER_ID;

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
public class X5NestedScrollWebView extends WebView implements NestedScrollingChild {

    public static final String TAG = "NestedScrollWebView";

    private float mDownY;
    private float mLastY;

    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];

    private int mNestedYOffset;

    private int mPointerId = INVALID_POINTER_ID;

    private boolean mIsBeingDragged = false;
    private boolean mIsNestScrolling = false;
    private boolean mHasNestedScroll = false;

    private NestedScrollingChildHelper mChildHelper;

    private VelocityTracker mTracker;

    private int mTouchSlop;

    private int mMaxVelocity;
    private int mMinVelocity;

    public X5NestedScrollWebView(Context context) {
        this(context, null);
    }

    public X5NestedScrollWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public X5NestedScrollWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mChildHelper = new NestedScrollingChildHelper(this);

        setNestedScrollingEnabled(true);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMaxVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        boolean result = false;

        final int actionIndex = event.getActionIndex();
        final int action = event.getActionMasked();

        //专门用于计算速度，每次加上mNestedYOffset获取真实坐标
        MotionEvent velocityEvent = MotionEvent.obtain(event);
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }
        velocityEvent.offsetLocation(0, mNestedYOffset);

        initTracker();
        boolean isAddToTracker = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:

                mPointerId = event.getPointerId(actionIndex);

                mIsBeingDragged = false;
                mIsNestScrolling = false;
                mHasNestedScroll = false;

                mDownY = event.getY(actionIndex);
                mLastY = mDownY;

                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

                result = super.super_onTouchEvent(event);

                break;

            case MotionEvent.ACTION_POINTER_DOWN:

                // 将新落下来那根手指作为活动手指
                mPointerId = event.getPointerId(actionIndex);
                mLastY = event.getY(actionIndex);
                result = super.super_onTouchEvent(event);

                break;

            case MotionEvent.ACTION_POINTER_UP:

                // 如果松开的是活动手指
                if (mPointerId == event.getPointerId(actionIndex)) {
                    final int newIndex = actionIndex == 0 ? 1 : 0;
                    mPointerId = event.getPointerId(newIndex);
                    mLastY = event.getY(newIndex);
                }
                result = super.super_onTouchEvent(event);

                break;

            case MotionEvent.ACTION_MOVE:

                int index = event.findPointerIndex(mPointerId);
                if (index == -1) {
                    Log.d(TAG, "Invalid pointer id : " + mPointerId + " in onTouchEvent.");
                    break;
                }

                float newY = event.getY(index);
                float moveY = mDownY - newY;

                if (!mIsBeingDragged && Math.abs(moveY) > mTouchSlop) {
                    mIsBeingDragged = true;
                }

                int deltaY = (int) (mLastY - newY);

                //嵌套滑动时mLastY需要加上嵌套滑动距离，否则deltaY==0
                mLastY = newY;

                if (mIsBeingDragged) {
                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                        //嵌套滑动
                        deltaY -= mScrollConsumed[1];
                        mLastY -= mScrollOffset[1];
                        mNestedYOffset += mScrollOffset[1];
                    }

                    Log.d(TAG, "mScrollConsumed = " + Arrays.toString(mScrollConsumed) + ", mScrollOffset = " + Arrays.toString(mScrollOffset));
                    if (mScrollConsumed[1] == 0 && mScrollOffset[1] == 0) {//WebView内部滑动
                        Log.d(TAG, "WebView 内部处理滑动事件");
                        if (mIsNestScrolling) {
                            event.setAction(MotionEvent.ACTION_DOWN);
                            mIsNestScrolling = false;
                        }
                        result = super.super_onTouchEvent(event);
                    } else {//嵌套滑动
                        Log.d(TAG, "嵌套滑动事件");
                        if (!mIsNestScrolling) {
                            long now = SystemClock.uptimeMillis();
                            super.super_onTouchEvent(MotionEvent.obtain(now, now,
                                    MotionEvent.ACTION_CANCEL,
                                    0, 0,
                                    -1));
                            mIsNestScrolling = true;
                        }
                        mHasNestedScroll = true;
                    }
                } else {
                    result = super.super_onTouchEvent(event);
                }

                break;

            case MotionEvent.ACTION_UP:

                mTracker.addMovement(velocityEvent);
                isAddToTracker = true;
                mTracker.computeCurrentVelocity(1000, mMaxVelocity);

                if (mIsBeingDragged) {
                    checkIfNeedFling();
                }

            case MotionEvent.ACTION_CANCEL:

                mPointerId = INVALID_POINTER_ID;

                //WebView内部滚动时才传递UP、CANCEL事件
                if (!mIsNestScrolling) {
                    if (mHasNestedScroll) {
                        event.setAction(MotionEvent.ACTION_CANCEL);
                    }
                    result = super.super_onTouchEvent(event);
                }

                mIsBeingDragged = false;
                mIsNestScrolling = false;
                mHasNestedScroll = false;

                stopNestedScroll();

                resetTracker();

                break;
        }

        if (!isAddToTracker) {
            mTracker.addMovement(velocityEvent);
        }

        velocityEvent.recycle();

        return result;
    }

    private void checkIfNeedFling() {
        int velocityY = (int) -mTracker.getYVelocity(mPointerId);

        if (Math.abs(velocityY) >= mMinVelocity) {
            if (!dispatchNestedPreFling(0, velocityY)) {
                dispatchNestedFling(0, velocityY, true);
            }
        }
    }

    private void initTracker() {
        if (mTracker == null) {
            mTracker = VelocityTracker.obtain();
        }
    }

    private void resetTracker() {
        if (mTracker != null) {
            mTracker.clear();
        }
    }

    // NestedScrollingChild
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
}