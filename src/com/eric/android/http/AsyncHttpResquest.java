package com.eric.android.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

public class AsyncHttpResquest implements Runnable {

	private static final String TAG = AsyncHttpResquest.class.getSimpleName();

	public static final int GET = 1;
	public static final int POST = 2;
	// public static final int DELETE = 3;
	// public static final int UPDATE = 4;

	private URI mUri;

	private int mRequestType;

	private static final int DEFAULT_RETRY_COUNT = 3;
	private static final int DEFAULT_SOCKET_TIMEOUT = 16 * 1000;

	private int mSocketTimeout = DEFAULT_SOCKET_TIMEOUT;
	private int mRetryCount = DEFAULT_RETRY_COUNT;
	private boolean isCancelled = false;
	private boolean isFinished = false;
	private ResponseHandlerInferface mResponseHandler;
	private HashMap<String, String> mRequestParams;
	private HashMap<String, List<String>> mHeaders;

	private static final HashMap<String, List<String>> DEFAULT_HEADERS;

	static {
		DEFAULT_HEADERS = new HashMap<String, List<String>>();
		// add some header
	}

	public AsyncHttpResquest(URI uri, int requestType,
			ResponseHandlerInferface rh) {
		init(uri, requestType, rh, null, null);
	}

	public AsyncHttpResquest(URI uri, int requestType,
			ResponseHandlerInferface rh, HashMap<String, String> requestParams) {
		init(uri, requestType, rh, requestParams, null);
	}

	public AsyncHttpResquest(URI uri, int requestType,
			ResponseHandlerInferface rh, HashMap<String, String> requestParams,
			HashMap<String, List<String>> headers) {
		init(uri, requestType, rh, requestParams, headers);
	}

	private void init(URI uri, int requestType, ResponseHandlerInferface rh,
			HashMap<String, String> requestParams,
			HashMap<String, List<String>> headers) {

		mUri = uri;
		mRequestType = requestType;
		mResponseHandler = rh;
		mRequestParams = requestParams;
		mHeaders = (headers == null) ? headers : DEFAULT_HEADERS;

	}

	private void makeRequest() throws IOException {
		HttpURLConnection conn = null;
		BufferedInputStream input = null;

		try {
			URL url = mUri.toURL();

			conn = (HttpURLConnection) url.openConnection();
			if (POST == mRequestType) {
				conn.setDoOutput(true);
				//
			}

			if (mHeaders != null) {
				for (String key : mHeaders.keySet()) {
					for (String value : mHeaders.get(key))
						conn.addRequestProperty(key, value);
				}
			}
			// set socket timeout;
			conn.setReadTimeout(mSocketTimeout);
			// disable http cache;
			conn.setUseCaches(false);

			input = new BufferedInputStream(conn.getInputStream());
			byte[] body = readStream(input);
			
			if(mResponseHandler != null) {
				int statusCode = conn.getResponseCode();
				mResponseHandler.sendResponseMessage(statusCode, conn.getHeaderFields(), body);
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
		}
	}

	private void makeRequestWithRetries() throws IOException {

		IOException cause = null;

		int retryCount = 0;

		try {

			while (retryCount == mRetryCount) {
				try {
					makeRequest();
					return;
				} catch (UnknownHostException e) {
					cause = e;
				} catch (SocketTimeoutException e) {
					cause = e;
				} catch (IOException e) {
					cause = e;
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
	
	public void cancel(){
		isCancelled = true;
	}

	public boolean isCancelled() {
		
		if(isCancelled) {
			sendCancelNotification();
		}
		return isCancelled;
	}

	private void sendCancelNotification() {

		if(mResponseHandler != null) {
			mResponseHandler.sendCancelMessage();
		}
	}

	@Override
	public void run() {
		
		if(isCancelled())
			return;
		
		if(mResponseHandler != null){
			mResponseHandler.sendStartMessage();
		}
		
		if(isCancelled())
			return;
		
		try {
			makeRequestWithRetries();
		} catch (IOException e) {
			
			if(!isCancelled() && mResponseHandler != null) {
				mResponseHandler.sendFailureMessage(0, null, null, e);
			}
			e.printStackTrace();
		}
		
		if(isCancelled())
			return;

		if(mResponseHandler != null) {
			mResponseHandler.sendFinishedMessage();
		}
		
		isFinished = true;
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
