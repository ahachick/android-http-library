package com.eric.android.http;

import java.net.URI;
import java.util.Map;
import java.util.List;
import java.util.Map;

public interface ResponseHandlerInterface {

	void sendStartMessage();

	void sendCancelMessage();

	void sendFinishMessage();

	void sendSuccessMessage(int statusCode,
			Map<String, List<String>> headers, byte[] responseBody);

	void sendFailureMessage(int statusCode,
			Map<String, List<String>> headers, byte[] responseBody,
			Throwable t);

	void sentRetryMessage(int retryNo);

	public URI getRequestURI();

	public Map<String, List<String>> getRequestHeaders();

	public void sendResponseMessage(int statusCode,
			Map<String, List<String>> params, byte[] responseBody);
}
