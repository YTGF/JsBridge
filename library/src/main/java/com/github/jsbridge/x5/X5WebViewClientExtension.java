package com.github.jsbridge.x5;

import android.view.MotionEvent;
import android.view.View;

import com.tencent.smtt.export.external.extension.proxy.ProxyWebViewClientExtension;

/**
 * X5WebViewClientExtension
 *
 * @author Edward
 * @email miansheng.zheng@inin88.com
 * @company 深圳市盈通数据服务股份有限公司
 * <p>
 * @description 描述
 * <p>
 * @date 2019-09-16
 */
public class X5WebViewClientExtension extends ProxyWebViewClientExtension {

    private X5WebViewCallbackClient mCallbackClient;

    public X5WebViewClientExtension(X5WebViewCallbackClient x5WebViewCallbackClient) {
        this.mCallbackClient = x5WebViewCallbackClient;
    }

    public void invalidate() {
    }

    public void onReceivedViewSource(String data) {

    }

    public boolean onTouchEvent(MotionEvent event, View view) {
        return mCallbackClient.onTouchEvent(event, view);
    }

    // 1
    public boolean onInterceptTouchEvent(MotionEvent ev, View view) {
        return mCallbackClient.onInterceptTouchEvent(ev, view);
    }

    // 3
    public boolean dispatchTouchEvent(MotionEvent ev, View view) {
        return mCallbackClient.dispatchTouchEvent(ev, view);
    }

    // 4
    public boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY,
                                int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent, View view) {
        return mCallbackClient.overScrollBy(deltaX, deltaY, scrollX, scrollY,
                scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent, view);
    }

    // 5
    public void onScrollChanged(int l, int t, int oldl, int oldt, View view) {
        mCallbackClient.onScrollChanged(l, t, oldl, oldt, view);
    }

    // 6
    public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY, View view) {
        mCallbackClient.onOverScrolled(scrollX, scrollY, clampedX, clampedY, view);
    }

    // 7
    public void computeScroll(View view) {
        mCallbackClient.computeScroll(view);
    }
}
