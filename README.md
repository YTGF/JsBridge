#JsBridge
-----
##### 集成指南详见: library/README.md

##### 此库 Fork 来源为 https://github.com/lzyzsd/JsBridge , 相对修改历史:
1. 修改日期 2018-08-10, 内容: 集成 腾讯 X5 WebView 功能库, 替换默认继承的 WebView 及相关 API 类, 详见 c676d31297 此记录修改.
	- X5 WebView SDK 库: https://x5.tencent.com/tbs/sdk.html 的 Android SDK（完整版 v3.6.0.1315, 2018-07-18）版本; 
2. 修改日期 2019-04-10, 内容: 截止目前修复部分 Bug 及优化 JsBridge registerHandler 方式, 更新 X5 WebView SdK 库版本为当前最新 tbs_sdk_thirdapp_v4.3.0.1020_43633_sharewithdownload_withoutGame_obfs_20190111_105200.jar 版本;
3. 修改日期 2019-08-05, 内容: 为避免同名冲突, 该库所有类添加以 Yt 前缀重命名, 同时剥离 X5 WebView Jar 库及 SO 库(故集成当前版本时还依赖 yingt.external.tencent:tbs:1.0.0, 或直接集成腾讯 X5 更高的支持版本, 详见 example/build.gradle 示例);
4. .................

-----

inspired and modified from [this](https://github.com/jacin1/JsBridge) and wechat jsBridge file, with some bugs fix and feature enhancement.

This project make a bridge between Java and JavaScript.

It provides safe and convenient way to call Java code from js and call js code from java.

## Demo
![JsBridge Demo](https://raw.githubusercontent.com/lzyzsd/JsBridge/master/JsBridge.gif)

## Usage

## JitPack.io

I strongly recommend https://jitpack.io

```groovy
repositories {
    // ...
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.lzyzsd:jsbridge:1.0.4'
}
```

## Use it in Java

add com.github.lzyzsd.jsbridge.BridgeWebView to your layout, it is inherited from WebView.

### Register a Java handler function so that js can call

```java

    webView.registerHandler("submitFromWeb", new BridgeHandler() {
        @Override
        public void handler(String data, CallBackFunction function) {
            Log.i(TAG, "handler = submitFromWeb, data from web = " + data);
            function.onCallBack("submitFromWeb exe, response data from Java");
        }
    });

```

js can call this Java handler method "submitFromWeb" through:

```javascript

    WebViewJavascriptBridge.callHandler(
        'submitFromWeb'
        , {'param': str1}
        , function(responseData) {
            document.getElementById("show").innerHTML = "send get responseData from java, data = " + responseData
        }
    );

```

You can set a default handler in Java, so that js can send message to Java without assigned handlerName

```java

    webView.setDefaultHandler(new DefaultHandler());

```

```javascript

    window.WebViewJavascriptBridge.send(
        data
        , function(responseData) {
            document.getElementById("show").innerHTML = "repsonseData from java, data = " + responseData
        }
    );

```

### Register a JavaScript handler function so that Java can call

```javascript

    WebViewJavascriptBridge.registerHandler("functionInJs", function(data, responseCallback) {
        document.getElementById("show").innerHTML = ("data from Java: = " + data);
        var responseData = "Javascript Says Right back aka!";
        responseCallback(responseData);
    });

```

Java can call this js handler function "functionInJs" through:

```java

    webView.callHandler("functionInJs", new Gson().toJson(user), new CallBackFunction() {
        @Override
        public void onCallBack(String data) {

        }
    });

```
You can also define a default handler use init method, so that Java can send message to js without assigned handlerName

for example:

```javascript

    bridge.init(function(message, responseCallback) {
        console.log('JS got a message', message);
        var data = {
            'Javascript Responds': 'Wee!'
        };
        console.log('JS responding with', data);
        responseCallback(data);
    });

```

```java
    webView.send("hello");
```

will print 'JS got a message hello' and 'JS responding with' in webview console.

## Notice

This lib will inject a WebViewJavascriptBridge Object to window object.
So in your js, before use WebViewJavascriptBridge, you must detect if WebViewJavascriptBridge exist.
If WebViewJavascriptBridge does not exit, you can listen to WebViewJavascriptBridgeReady event, as the blow code shows:

```javascript

    if (window.WebViewJavascriptBridge) {
        //do your work here
    } else {
        document.addEventListener(
            'WebViewJavascriptBridgeReady'
            , function() {
                //do your work here
            },
            false
        );
    }

```

## License

This project is licensed under the terms of the MIT license.


