package com.eric.android.http;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ResponseHandlerInferface {

	void sendStartMessage();

	void sendCancelMessage();

	void sendFinishedMessage();

	void sendSuccessMessage(int statusCode,
			HashMap<String, List<String>> headers, byte[] responseBody);

	void sendFailureMessage(int statusCode,
			HashMap<String, List<String>> headers, byte[] responseBody,
			Throwable t);

	void sentRetryMessage(int retryNo);

	public URI getRequestURI();

	public HashMap<String, List<String>> getRequestHeaders();

	public void sendResponseMessage(int statusCode,
			Map<String, List<String>> map, byte[] responseBody);
}
