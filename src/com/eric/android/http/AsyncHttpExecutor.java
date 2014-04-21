package com.eric.android.http;

import java.io.UnsupportedEncodingException;

import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;

/**
 * Copyright (c) 2014
 * All right reserved.
 * 
 * @author ji.jiaxiang
 *
 * 2014/04/21 First Release
 */
public class AsyncHttpExecutor {

	@SuppressWarnings("unused")
	private static final String TAG = AsyncHttpExecutor.class.getSimpleName();

	private ExecutorService mThreadPool;

	private static AsyncHttpExecutor sInstance;
	
	private PersistenceCookieStore mCookieStore;

	private AsyncHttpExecutor(Context ctx) {
		init(ctx);
	}

	private void init(Context ctx) {
		mThreadPool = Executors.newCachedThreadPool();
		mCookieStore = new PersistenceCookieStore(ctx);
	}

	public synchronized static AsyncHttpExecutor getInstance(Context ctx) {
		if (null == sInstance) {
			sInstance = new AsyncHttpExecutor(ctx);
		}

		return sInstance;
	}
	
	private URI buildURI(String uriStr) {
		return buildURI(uriStr, null);
	}

	private URI buildURI(String uriStr, Map<String, Object> params){

		if (params != null) {
			StringBuilder sb = new StringBuilder(uriStr);

			sb.append((!uriStr.contains("?")) ? '?' : '&');

			for (Entry<String, Object> entry : params.entrySet()) {
				
				try {
				sb.append(URLEncoder.encode(entry.getKey().toString(),"UTF-8"))
					.append('=')
					.append(URLEncoder.encode(entry.getValue().toString(),"UTF-8"))
					.append('&');
				} catch(UnsupportedEncodingException ex){
					ex.printStackTrace();
				}
			}

			return URI.create(sb.toString().substring(0, sb.length() - 1));
		}


			return URI.create(uriStr);
	}

	public void get(String uri, ResponseHandlerInterface rhi) {
		get(uri, rhi, null, null);
	}

	public void get(String uri, ResponseHandlerInterface rhi,
			Map<String, Object> params) {
		get(uri, rhi, params, null);
	}

	public void get(String uriStr, ResponseHandlerInterface rhi,
			Map<String, Object> params,
			Map<String, List<String>> headers) {

		URI uri = buildURI(uriStr, params);
		AsyncHttpRequest request = new AsyncHttpRequest(uri,
				AsyncHttpRequest.GET, rhi, params, headers);
		request.setCookieEnable(true);
		request.setCookieStore(mCookieStore);
		mThreadPool.execute(request);
	}

	public void post(String uriStr, ResponseHandlerInterface rhi) {
		post(uriStr, rhi, null, null);
	}

	public void post(String uriStr, ResponseHandlerInterface rhi,
			Map<String, Object> params) {
		post(uriStr, rhi, params, null);
	}

	public void post(String uriStr, ResponseHandlerInterface rhi,
			Map<String, Object> params,
			Map<String, List<String>> headers) {
		
		URI uri = buildURI(uriStr);
		AsyncHttpRequest request = new AsyncHttpRequest(uri,
				AsyncHttpRequest.POST, rhi, params, headers);
		request.setCookieEnable(true);
		request.setCookieStore(mCookieStore);
		mThreadPool.execute(request);
	}

	
}
