package com.example.tinhtinh;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;
import android.util.Log;
import android.content.pm.ResolveInfo;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String DEFAULT_NOTIFICATION_PREFIX = "Bạn đã nhận được";
    public static final String KEY_BACKGROUND_SERVICE_ENABLED = "background_service_enabled";
    public static final String KEY_TTS_ENABLED = "tts_enabled";
    public static final String KEY_TIME_LIMIT_ENABLED = "time_limit_enabled";
    public static final String KEY_START_HOUR = "start_hour";
    public static final String KEY_START_MINUTE = "start_minute";
    public static final String KEY_END_HOUR = "end_hour";
    public static final String KEY_END_MINUTE = "end_minute";
    
    private static final int REQUEST_BATTERY_OPTIMIZATION = 1001;
    
    private EditText notificationPrefixEditText;
    private SwitchMaterial backgroundServiceSwitch;
    private SwitchMaterial ttsEnabledSwitch;
    private SwitchMaterial timeLimitSwitch;
    private MaterialButton startTimeButton;
    private MaterialButton endTimeButton;
    private LinearLayout timeRangeContainer;
    
    private int startHour = 8;
    private int startMinute = 0;
    private int endHour = 22;
    private int endMinute = 0;
    
    private TextToSpeech tts;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("vi", "VN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
            }
        });

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Cài đặt");
        }

        notificationPrefixEditText = findViewById(R.id.notification_prefix_edit);
        TextInputLayout notificationPrefixLayout = findViewById(R.id.notification_prefix_layout);
        MaterialButton saveButton = findViewById(R.id.save_settings_button);
        MaterialButton resetButton = findViewById(R.id.reset_settings_button);
        MaterialButton permissionButton = findViewById(R.id.permissionSettingButton);
        MaterialButton testTtsButton = findViewById(R.id.testTtsButton);
        
        backgroundServiceSwitch = findViewById(R.id.background_service_switch);
        MaterialButton batteryOptimizationButton = findViewById(R.id.battery_optimization_button);
        ttsEnabledSwitch = findViewById(R.id.tts_enabled_switch);
        timeLimitSwitch = findViewById(R.id.time_limit_switch);
        startTimeButton = findViewById(R.id.start_time_button);
        endTimeButton = findViewById(R.id.end_time_button);
        timeRangeContainer = findViewById(R.id.time_range_container);
        MaterialButton testTtsInSettingsButton = findViewById(R.id.testTtsInSettingsButton);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        String currentPrefix = prefs.getString(MainActivity.KEY_NOTIFICATION_PREFIX, DEFAULT_NOTIFICATION_PREFIX);
        boolean isBackgroundServiceEnabled = prefs.getBoolean(KEY_BACKGROUND_SERVICE_ENABLED, true);
        boolean isTtsEnabled = prefs.getBoolean(KEY_TTS_ENABLED, true);
        boolean isTimeLimitEnabled = prefs.getBoolean(KEY_TIME_LIMIT_ENABLED, false);
        
        startHour = prefs.getInt(KEY_START_HOUR, 8);
        startMinute = prefs.getInt(KEY_START_MINUTE, 0);
        endHour = prefs.getInt(KEY_END_HOUR, 22);
        endMinute = prefs.getInt(KEY_END_MINUTE, 0);
        
        notificationPrefixEditText.setText(currentPrefix);
        backgroundServiceSwitch.setChecked(isBackgroundServiceEnabled);
        ttsEnabledSwitch.setChecked(isTtsEnabled);
        timeLimitSwitch.setChecked(isTimeLimitEnabled);
        updateTimeButtons();
        
        timeRangeContainer.setVisibility(isTimeLimitEnabled ? View.VISIBLE : View.GONE);

        permissionButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
            showSnackbar("Hãy bật quyền cho Tinh Tinh trong danh sách");
        });

        testTtsButton.setOnClickListener(v -> {
            String testPrefix = notificationPrefixEditText.getText().toString();
            if (TextUtils.isEmpty(testPrefix)) {
                testPrefix = DEFAULT_NOTIFICATION_PREFIX;
            }
            String testMessage = testPrefix + " 2 triệu đồng";
            speakNotification(testMessage);
            showSnackbar("Đang phát: " + testMessage);
        });
        
        testTtsInSettingsButton.setOnClickListener(v -> {
            String testPrefix = notificationPrefixEditText.getText().toString();
            if (TextUtils.isEmpty(testPrefix)) {
                testPrefix = DEFAULT_NOTIFICATION_PREFIX;
            }
            String testMessage = testPrefix + " 2 triệu đồng";
            speakNotification(testMessage);
            showSnackbar("Đang phát: " + testMessage);
        });
        
        ttsEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_TTS_ENABLED, isChecked).apply();
            
            if (isChecked) {
                showSnackbar("Đã bật đọc thông báo");
            } else {
                showSnackbar("Đã tắt đọc thông báo");
            }
        });
        
        timeLimitSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_TIME_LIMIT_ENABLED, isChecked).apply();
            
            timeRangeContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            
            if (isChecked) {
                showSnackbar("Đã bật giới hạn thời gian đọc thông báo");
            } else {
                showSnackbar("Đã tắt giới hạn thời gian đọc thông báo");
            }
        });
        
        startTimeButton.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        startHour = hourOfDay;
                        startMinute = minute;
                        updateTimeButtons();
                        
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(KEY_START_HOUR, startHour);
                        editor.putInt(KEY_START_MINUTE, startMinute);
                        editor.apply();
                        
                        showSnackbar("Đã đặt thời gian bắt đầu: " + formatTime(startHour, startMinute));
                    },
                    startHour,
                    startMinute,
                    true
            );
            timePickerDialog.show();
        });
        
        endTimeButton.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        endHour = hourOfDay;
                        endMinute = minute;
                        updateTimeButtons();
                        
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(KEY_END_HOUR, endHour);
                        editor.putInt(KEY_END_MINUTE, endMinute);
                        editor.apply();
                        
                        showSnackbar("Đã đặt thời gian kết thúc: " + formatTime(endHour, endMinute));
                    },
                    endHour,
                    endMinute,
                    true
            );
            timePickerDialog.show();
        });
        
        backgroundServiceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_BACKGROUND_SERVICE_ENABLED, isChecked).apply();
            
            if (isChecked) {
                startBackgroundService();
                showSnackbar("Đã bật dịch vụ chạy ngầm");
            } else {
                stopBackgroundService();
                showSnackbar("Đã tắt dịch vụ chạy ngầm");
            }
        });
        
        batteryOptimizationButton.setOnClickListener(v -> {
            openBatteryOptimizationSettings();
        });

        saveButton.setOnClickListener(v -> {
            String newPrefix = notificationPrefixEditText.getText().toString();
            if (TextUtils.isEmpty(newPrefix)) {
                notificationPrefixLayout.setError("Vui lòng nhập nội dung thông báo");
                return;
            }
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(MainActivity.KEY_NOTIFICATION_PREFIX, newPrefix);
            editor.putBoolean(KEY_BACKGROUND_SERVICE_ENABLED, backgroundServiceSwitch.isChecked());
            editor.putBoolean(KEY_TTS_ENABLED, ttsEnabledSwitch.isChecked());
            editor.putBoolean(KEY_TIME_LIMIT_ENABLED, timeLimitSwitch.isChecked());
            editor.putInt(KEY_START_HOUR, startHour);
            editor.putInt(KEY_START_MINUTE, startMinute);
            editor.putInt(KEY_END_HOUR, endHour);
            editor.putInt(KEY_END_MINUTE, endMinute);
            editor.apply();
            
            showSnackbar("Đã lưu cài đặt");
            finish();
        });

        resetButton.setOnClickListener(v -> {
            notificationPrefixEditText.setText(DEFAULT_NOTIFICATION_PREFIX);
            backgroundServiceSwitch.setChecked(true);
            ttsEnabledSwitch.setChecked(true);
            timeLimitSwitch.setChecked(false);
            startHour = 8;
            startMinute = 0;
            endHour = 22;
            endMinute = 0;
            updateTimeButtons();
            timeRangeContainer.setVisibility(View.GONE);
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(MainActivity.KEY_NOTIFICATION_PREFIX, DEFAULT_NOTIFICATION_PREFIX);
            editor.putBoolean(KEY_BACKGROUND_SERVICE_ENABLED, true);
            editor.putBoolean(KEY_TTS_ENABLED, true);
            editor.putBoolean(KEY_TIME_LIMIT_ENABLED, false);
            editor.putInt(KEY_START_HOUR, startHour);
            editor.putInt(KEY_START_MINUTE, startMinute);
            editor.putInt(KEY_END_HOUR, endHour);
            editor.putInt(KEY_END_MINUTE, endMinute);
            editor.apply();
            
            showSnackbar("Đã đặt lại thành cài đặt mặc định");
            
            startBackgroundService();
        });
    }
    
    private void updateTimeButtons() {
        startTimeButton.setText("Bắt đầu: " + formatTime(startHour, startMinute));
        endTimeButton.setText("Kết thúc: " + formatTime(endHour, endMinute));
    }
    
    private String formatTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
    
    public static boolean isInTimeRange(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        boolean isTimeLimitEnabled = prefs.getBoolean(KEY_TIME_LIMIT_ENABLED, false);
        
        if (!isTimeLimitEnabled) {
            return true;
        }
        
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        
        int startHour = prefs.getInt(KEY_START_HOUR, 8);
        int startMinute = prefs.getInt(KEY_START_MINUTE, 0);
        int endHour = prefs.getInt(KEY_END_HOUR, 22);
        int endMinute = prefs.getInt(KEY_END_MINUTE, 0);
        
        int currentTime = currentHour * 60 + currentMinute;
        int startTime = startHour * 60 + startMinute;
        int endTime = endHour * 60 + endMinute;
        
        if (startTime <= endTime) {
            return currentTime >= startTime && currentTime <= endTime;
        } else {
            return currentTime >= startTime || currentTime <= endTime;
        }
    }
    
    private void openBatteryOptimizationSettings() {
        String packageName = getPackageName();
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                
                if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    
                    try {
                        startActivity(intent);
                        return;
                    } catch (Exception e) {
                        Log.e("SettingsActivity", "Không thể mở cài đặt tối ưu hóa pin cụ thể: " + e.getMessage());
                    }
                } else {
                    showSnackbar("Ứng dụng đã được tắt tối ưu hóa pin");
                }
            }
            
            try {
                Intent intent = new Intent();
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                } else {
                    intent.setAction(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                }
                
                startActivity(intent);
                showSnackbar("Tìm và tắt tối ưu hóa pin cho Tinh Tinh");
                return;
            } catch (Exception e) {
                Log.e("SettingsActivity", "Không thể mở cài đặt danh sách tối ưu hóa pin: " + e.getMessage());
            }
            
            try {
                Intent powerUsageIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                ResolveInfo resolveInfo = getPackageManager().resolveActivity(powerUsageIntent, 0);
                
                if (resolveInfo != null) {
                    startActivity(powerUsageIntent);
                    showSnackbar("Hãy tìm Tinh Tinh trong danh sách và tắt tối ưu hóa");
                    return;
                }
            } catch (Exception e) {
                Log.e("SettingsActivity", "Không thể mở cài đặt pin: " + e.getMessage());
            }
            
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
                showSnackbar("Vào phần Pin và tắt tối ưu hóa pin cho Tinh Tinh");
                return;
            } catch (Exception e) {
                Log.e("SettingsActivity", "Không thể mở cài đặt ứng dụng: " + e.getMessage());
            }
            
            showSnackbar("Không thể mở cài đặt pin tự động. Vui lòng vào Cài đặt > Pin > Tối ưu hóa pin");
            
        } catch (Exception e) {
            Log.e("SettingsActivity", "Lỗi khi mở cài đặt tối ưu hóa pin: " + e.getMessage());
            showSnackbar("Không thể mở cài đặt pin. Vui lòng vào Cài đặt > Pin > Tối ưu hóa pin");
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
    
    private void stopBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        stopService(serviceIntent);
    }

    private void speakNotification(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "notification_id");
        }
    }
    
    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
} 