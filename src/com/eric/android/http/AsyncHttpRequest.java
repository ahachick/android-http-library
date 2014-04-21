package com.eric.android.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.eric.android.util.Logger;

/**
 * Copyright (c) 2014 All right reserved.
 * 
 * @author ji.jiaxiang
 * 
 *         2014/04/21 First Release
 */

public class AsyncHttpRequest implements Runnable {

	private static final String TAG = AsyncHttpRequest.class.getSimpleName();

	public static final int GET = 1;
	public static final int POST = 2;
	// public static final int DELETE = 3;
	// public static final int UPDATE = 4;

	private URI mUri;

	private int mRequestType;

	private static final int DEFAULT_RETRY_COUNT = 3;
	private static final int DEFAULT_SOCKET_TIMEOUT = 15 * 1000;
	private static final int DEFAULT_READ_TIMEOUT = 20 * 1000;

	private int mSocketTimeout = DEFAULT_SOCKET_TIMEOUT;
	private int mReadTimeout = DEFAULT_READ_TIMEOUT;
	private int mRetryCount = DEFAULT_RETRY_COUNT;
	private boolean isCancelled = false;
	private boolean isFinished = false;
	private ResponseHandlerInterface mResponseHandler;
	private Map<String, Object> mRequestParams;
	private Map<String, List<String>> mHeaders;

	private static final Map<String, List<String>> DEFAULT_HEADERS;

	private boolean mCookieEnable = true;
	private PersistenceCookieStore mCookieStore;

	static {
		DEFAULT_HEADERS = new HashMap<String, List<String>>();
		// add some header

	}

	public AsyncHttpRequest(URI uri, int requestType,
			ResponseHandlerInterface rh) {
		init(uri, requestType, rh, null, null);
	}

	public AsyncHttpRequest(URI uri, int requestType,
			ResponseHandlerInterface rh, Map<String, Object> requestParams) {
		init(uri, requestType, rh, requestParams, null);
	}

	public AsyncHttpRequest(URI uri, int requestType,
			ResponseHandlerInterface rh, Map<String, Object> requestParams,
			Map<String, List<String>> headers) {
		init(uri, requestType, rh, requestParams, headers);
	}

	private void init(URI uri, int requestType, ResponseHandlerInterface rh,
			Map<String, Object> requestParams, Map<String, List<String>> headers) {

		mUri = uri;
		mRequestType = requestType;
		mResponseHandler = rh;
		mRequestParams = requestParams;
		mHeaders = (headers == null) ? headers : DEFAULT_HEADERS;

	}

	public void setCookieEnable(boolean enable) {
		mCookieEnable = enable;
	}

	public void setCookieStore(PersistenceCookieStore store) {
		mCookieStore = store;
	}

	@Override
	public void run() {

		if (isCancelled())
			return;

		if (mResponseHandler != null) {
			mResponseHandler.sendStartMessage();
		}

		if (isCancelled())
			return;

		try {
			makeRequestWithRetries();
		} catch (IOException e) {

			if (!isCancelled() && mResponseHandler != null) {
				mResponseHandler.sendFailureMessage(0, null, null, e);
			}
			e.printStackTrace();
		}

		if (isCancelled())
			return;

		if (mResponseHandler != null) {
			mResponseHandler.sendFinishMessage();
		}

		isFinished = true;
	}

