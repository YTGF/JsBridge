package com.github.jsbridge;

public class YtDefaultHandler implements YtBridgeHandler {

	String TAG = "DefaultHandler";

	@Override
	public void handler(String name, String data, YtCallBackFunction function) {
		if(function != null){
			function.onCallBack("DefaultHandler response data");
		}
	}
}
