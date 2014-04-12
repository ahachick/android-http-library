package com.eric.android.http;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.eric.android.util.Logger;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public abstract class AbstractResponseHandler 
					implements ResponseHandlerInterface{
	
	
	private static final String TAG = AbstractResponseHandler.class.getSimpleName();

	protected static final int SUCCESS_MESSAGE = 0;
    protected static final int FAILURE_MESSAGE = 1;
    protected static final int START_MESSAGE = 2;
    protected static final int FINISH_MESSAGE = 3;
    protected static final int PROGRESS_MESSAGE = 4;
    protected static final int RETRY_MESSAGE = 5;
    protected static final int CANCEL_MESSAGE = 6;
	
	private Handler mHandler;
	private Map<String, List<String>> mHeaders;
	
	
	public AbstractResponseHandler() {
		
		mHandler = new InternalHandler(this);
		
		postRunnable(null);
	
	}
	
	protected void postRunnable(Runnable r){
		Looper looper = Looper.myLooper();
		
		boolean missingLooper = null == looper;
		if(missingLooper) {
			Looper.prepare();
		}
		
		if(null != r){
			mHandler.post(r);
		}
		
		if(missingLooper) {
			Looper.loop();
		}
	}
	
	@Override
	public void sendStartMessage() {
		
		sendMessage(START_MESSAGE);
	}

	@Override
	public void sendCancelMessage() {
		
	}

	@Override
	public void sendFinishMessage() {
		sendMessage(FINISH_MESSAGE);
	}

	@Override
	public void sendSuccessMessage(int statusCode,
			Map<String, List<String>> headers, byte[] responseBody) {
		sendMessage(SUCCESS_MESSAGE, statusCode, headers, responseBody);
	}

	@Override
	public void sendFailureMessage(int statusCode,
			Map<String, List<String>> headers, byte[] responseBody,
			Throwable t) {
		sendMessage(FAILURE_MESSAGE, statusCode, headers, responseBody, t);
	}
	

	@Override
	public void sentRetryMessage(int retryNo) {
		
	}

	@Override
	public URI getRequestURI() {
		return null;
	}

	@Override
	public Map<String, List<String>> getRequestHeaders() {
		return mHeaders;
	}
	
	private void sendMessage(int what, Object... objects) {
		
		Message msg = mHandler.obtainMessage(what, objects);
		mHandler.sendMessage(msg);
	}

	@Override
	public void sendResponseMessage(int statusCode,
			Map<String, List<String>> headers, byte[] responseBody) {
		if(statusCode >= 300) {
			sendFailureMessage(statusCode,headers, responseBody, null);
		} else {
			sendSuccessMessage(statusCode, headers, responseBody);
		}
	}
	
	private static class InternalHandler extends Handler{
		
		private AbstractResponseHandler mResponse;
		
		public InternalHandler(AbstractResponseHandler response) {
			mResponse = response;
		}
		
		@Override
		public void handleMessage(Message msg) {
			if(null != mResponse) {
				mResponse.handleResponseMessage(msg);
			}
			
		}
	}

	@SuppressWarnings("unchecked")
	public void handleResponseMessage(Message msg) {
		
		Log.d(TAG, "MessageNo:" + msg.what);
		
		Object[] objs = null;
		switch(msg.what) {
		case START_MESSAGE :
			onStart();
			break;
		case FINISH_MESSAGE :
			onFinish();
			break;
		case SUCCESS_MESSAGE :
			objs = (Object[]) msg.obj;
			onSuccess((Integer)objs[0],(Map<String, List<String>>)objs[1], (byte[])objs[2]);
			break;
			
		case FAILURE_MESSAGE :
			objs = (Object[]) msg.obj;
			onFailure((Integer)objs[0],(Map<String, List<String>>)objs[1], (byte[])objs[2], (Throwable)objs[3]);
			break;
			
		case CANCEL_MESSAGE :
			break;
		case PROGRESS_MESSAGE :
			break;
		case RETRY_MESSAGE :
			break;
		default:
				
		}
	}
	
	protected void onStart() {};
	protected void onFinish() {};
	
	protected abstract void onSuccess(int statusCode, Map<String, List<String>> headers, byte[] body);
	
	protected abstract void onFailure(int statusCode, Map<String, List<String>> headers, byte[] body, Throwable t);
}
