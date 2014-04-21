package com.eric.android.http;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Copyright (c) 2014
 * All right reserved.
 * 
 * @author ji.jiaxiang
 *
 * 2014/04/21 First Release
 */
public class PersistenceCookieStore{

	@SuppressWarnings("unused")
	private static final String TAG = PersistenceCookieStore.class.getSimpleName();
	private static final String COOKIE_PREFS = "CookiePrefersFile";
	private static final String COOKIE_KEY = "COOKIE_KEY";
	private SharedPreferences cookiePrefs;
	
	private String cookieStr;
	
	PersistenceCookieStore(Context ctx) {
		cookiePrefs = ctx.getSharedPreferences(COOKIE_PREFS, Context.MODE_PRIVATE);
		
	}
	
	public String getCookieString() {
		cookieStr = cookiePrefs.getString(COOKIE_KEY, cookieStr);
		return cookieStr;
	}
	
	public void addCookieString(String str) {
		cookieStr = str;
		SharedPreferences.Editor editor = cookiePrefs.edit();
		editor.putString(COOKIE_KEY, str);
		editor.commit();
	}
	
	public void clearAll() {
		SharedPreferences.Editor editor = cookiePrefs.edit();
		editor.clear();
		editor.commit();
	}
	

}
