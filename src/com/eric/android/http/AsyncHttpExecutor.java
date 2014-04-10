package com.eric.android.http;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncHttpExecutor {
	
	@SuppressWarnings("unused")
	private static final String TAG = AsyncHttpExecutor.class.getSimpleName();
	
	private static ExecutorService sThreadPool;
	
	private static AsyncHttpExecutor sInstance;
	
	
	private AsyncHttpExecutor() {
		init();
	}
	
	private void init() {
		sThreadPool = Executors.newCachedThreadPool();
	}
	
	public synchronized static AsyncHttpExecutor getInstance() {
		if( null == sInstance) {
			sInstance = new AsyncHttpExecutor();
		}
		
		return sInstance;
	}

	
}
