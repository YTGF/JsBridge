package com.github.lzyzsd.jsbridge.example;

import android.app.Application;

import com.tencent.smtt.sdk.QbSdk;

/**
 * MainApplication
 *
 * @author Edward
 * @email miansheng.zheng@inin88.com
 * @company 深圳市盈通数据服务股份有限公司
 * <p>
 * @description 描述
 * <p>
 * @date 2019-04-09
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 搜集本地 tbs 内核信息并上报服务器, 服务器返回结果决定使用哪个内核
        // (有微信手Q则直接使用, 否则 wifi 下载, 都无则直接采用系统 WebView )
        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean arg0) {
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                System.out.println("X5 WebKit onViewInitFinished is " + arg0);
            }

            @Override
            public void onCoreInitFinished() {

            }
        };
        // x5内核初始化接口
        QbSdk.initX5Environment(this, cb);
    }
}
