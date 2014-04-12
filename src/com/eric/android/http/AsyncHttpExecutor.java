package com.eric.android.http;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncHttpExecutor {

	@SuppressWarnings("unused")
	private static final String TAG = AsyncHttpExecutor.class.getSimpleName();

	private ExecutorService mThreadPool;

	private static AsyncHttpExecutor sInstance;

	private AsyncHttpExecutor() {
		init();
	}
	

	private void init() {
		mThreadPool = Executors.newCachedThreadPool();
	}

	public synchronized static AsyncHttpExecutor getInstance() {
		if (null == sInstance) {
			sInstance = new AsyncHttpExecutor();
		}

		return sInstance;
	}

	public void get(String uri, ResponseHandlerInterface rhi) {
		get(uri, rhi, null, null);
	}

	public void get(String uri, ResponseHandlerInterface rhi,
			HashMap<String, String> params) {
		get(uri, rhi, params, null);
	}

	public void get(String uriStr, ResponseHandlerInterface rhi,
			HashMap<String, String> params,
			HashMap<String, List<String>> headers) {

		URI uri = buildGetURI(uriStr, params);
		AsyncHttpRequest request = new AsyncHttpRequest(uri,
				AsyncHttpRequest.GET, rhi);
		mThreadPool.execute(request);
	}

	public void post() {

	}

	private URI buildGetURI(String uriStr, HashMap<String, String> params) {

		if (params != null) {
			StringBuilder sb = new StringBuilder(uriStr);

			sb.append((uriStr.contains("?")) ? '?' : '&');

			for (Entry<String, String> entry : params.entrySet()) {
				sb.append(entry.getKey()).append('=').append(entry.getValue())
						.append('=');
			}

			return URI.create(sb.toString().substring(0, sb.length() - 1));
		}

		return URI.create(uriStr);
	}
}
