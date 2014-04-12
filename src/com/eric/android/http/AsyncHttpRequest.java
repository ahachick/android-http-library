package com.eric.android.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.util.Map;
import java.util.Map.Entry;

import com.eric.android.util.Logger;

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
	private static final int DEFAULT_READ_TIMEOUT = 60 * 1000;
	
	private int mSocketTimeout = DEFAULT_SOCKET_TIMEOUT;
	private int mReadTimeout = DEFAULT_READ_TIMEOUT;
	private int mRetryCount = DEFAULT_RETRY_COUNT;
	private boolean isCancelled = false;
	private boolean isFinished = false;
	private ResponseHandlerInterface mResponseHandler;
	private Map<String, String> mRequestParams;
	private Map<String, List<String>> mHeaders;

	private static final Map<String, List<String>> DEFAULT_HEADERS;

	static {
		DEFAULT_HEADERS = new HashMap<String, List<String>>();
		// add some header
		
	}

	public AsyncHttpRequest(URI uri, int requestType,
			ResponseHandlerInterface rh) {
		init(uri, requestType, rh, null, null);
	}

	public AsyncHttpRequest(URI uri, int requestType,
			ResponseHandlerInterface rh, Map<String, String> requestParams) {
		init(uri, requestType, rh, requestParams, null);
	}

	public AsyncHttpRequest(URI uri, int requestType,
			ResponseHandlerInterface rh, Map<String, String> requestParams,
			Map<String, List<String>> headers) {
		init(uri, requestType, rh, requestParams, headers);
	}

	private void init(URI uri, int requestType, ResponseHandlerInterface rh,
			Map<String, String> requestParams,
			Map<String, List<String>> headers) {

		mUri = uri;
		mRequestType = requestType;
		mResponseHandler = rh;
		mRequestParams = requestParams;
		mHeaders = (headers == null) ? headers : DEFAULT_HEADERS;

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
			mResponseHandler.sendFinishMessage();
		}
		
		isFinished = true;
	}
	
	public String paramsToString(Map<String,String> params) {
		
		StringBuilder sb = new StringBuilder();
		for(Entry<String, String> entry : params.entrySet()) {
			sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
		}
		
		return sb.toString().substring(0, sb.length() - 1);
		
		
	}

	private void makeRequest() throws IOException {
		HttpURLConnection conn = null;

		

		try {
			URL url = mUri.toURL();
			conn = (HttpURLConnection) url.openConnection();
			
			if (mHeaders != null) {
				for (String key : mHeaders.keySet()) {
					for (String value : mHeaders.get(key))
						conn.addRequestProperty(key, value);
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
				byte[] requestBody = paramsToString(mRequestParams).getBytes();
				conn.setFixedLengthStreamingMode(requestBody.length);
				
				BufferedOutputStream output = 
						new BufferedOutputStream(conn.getOutputStream());
				output.write(requestBody);
				output.flush();
				output.close();
			}
			BufferedInputStream input = 
					new BufferedInputStream(conn.getInputStream());
			
			Logger.debug(TAG, "ContentLength:" + conn.getContentLength());
			
			byte[] body = readStream(input);
			
			input.close();
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

			/*if (null != input) {
				try {
					input.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}*/
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

	public boolean isDone() {
		return isCancelled || isFinished;
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
