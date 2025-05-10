package com.example.tinhtinh;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import android.content.SharedPreferences;

public class SettingsActivity extends AppCompatActivity {

    private static final String DEFAULT_NOTIFICATION_PREFIX = "Bạn đã nhận được";
    private EditText notificationPrefixEditText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Thiết lập toolbar
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);

        // Hiển thị nút quay lại
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Cài đặt");
        }

        // Khởi tạo các view
        notificationPrefixEditText = findViewById(R.id.notification_prefix_edit);
        TextInputLayout notificationPrefixLayout = findViewById(R.id.notification_prefix_layout);
        MaterialButton saveButton = findViewById(R.id.save_settings_button);
        MaterialButton resetButton = findViewById(R.id.reset_settings_button);

        // Lấy cấu hình hiện tại
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        String currentPrefix = prefs.getString(MainActivity.KEY_NOTIFICATION_PREFIX, DEFAULT_NOTIFICATION_PREFIX);
        notificationPrefixEditText.setText(currentPrefix);

        // Lưu cài đặt
        saveButton.setOnClickListener(v -> {
            String newPrefix = notificationPrefixEditText.getText().toString();
            if (TextUtils.isEmpty(newPrefix)) {
                notificationPrefixLayout.setError("Vui lòng nhập nội dung thông báo");
                return;
            }
            
            // Lưu vào SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(MainActivity.KEY_NOTIFICATION_PREFIX, newPrefix);
            editor.apply();
            
            // Hiển thị thông báo và đóng màn hình
            finish();
        });

        // Đặt lại mặc định
        resetButton.setOnClickListener(v -> {
            notificationPrefixEditText.setText(DEFAULT_NOTIFICATION_PREFIX);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(MainActivity.KEY_NOTIFICATION_PREFIX, DEFAULT_NOTIFICATION_PREFIX);
            editor.apply();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 