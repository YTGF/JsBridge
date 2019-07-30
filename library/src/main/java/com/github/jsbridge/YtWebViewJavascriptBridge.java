package com.github.jsbridge;


public interface YtWebViewJavascriptBridge {
	void send(String data);
	void send(String data, YtCallBackFunction responseCallback);
}
