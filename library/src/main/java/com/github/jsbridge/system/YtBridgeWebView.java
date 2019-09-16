package com.github.jsbridge.system;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.github.jsbridge.YtBridgeHandler;
import com.github.jsbridge.YtCallBackFunction;
import com.github.jsbridge.YtDefaultHandler;
import com.github.jsbridge.YtMessage;
import com.github.jsbridge.YtWebViewJavascriptBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class YtBridgeWebView extends SystemNestedScrollWebView implements YtWebViewJavascriptBridge {

	private final String TAG = "BridgeWebView";

	public static final String toLoadJs = "WebViewJavascriptBridge.js";
	Map<String, YtCallBackFunction> responseCallbacks = new HashMap<String, YtCallBackFunction>();
	YtBridgeHandler messageHandler;
	YtBridgeHandler defaultHandler = new YtDefaultHandler();

//	private Map<String, Object> messageKeys = new HashMap<String, Object>();

	private List<YtMessage> startupMessage = new ArrayList<YtMessage>();

	public List<YtMessage> getStartupMessage() {
		return startupMessage;
	}

	public void setStartupMessage(List<YtMessage> startupMessage) {
		this.startupMessage = startupMessage;
	}

	private long uniqueId = 0;

	public YtBridgeWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public YtBridgeWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public YtBridgeWebView(Context context) {
		super(context);
		init();
	}

	/**
	 *
	 * @param handler
	 *            default handler,handle messages send by js without assigned handler name,
     *            if js message has handler name, it will be handled by named handlers registered by native
	 */
	public void setDefaultHandler(YtBridgeHandler handler) {
       this.defaultHandler = handler;
	}

    private void init() {
		this.setVerticalScrollBarEnabled(false);
		this.setHorizontalScrollBarEnabled(false);
		this.getSettings().setJavaScriptEnabled(true);
		this.setWebViewClient(generateBridgeWebViewClient());
	}

    protected YtBridgeWebViewClient generateBridgeWebViewClient() {
        return new YtBridgeWebViewClient(this);
    }

    /**
     * 获取到CallBackFunction data执行调用并且从数据集移除
     * @param url
     */
	void handlerReturnData(String url) {
		String functionName = YtBridgeUtil.getFunctionFromReturnUrl(url);
		YtCallBackFunction f = responseCallbacks.get(functionName);
		String data = YtBridgeUtil.getDataFromReturnUrl(url);
		if (f != null) {
			f.onCallBack(data);
			responseCallbacks.remove(functionName);
			return;
		}
	}

	@Override
	public void send(String data) {
		send(data, null);
	}

	@Override
	public void send(String data, YtCallBackFunction responseCallback) {
		doSend(null, data, responseCallback);
	}

    /**
     * 保存message到消息队列
     * @param handlerName handlerName
     * @param data data
     * @param responseCallback CallBackFunction
     */
	private void doSend(String handlerName, String data, YtCallBackFunction responseCallback) {
		YtMessage m = new YtMessage();
		if (!TextUtils.isEmpty(data)) {
			m.setData(data);
		}
		if (responseCallback != null) {
			String callbackStr = String.format(YtBridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (YtBridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
			responseCallbacks.put(callbackStr, responseCallback);
			m.setCallbackId(callbackStr);
		}
		if (!TextUtils.isEmpty(handlerName)) {
			m.setHandlerName(handlerName);
		}
		queueMessage(m);
	}

    /**
     * list<message> != null 添加到消息集合否则分发消息
     * @param m Message
     */
	private void queueMessage(YtMessage m) {
		if (startupMessage != null) {
			startupMessage.add(m);
		} else {
			dispatchMessage(m);
		}
	}

    /**
     * 分发message 必须在主线程才分发成功
     * @param m Message
     */
	void dispatchMessage(YtMessage m) {
        String messageJson = m.toJson();
        //escape special characters for json string  为json字符串转义特殊字符
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
		messageJson = messageJson.replaceAll("(?<=[^\\\\])(\')", "\\\\\'");
        String javascriptCommand = String.format(YtBridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    }

    /**
     * 刷新消息队列
     */
	void flushMessageQueue() {
		if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
			loadUrl(YtBridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new YtCallBackFunction() {
				@Override
				public void onCallBack(String data) {
					// deserializeMessage 反序列化消息
					List<YtMessage> list = null;
					try {
						list = YtMessage.toArrayList(data);
					} catch (Exception e) {
                        e.printStackTrace();
						return;
					}
					if (list == null || list.size() == 0) {
						return;
					}
					for (int i = 0; i < list.size(); i++) {
						YtMessage m = list.get(i);
						String responseId = m.getResponseId();
						// 是否是response  CallBackFunction
						if (!TextUtils.isEmpty(responseId)) {
							YtCallBackFunction function = responseCallbacks.get(responseId);
							String responseData = m.getResponseData();
							function.onCallBack(responseData);
							responseCallbacks.remove(responseId);
						} else {
							YtCallBackFunction responseFunction = null;
							// if had callbackId 如果有回调Id
							final String callbackId = m.getCallbackId();
							if (!TextUtils.isEmpty(callbackId)) {
								responseFunction = new YtCallBackFunction() {
									@Override
									public void onCallBack(String data) {
										YtMessage responseMsg = new YtMessage();
										responseMsg.setResponseId(callbackId);
										responseMsg.setResponseData(data);
										queueMessage(responseMsg);
									}
								};
							} else {
								responseFunction = new YtCallBackFunction() {
									@Override
									public void onCallBack(String data) {
										// do nothing
									}
								};
							}
                            // BridgeHandler执行
							if (messageHandler != null) {
								messageHandler.handler(m.getHandlerName(), m.getData(), responseFunction);
							}
						}
					}
				}
			});
		}
	}


	public void loadUrl(String jsUrl, YtCallBackFunction returnCallback) {
		this.loadUrl(jsUrl);
        // 添加至 Map<String, CallBackFunction>
		responseCallbacks.put(YtBridgeUtil.parseFunctionName(jsUrl), returnCallback);
	}

	/**
	 * register handler,so that javascript can call it
	 * 注册处理程序,以便javascript调用它
	 * @param handler BridgeHandler
	 */
    public void registerHandler(YtBridgeHandler handler) {
        if (handler != null) {
            messageHandler = handler;
        }
    }

    /**
     * unregister handler
     */
    public void unregisterHandler() {
        messageHandler = null;
    }

	/**
	 * call javascript registered handler
	 * 调用javascript处理程序注册
     * @param handlerName handlerName
	 * @param data data
	 * @param callBack CallBackFunction
	 */
	public void callHandler(String handlerName, String data, YtCallBackFunction callBack) {
        doSend(handlerName, data, callBack);
	}
}
