package com.eric.android.http;

import java.lang.ref.WeakReference;

public class RequestHandle {
	//A wear refererenc of AcyncHttpRequest
	private WeakReference<AsyncHttpRequest> mRequestRef;
	
	public RequestHandle(AsyncHttpRequest request) {
		mRequestRef = new WeakReference<AsyncHttpRequest>(request);
		
	}
	
	public void cancel() {
		
		AsyncHttpRequest request = mRequestRef.get();
		if(request != null) {
			request.cancel();
		}
	}
	
	public boolean isDone() {
		AsyncHttpRequest request = mRequestRef.get();
		return (request != null) ? request.isDone() : true;		
	}

}
