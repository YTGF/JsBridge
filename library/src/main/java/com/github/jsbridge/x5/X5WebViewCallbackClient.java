package com.github.jsbridge.x5;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import com.tencent.smtt.sdk.WebViewCallbackClient;

/**
 * X5WebViewCallbackClient
 *
 * @author Edward
 * @email miansheng.zheng@inin88.com
 * @company 深圳市盈通数据服务股份有限公司
 * <p>
 * @description 描述
 * <p>
 * @date 2019-09-16
 */
public class X5WebViewCallbackClient implements WebViewCallbackClient {

    private X5NestedScrollWebView mWebView;

    public X5WebViewCallbackClient(X5NestedScrollWebView x5WebView) {
        this.mWebView = x5WebView;
    }

    public void invalidate() {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event, View view) {
        return mWebView.onTouchEvent(event);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public boolean overScrollBy(int deltaX, int deltaY, int scrollX,
                                int scrollY, int scrollRangeX, int scrollRangeY,
                                int maxOverScrollX, int maxOverScrollY,
                                boolean isTouchEvent, View view) {
        return mWebView.super_overScrollBy(deltaX, deltaY, scrollX, scrollY,
                scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
    }

    @Override
    public void computeScroll(View view) {
        mWebView.super_computeScroll();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY, View view) {
        mWebView.super_onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt, View view) {
        mWebView.super_onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev, View view) {
        return mWebView.super_dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev, View view) {
        return mWebView.super_onInterceptTouchEvent(ev);
    }
}
