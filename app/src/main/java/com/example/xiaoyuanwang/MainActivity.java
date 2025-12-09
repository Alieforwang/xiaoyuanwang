package com.example.xiaoyuanwang;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import com.example.xiaoyuanwang.api.SrunPortal;
import com.example.xiaoyuanwang.service.KeepAliveService;
import com.example.xiaoyuanwang.utils.ConfigManager;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private EditText etDomain;
    private TextView tvLog;
    private Button btnLogin;
    private Button btnLogout;
    private Button btnInfo;

    private SrunPortal srunPortal;
    private ConfigManager configManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private android.widget.ScrollView scrollView;
    private android.widget.ScrollView logScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request Notification Permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Initialize Views
        scrollView = findViewById(R.id.scroll_view);
        logScrollView = findViewById(R.id.log_scroll);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etDomain = findViewById(R.id.et_domain);
        tvLog = findViewById(R.id.tv_log);
        btnLogin = findViewById(R.id.btn_login);
        btnLogout = findViewById(R.id.btn_logout);
        btnInfo = findViewById(R.id.btn_info);
        
        // Initial Button State
        updateUIState(false);
        
        // Setup Settings Button
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        configManager = new ConfigManager(this);
        
        // Initialize SrunPortal immediately
        String serverUrl = configManager.getServerUrl();
        String userAgent = configManager.getUserAgent();
        srunPortal = new SrunPortal(serverUrl, userAgent);

        // Restore saved credentials
        etUsername.setText(configManager.getUsername());
        etPassword.setText(configManager.getPassword());
        etDomain.setText(configManager.getDomain());

        btnLogin.setOnClickListener(v -> performLogin());
        btnLogout.setOnClickListener(v -> performLogout());
        btnInfo.setOnClickListener(v -> performGetInfo());

        // Check Auto Login
        if (configManager.isAutoLogin()) {
             String username = etUsername.getText().toString();
             String password = etPassword.getText().toString();
             if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                 appendLog("触发自动登录...");
                 performLogin();
             }
        } else {
             checkLoginStatus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload SrunPortal with latest config
        String serverUrl = configManager.getServerUrl();
        String userAgent = configManager.getUserAgent();
        srunPortal = new SrunPortal(serverUrl, userAgent);

        // Handle Background Service Logic
        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        if (configManager.isRunBackground()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            stopService(serviceIntent);
        }
        
        // Refresh status if not auto-logging in (avoid double request if onCreate just ran)
        // But onResume is called after onCreate.
        // Let's just check status if we are not "processing".
        // Simple check:
        if (btnLogin.isEnabled() && btnLogout.isEnabled()) {
            // This state shouldn't happen with our logic (one is disabled),
            // but if we came back from settings, maybe we want to refresh.
            checkLoginStatus();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // If Run in Background is enabled, we DO NOT logout here.
        // The Service is responsible for logout when it is destroyed.
        if (configManager.isRunBackground()) {
             return;
        }

        if (configManager.isAutoLogout()) {
            // Note: onDestroy is not guaranteed to be called, and network requests here might be killed.
            // But for "Auto Logout on Exit", this is the best effort without a background service.
            // Ideally we should use a Service or Worker, but keeping it simple for now.
            // Since we need to run on a thread, we'll try to spawn one.
            String username = etUsername.getText().toString();
            String domain = etDomain.getText().toString();
            if (!TextUtils.isEmpty(username)) {
                new Thread(() -> {
                     // Re-instantiate portal in case srunPortal is null or context is weird
                     String serverUrl = configManager.getServerUrl();
                     String userAgent = configManager.getUserAgent();
                     new SrunPortal(serverUrl, userAgent).logout(username, "1", "", domain);
                }).start();
            }
        }
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String domain = etDomain.getText().toString().trim();
        
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            appendLog("错误: 用户名或密码不能为空");
            return;
        }
        
        // Save credentials if "Remember Password" is enabled
        if (configManager.isRememberPassword()) {
            configManager.setUsername(username);
            configManager.setPassword(password);
            configManager.setDomain(domain);
        } else {
            // Optionally clear them if they were saved before?
            // For now, let's just not update them. Or better, clear them to respect the switch.
            configManager.setUsername("");
            configManager.setPassword("");
            // Domain might be worth keeping? Usually yes. But let's keep it simple: 
            // "Remember Password" usually implies remembering the whole login set. 
            // But domain is configuration-like. Let's clear everything to be safe/strict.
            configManager.setDomain(""); 
        }

        btnLogin.setEnabled(false);
        appendLog("正在登录...");

        new Thread(() -> {
            boolean success = srunPortal.login(username, password, "1", "", domain);
            mainHandler.post(() -> {
                btnLogin.setEnabled(true);
                if (success) {
                    appendLog("[OK] 登录成功！");
                    updateUIState(true);
                    performGetInfo(); // Auto refresh info
                } else {
                    appendLog("[FAIL] 登录失败，请检查账号密码或网络连接");
                    updateUIState(false);
                }
            });
        }).start();
    }

    private void performLogout() {
        String username = etUsername.getText().toString().trim();
        String domain = etDomain.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            appendLog("错误: 用户名不能为空");
            return;
        }

        btnLogout.setEnabled(false);
        appendLog("正在注销...");

        new Thread(() -> {
            boolean success = srunPortal.logout(username, "1", "", domain);
            mainHandler.post(() -> {
                if (success) {
                    appendLog("[OK] 注销成功！");
                    updateUIState(false);
                } else {
                    appendLog("[FAIL] 注销失败");
                    // Enable logout button again to retry, or check status
                    btnLogout.setEnabled(true);
                }
            });
        }).start();
    }

    private void performGetInfo() {
        btnInfo.setEnabled(false);
        appendLog("正在查询在线信息...");

        new Thread(() -> {
            JSONObject info = srunPortal.getUserInfo("");
            mainHandler.post(() -> {
                btnInfo.setEnabled(true);
                if (info != null) {
                    try {
                        String userName = info.optString("user_name");
                        String onlineIp = info.optString("online_ip");
                        long sumBytes = info.optLong("sum_bytes", 0);
                        long sumSeconds = info.optLong("sum_seconds", 0);
                        double gb = sumBytes / (1024.0 * 1024.0 * 1024.0);
                        long hours = sumSeconds / 3600;

                        appendLog("--- 在线信息 ---");
                        appendLog("用户名: " + userName);
                        appendLog("IP地址: " + onlineIp);
                        appendLog(String.format(Locale.getDefault(), "已用流量: %.2f GB", gb));
                        appendLog("已用时长: " + hours + " 小时");
                        
                        updateUIState(true);
                    } catch (Exception e) {
                        appendLog("解析信息失败: " + e.getMessage());
                    }
                } else {
                    appendLog("查询失败或当前不在线");
                    updateUIState(false);
                }
            });
        }).start();
    }

    private void checkLoginStatus() {
        new Thread(() -> {
            // SrunPortal.detectClientIp() is lightweight, but getUserInfo("") is more accurate for login status
            // We use getUserInfo("") which returns null if not logged in
            JSONObject info = srunPortal.getUserInfo("");
            mainHandler.post(() -> {
                boolean isOnline = (info != null);
                updateUIState(isOnline);
            });
        }).start();
    }

    private void updateUIState(boolean isOnline) {
        if (isOnline) {
            btnLogin.setEnabled(false);
            btnLogin.setText("已登录");
            btnLogout.setEnabled(true);
        } else {
            btnLogin.setEnabled(true);
            btnLogin.setText("立即登录");
            btnLogout.setEnabled(false);
        }
    }

    private void appendLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvLog.append(time + " " + message + "\n");
        // Scroll to bottom
        if (logScrollView != null) {
            logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
        }
    }
}
