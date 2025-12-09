package com.example.xiaoyuanwang.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {
    private static final String PREF_NAME = "xiaoyuanwang_config";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_USER_AGENT = "user_agent";
    private static final String KEY_AUTO_LOGIN = "auto_login";
    private static final String KEY_AUTO_LOGOUT = "auto_logout";
    private static final String KEY_RUN_BACKGROUND = "run_background";
    private static final String KEY_REMEMBER_PASSWORD = "remember_password";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_DOMAIN = "domain";

    private static final String DEFAULT_SERVER_URL = "http://10.0.100.100";
    // Default Mobile UA
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36";

    private final SharedPreferences prefs;

    public ConfigManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getServerUrl() {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }

    public void setServerUrl(String url) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
    }

    public String getUserAgent() {
        return prefs.getString(KEY_USER_AGENT, DEFAULT_USER_AGENT);
    }

    public void setUserAgent(String ua) {
        prefs.edit().putString(KEY_USER_AGENT, ua).apply();
    }

    public boolean isAutoLogin() {
        return prefs.getBoolean(KEY_AUTO_LOGIN, false);
    }

    public void setAutoLogin(boolean autoLogin) {
        prefs.edit().putBoolean(KEY_AUTO_LOGIN, autoLogin).apply();
    }

    public boolean isAutoLogout() {
        return prefs.getBoolean(KEY_AUTO_LOGOUT, false);
    }

    public void setAutoLogout(boolean autoLogout) {
        prefs.edit().putBoolean(KEY_AUTO_LOGOUT, autoLogout).apply();
    }

    public boolean isRunBackground() {
        return prefs.getBoolean(KEY_RUN_BACKGROUND, false);
    }

    public void setRunBackground(boolean runBackground) {
        prefs.edit().putBoolean(KEY_RUN_BACKGROUND, runBackground).apply();
    }

    public boolean isRememberPassword() {
        return prefs.getBoolean(KEY_REMEMBER_PASSWORD, true); // Default true to match previous behavior
    }

    public void setRememberPassword(boolean remember) {
        prefs.edit().putBoolean(KEY_REMEMBER_PASSWORD, remember).apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public void setUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, "");
    }

    public void setPassword(String password) {
        prefs.edit().putString(KEY_PASSWORD, password).apply();
    }
    
    public String getDomain() {
        return prefs.getString(KEY_DOMAIN, "");
    }

    public void setDomain(String domain) {
        prefs.edit().putString(KEY_DOMAIN, domain).apply();
    }
}
