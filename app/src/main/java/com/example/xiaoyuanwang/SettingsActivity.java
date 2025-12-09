package com.example.xiaoyuanwang;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.xiaoyuanwang.utils.ConfigManager;

public class SettingsActivity extends AppCompatActivity {

    private EditText etServerUrl;
    private EditText etUserAgent;
    private Switch switchAutoLogin;
    private Switch switchRememberPassword;
    private Switch switchRunBackground;
    private Switch switchAutoLogout;
    private Button btnSave;
    private ConfigManager configManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("设置");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        configManager = new ConfigManager(this);

        etServerUrl = findViewById(R.id.et_server_url);
        etUserAgent = findViewById(R.id.et_user_agent);
        switchAutoLogin = findViewById(R.id.switch_auto_login);
        switchRememberPassword = findViewById(R.id.switch_remember_password);
        switchRunBackground = findViewById(R.id.switch_run_background);
        switchAutoLogout = findViewById(R.id.switch_auto_logout);
        btnSave = findViewById(R.id.btn_save);

        loadConfig();

        btnSave.setOnClickListener(v -> saveConfig());
    }

    private void loadConfig() {
        etServerUrl.setText(configManager.getServerUrl());
        etUserAgent.setText(configManager.getUserAgent());
        switchAutoLogin.setChecked(configManager.isAutoLogin());
        switchRememberPassword.setChecked(configManager.isRememberPassword());
        switchRunBackground.setChecked(configManager.isRunBackground());
        switchAutoLogout.setChecked(configManager.isAutoLogout());
    }

    private void saveConfig() {
        String url = etServerUrl.getText().toString().trim();
        String ua = etUserAgent.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, "服务器地址不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        configManager.setServerUrl(url);
        configManager.setUserAgent(ua);
        configManager.setAutoLogin(switchAutoLogin.isChecked());
        configManager.setRememberPassword(switchRememberPassword.isChecked());
        configManager.setRunBackground(switchRunBackground.isChecked());
        configManager.setAutoLogout(switchAutoLogout.isChecked());

        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
