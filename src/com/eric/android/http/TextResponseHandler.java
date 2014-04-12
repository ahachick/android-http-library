package com.eric.android.http;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import com.eric.android.util.Logger;

public abstract class TextResponseHandler extends AbstractResponseHandler{
	
	private static final String TAG = TextResponseHandler.class.getSimpleName();

	private static final String DEFAULT_CHARSET = "UTF-8";
	
	private String mCharset;
	
	public TextResponseHandler() {
		this(DEFAULT_CHARSET);
	}
	
	public TextResponseHandler(String charset) {
		mCharset = charset == null ? DEFAULT_CHARSET : charset;
		
	}
	
	protected abstract void onSuccess(int statusCode, Map<String, List<String>> headers, String responseStr);
	
	protected abstract void onFailure(int statusCode, Map<String, List<String>> headers,
			String responseString, Throwable t);
	
	@Override
	protected void onSuccess(int statusCode, Map<String, List<String>> headers,
			byte[] body) {
		this.onSuccess(statusCode, headers, getResponseString(body, mCharset));
	}

	@Override
	protected void onFailure(int statusCode, Map<String, List<String>> headers,
			byte[] body, Throwable t) {
		this.onFailure(statusCode, headers, getResponseString(body, mCharset), t);
	}
	
	

	private String getResponseString(byte[] stringBytes, String charset) {
		
		try {
			return stringBytes == null ? null : new String(stringBytes, charset);
		} catch (UnsupportedEncodingException e) {
			Logger.debug(TAG, "getResponseString : Encoding response failed");
			return null;
		}
	}

}
