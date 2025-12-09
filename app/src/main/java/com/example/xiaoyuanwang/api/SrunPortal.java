package com.example.xiaoyuanwang.api;

import android.util.Log;
import com.example.xiaoyuanwang.utils.XXTEACipher;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SrunPortal {
    private static final String TAG = "SrunPortal";
    private String baseUrl;
    private String userAgent;
    private static final String DEFAULT_URL = "http://10.0.100.100";
    // Default Mobile UA for backup
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36";

    public SrunPortal() {
        this(DEFAULT_URL, DEFAULT_USER_AGENT);
    }

    public SrunPortal(String baseUrl) {
        this(baseUrl, DEFAULT_USER_AGENT);
    }
    
    public SrunPortal(String baseUrl, String userAgent) {
        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
        this.userAgent = (userAgent != null && !userAgent.isEmpty()) ? userAgent : DEFAULT_USER_AGENT;
    }

    private JSONObject parseJsonp(String text) throws JSONException {
        Pattern pattern = Pattern.compile("\\(\\{.*\\}\\)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String jsonStr = matcher.group(0);
            jsonStr = jsonStr.substring(1, jsonStr.length() - 1); // Remove ()
            return new JSONObject(jsonStr);
        }
        return new JSONObject(text);
    }

    private String getMd5(String password, String token) {
        return md5(password + token);
    }

    private String getSha1(String text) {
        return sha1(text);
    }

    private String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            return toHexString(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String sha1(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            return toHexString(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String encodeUserInfo(JSONObject info, String token) {
        String customAlphabet = "LVoJPiCN2R8G90yg+hmFHuacZ1OWMnrsSTXkYpUq/3dlbfKwv6xztjI7DeBE45QA";
        String standardAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

        try {
            // JSON serialization usually matches simple string concatenation for simple dicts,
            // but we should match Python's separators=(',', ':') which removes whitespace.
            // org.json.JSONObject.toString() does not add whitespace by default, so it should be fine.
            // Python's json.dumps(info, separators=(',', ':')) produces {"key":"value"} without spaces.
            String jsonStr = info.toString();

            byte[] encrypted = XXTEACipher.encrypt(jsonStr, token);
            
            // Standard Base64
            String standardB64;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                standardB64 = Base64.getEncoder().encodeToString(encrypted);
            } else {
                 standardB64 = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP);
            }

            // Translate characters
            StringBuilder customB64 = new StringBuilder();
            for (char c : standardB64.toCharArray()) {
                int index = standardAlphabet.indexOf(c);
                if (index != -1) {
                    customB64.append(customAlphabet.charAt(index));
                } else {
                    customB64.append(c);
                }
            }

            return "{SRBX1}" + customB64.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    // Simple GET request helper
    private String sendGetRequest(String urlStr, String params) {
        try {
            URL url = new URL(urlStr + "?" + params);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", this.userAgent);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } else {
                Log.e(TAG, "GET request failed: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "GET request exception", e);
            return null;
        }
    }

    public String getChallenge(String username, String ip) {
        String url = baseUrl + "/cgi-bin/get_challenge";
        long timestamp = new Date().getTime();
        String params = "username=" + username + "&ip=" + ip + "&callback=jQuery_" + timestamp;
        
        try {
            String resp = sendGetRequest(url, params);
            if (resp != null) {
                JSONObject data = parseJsonp(resp);
                if ("ok".equals(data.optString("error"))) {
                    return data.optString("challenge");
                } else {
                    Log.e(TAG, "Get Token failed: " + data);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Get Token exception", e);
        }
        return null;
    }

    public JSONObject getUserInfo(String ip) {
        String url = baseUrl + "/cgi-bin/rad_user_info";
        long timestamp = new Date().getTime();
        String params = "ip=" + ip + "&callback=jQuery_" + timestamp;

        try {
            String resp = sendGetRequest(url, params);
            if (resp != null) {
                JSONObject data = parseJsonp(resp);
                if ("ok".equals(data.optString("error"))) {
                    return data;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Get User Info exception", e);
        }
        return null;
    }

    public String detectClientIp() {
        String url = baseUrl + "/cgi-bin/rad_user_info";
        long timestamp = new Date().getTime();
        String params = "callback=jQuery_" + timestamp;

        try {
            String resp = sendGetRequest(url, params);
            if (resp != null) {
                JSONObject data = parseJsonp(resp);
                if (data.has("client_ip")) {
                    return data.optString("client_ip");
                } else if (data.has("online_ip")) {
                    return data.optString("online_ip");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Detect IP exception", e);
        }
        return "";
    }

    public boolean login(String username, String password, String acId, String ip, String domain) {
        if (ip == null || ip.isEmpty()) {
            ip = detectClientIp();
            if (ip == null) ip = "";
        }
        
        Log.d(TAG, "Detected IP: " + ip);

        String fullUsername = username + (domain != null ? domain : "");
        String token = getChallenge(fullUsername, ip);
        
        if (token == null) {
            Log.e(TAG, "Failed to get token");
            return false;
        }
        
        Log.d(TAG, "Got token: " + token);

        String hmd5 = getMd5(password, token);
        
        JSONObject userInfo = new JSONObject();
        try {
            userInfo.put("username", fullUsername);
            userInfo.put("password", password);
            userInfo.put("ip", ip);
            userInfo.put("acid", acId);
            userInfo.put("enc_ver", "srun_bx1");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String info = encodeUserInfo(userInfo, token);
        
        String n = "200";
        String type = "1";
        String chksumStr = token + fullUsername + token + hmd5 + token + acId + 
                           token + ip + token + n + token + type + token + info;
        String chksum = getSha1(chksumStr);

        long timestamp = new Date().getTime();
        
        try {
            // Construct params manually to handle encoding correctly
            StringBuilder params = new StringBuilder();
            params.append("action=login");
            params.append("&username=").append(URLEncoder.encode(fullUsername, "UTF-8"));
            params.append("&password=").append(URLEncoder.encode("{MD5}" + hmd5, "UTF-8"));
            params.append("&os=").append(URLEncoder.encode("Android", "UTF-8")); // Changed to Android
            params.append("&name=").append(URLEncoder.encode("Android", "UTF-8")); // Changed to Android
            params.append("&double_stack=0");
            params.append("&chksum=").append(chksum);
            params.append("&info=").append(URLEncoder.encode(info, "UTF-8"));
            params.append("&ac_id=").append(acId);
            params.append("&ip=").append(ip);
            params.append("&n=").append(n);
            params.append("&type=").append(type);
            params.append("&callback=jQuery_").append(timestamp);

            String url = baseUrl + "/cgi-bin/srun_portal";
            String resp = sendGetRequest(url, params.toString());
            
            if (resp != null) {
                JSONObject data = parseJsonp(resp);
                Log.d(TAG, "Login response: " + data);
                return "ok".equals(data.optString("error"));
            }

        } catch (Exception e) {
            Log.e(TAG, "Login exception", e);
        }

        return false;
    }

    public boolean logout(String username, String acId, String ip, String domain) {
        String fullUsername = username + (domain != null ? domain : "");
        long timestamp = new Date().getTime();
        
        try {
            StringBuilder params = new StringBuilder();
            params.append("action=logout");
            params.append("&username=").append(URLEncoder.encode(fullUsername, "UTF-8"));
            params.append("&ip=").append(ip);
            params.append("&ac_id=").append(acId);
            params.append("&callback=jQuery_").append(timestamp);

            String url = baseUrl + "/cgi-bin/srun_portal";
            String resp = sendGetRequest(url, params.toString());
            
            if (resp != null) {
                JSONObject data = parseJsonp(resp);
                Log.d(TAG, "Logout response: " + data);
                return "ok".equals(data.optString("error"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Logout exception", e);
        }
        return false;
    }
}