	private void makeRequest() throws IOException {
		HttpURLConnection conn = null;
		BufferedOutputStream output = null;
		BufferedInputStream input = null;
		try {
			URL url = mUri.toURL();
			Logger.debug(TAG, "URI:" + url.toString());
			conn = (HttpURLConnection) url.openConnection();

			if (mHeaders != null) {
				for (String key : mHeaders.keySet()) {
					for (String value : mHeaders.get(key))
						conn.addRequestProperty(key, value);
				}
			}
			// set "Cookie" to request header
			if (mCookieEnable && null != mCookieStore) {
				String cookieStr = mCookieStore.getCookieString();
				if (cookieStr != null) {
					Logger.debug(TAG, cookieStr);
					conn.addRequestProperty("Cookie", cookieStr);
				}
			}

			// set socket timeout;
			conn.setConnectTimeout(mSocketTimeout);
			// set read timeout
			conn.setReadTimeout(mReadTimeout);
			// disable http cache;
			conn.setUseCaches(false);

			if (POST == mRequestType) {
				conn.setDoOutput(true);
				byte[] requestBody = mRequestParams == null ? new byte[] {}
						: paramsToString(mRequestParams).getBytes();
				conn.setFixedLengthStreamingMode(requestBody.length);

				output = new BufferedOutputStream(conn.getOutputStream());
				output.write(requestBody);
				output.flush();
				output.close();
			}

			// get "Set-Cookie" property from http response
			if (null != mCookieStore) {
				Map<String, List<String>> headers = conn.getHeaderFields();
				if (null != headers) {
					StringBuilder sb = new StringBuilder();
					for (Entry<String, List<String>> entry : headers.entrySet()) {
						if (entry.getKey() != null && entry.getKey().equals("Set-Cookie")) {
							for (String value : entry.getValue()) {
								sb.append(value).append(";");
							}
							String cookieStr = sb.substring(0, sb.length() - 1);
							Logger.debug(TAG, cookieStr);
							mCookieStore.addCookieString(cookieStr);
						}
					}
				}
			}

			Logger.debug(TAG, "ContentLength:" + conn.getContentLength());

			input = new BufferedInputStream(conn.getInputStream());

			byte[] body = readStream(input);

			input.close();
			if (mResponseHandler != null) {
				int statusCode = conn.getResponseCode();
				mResponseHandler.sendResponseMessage(statusCode,
						conn.getHeaderFields(), body);
			}

			return;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} finally {
			if (null != conn) {
				conn.disconnect();
			}

			if (null != input) {
				try {
					input.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (null != output) {
				try {
					output.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	}

	private void makeRequestWithRetries() throws IOException {

		IOException cause = null;

		int retryCount = 0;

		try {

			while (retryCount != mRetryCount) {
				try {
					makeRequest();
					return;
				} catch (UnknownHostException e) {
					cause = e;
				} catch (SocketTimeoutException e) {
					cause = e;
				} catch (IOException e) {
					cause = e;
				} catch (OutOfMemoryError e) {
					e.printStackTrace();
				} finally {
					if (isCancelled())
						return;
					retryCount++;
				}
			}
		} catch (Exception e) {
			cause = new IOException("Unhandled exceptionL :" + e.getMessage());
			e.printStackTrace();
		}

		throw cause;
	}

	public void cancel() {
		isCancelled = true;
	}

	public boolean isCancelled() {

		if (isCancelled) {
			sendCancelNotification();
		}
		return isCancelled;
	}

	private void sendCancelNotification() {

		if (mResponseHandler != null) {
			mResponseHandler.sendCancelMessage();
		}
	}

	public boolean isDone() {
		return isCancelled || isFinished;
	}

	private String paramsToString(Map<String, Object> params) {

		StringBuilder sb = new StringBuilder();
		for (Entry<String, Object> entry : params.entrySet()) {
			try {
				sb.append(URLEncoder.encode(entry.getKey().toString(), "UTF-8"))
						.append("=")
						.append(URLEncoder.encode(entry.getValue().toString(),
								"UTF-8")).append("&");
			} catch (UnsupportedEncodingException ex) {
				ex.printStackTrace();
			}
		}

		return sb.toString().substring(0, sb.length() - 1);

	}

	private byte[] readStream(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		byte[] buffer = new byte[1024];
		int length = 0;

		while ((length = in.read(buffer, 0, buffer.length)) != -1) {
			out.write(buffer, 0, length);
		}

		out.flush();
		out.close();
		return out.toByteArray();

	}

}
