package com.eric.android.http;

import android.annotation.TargetApi;
import android.os.Build;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class PersistenceCookieStore implements CookieStore{

	private static final String TAG = PersistenceCookieStore.class.getSimpleName();
	private static final String COOKIE_PREFS = "CookiePrefersFile";
	

	@Override
	public void add(URI uri, HttpCookie cookie) {
		
	}

	@Override
	public List<HttpCookie> get(URI uri) {
		return null;
	}

	@Override
	public List<HttpCookie> getCookies() {
		return null;
	}

	@Override
	public List<URI> getURIs() {
		return null;
	}

	@Override
	public boolean remove(URI uri, HttpCookie cookie) {
		return false;
	}

	@Override
	public boolean removeAll() {
		return false;
	}

}
