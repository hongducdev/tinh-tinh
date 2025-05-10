package com.example.tinhtinh;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements OnInitListener {
    private static final String TAG = "MainActivity";
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String PREFS_NAME = "TinhTinhPrefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    
    private TextView amountReceivedTextView;
    private RecyclerView transactionsListView;
    private MaterialButton permissionButton;
    private List<Map<String, String>> transactionsList;
    private TransactionAdapter adapter;
    private BroadcastReceiver notificationReceiver;

    private TextToSpeech tts;
    
    private long totalAmountReceived = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        tts = new TextToSpeech(this, this);

        amountReceivedTextView = findViewById(R.id.amountReceivedTextView);
        transactionsListView = findViewById(R.id.transactionsListView);
        permissionButton = findViewById(R.id.permissionButton);
        
        amountReceivedTextView.setText("Sẵn sàng theo dõi biến động số dư");
        
        permissionButton.setOnClickListener(v -> {
            requestNotificationPermission();
        });
        
        MaterialButton testTtsButton = findViewById(R.id.testTtsButton);
        testTtsButton.setOnClickListener(v -> {
            String testMessage = "Bạn đã nhận được 2 triệu đồng";
            speakNotification(testMessage);
            showSnackbar("Đang phát: " + testMessage);
        });
        
        transactionsList = new ArrayList<>();
        
        adapter = new TransactionAdapter(this, transactionsList);
        
        transactionsListView.setAdapter(adapter);
        
        checkNotificationListenerPermission();
        
        registerNotificationReceiver();
        
        checkFirstLaunch();
    }
    
    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        
        if (isFirstLaunch) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_FIRST_LAUNCH, false);
            editor.apply();
            
            showWelcomeDialog();
        }
    }
    
    private void showWelcomeDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Chào mừng đến với Tinh Tinh!")
            .setMessage("Ứng dụng cần quyền đọc thông báo để có thể theo dõi biến động số dư từ các ứng dụng ngân hàng. Bạn có muốn cấp quyền ngay bây giờ không?")
            .setPositiveButton("Cấp quyền ngay", (dialog, which) -> {
                requestNotificationPermission();
            })
            .setNegativeButton("Để sau", null)
            .setCancelable(false)
            .show();
    }
    
    private void requestNotificationPermission() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        startActivity(intent);
        
        showSnackbar("Hãy bật quyền cho Tinh Tinh trong danh sách");
    }
    
    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkNotificationListenerPermission();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
        }
        
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
    
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("vi", "VN"));
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Ngôn ngữ tiếng Việt không được hỗ trợ, sử dụng tiếng Anh");
                tts.setLanguage(Locale.US);
            }
            
            tts.setPitch(1.0f);
            tts.setSpeechRate(0.9f);
            
            Log.d(TAG, "TextToSpeech đã được khởi tạo thành công");
        } else {
            Log.e(TAG, "Không thể khởi tạo TextToSpeech");
        }
    }
    
    private void speakNotification(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "notification_id");
        }
    }
    
    private void checkNotificationListenerPermission() {
        String enabledNotificationListeners = Settings.Secure.getString(
                getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        
        String packageName = getPackageName();
        
        if (enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName)) {
            permissionButton.setText("ĐÃ CẤP QUYỀN ĐỌC THÔNG BÁO");
            permissionButton.setBackgroundColor(getResources().getColor(R.color.money_green));
            permissionButton.setIcon(getDrawable(android.R.drawable.ic_dialog_info));
            amountReceivedTextView.setText("Đang theo dõi biến động số dư...");
        } else {
            permissionButton.setText("CẤP QUYỀN ĐỌC THÔNG BÁO");
            permissionButton.setBackgroundColor(getResources().getColor(R.color.primary));
            permissionButton.setIcon(getDrawable(android.R.drawable.ic_dialog_alert));
            amountReceivedTextView.setText("Chưa có quyền đọc thông báo");
        }
    }
    
    private void registerNotificationReceiver() {
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (NotificationService.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
                    String title = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TITLE);
                    String text = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TEXT);
                    String packageName = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_PACKAGE);
                    
                    Log.d(TAG, "Notification received: " + title + " - " + text);
                    
                    processNotification(title, text, packageName);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(NotificationService.ACTION_NOTIFICATION_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, filter);
    }

    private void processNotification(String title, String text, String packageName) {
        Map<String, String> notification = new HashMap<>();
        
        String bankName = getApplicationNameFromPackage(packageName);
        notification.put("sender", bankName);
        
        String amountStr = extractAmount(text);
        String cleanAmount = formatAmountForSpeech(amountStr);
        notification.put("amount", amountStr);
        
        notification.put("date", getCurrentDate());
        
        notification.put("message", text);
        
        transactionsList.add(0, notification);
        
        adapter.notifyDataSetChanged();

        if (!amountStr.isEmpty()) {
            String displayMsg = "Bạn đã nhận được " + amountStr;
            showSnackbar(displayMsg);
            amountReceivedTextView.setText(displayMsg);
            
            String speechMsg = "Bạn đã nhận được " + cleanAmount + " đồng";
            speakNotification(speechMsg);
        } else {
            showSnackbar("Có biến động số dư từ: " + bankName);
        }
        
        Log.d(TAG, "Processed balance change: " + title + " - " + text);
    }
    
    private String extractAmount(String text) {
        if (text == null) return "";

        boolean isReceiving = text.toLowerCase().contains("nhận được") || 
                              text.toLowerCase().contains("nhận tiền") ||
                              text.toLowerCase().contains("tiền vào") ||
                              text.toLowerCase().contains("được chuyển") ||
                              text.toLowerCase().contains("cộng") ||
                              text.toLowerCase().contains("+") ||
                              text.toLowerCase().contains("tăng");
        
        if (!isReceiving) {
            boolean isPaying = text.toLowerCase().contains("chuyển tiền") ||
                              text.toLowerCase().contains("thanh toán") ||
                              text.toLowerCase().contains("tiền ra") ||
                              text.toLowerCase().contains("trừ") ||
                              text.toLowerCase().contains("-") ||
                              text.toLowerCase().contains("giảm");
            
            if (isPaying) {
                return "";
            }
        }
        
        String[] regexPatterns = {
            "(\\+?\\s*\\d{1,3}([,.\\s]\\d{3})+)\\s*(VND|₫|đồng)",
            "(\\+?\\s*\\d+)\\s*(VND|₫|đồng)"
        };
        
        for (String pattern : regexPatterns) {
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(text);
            
            if (m.find()) {
                return m.group(0);
            }
        }
        
        return "";
    }
    
    private String formatAmountForSpeech(String amountStr) {
        if (amountStr.isEmpty()) return "";
        
        String cleanAmount = amountStr.replaceAll("[^0-9]", "");
        
        try {
            long amount = Long.parseLong(cleanAmount);
            
            if (amount == 0) return "không đồng";
            
            if (amount >= 1000000000) {
                long billions = amount / 1000000000;
                long remainder = (amount % 1000000000) / 1000000;
                
                if (remainder > 0) {
                    return billions + " tỷ " + remainder + " triệu";
                } else {
                    return billions + " tỷ";
                }
            } else if (amount >= 1000000) {
                long millions = amount / 1000000;
                long remainder = (amount % 1000000) / 1000;
                
                if (remainder > 0) {
                    return millions + " triệu " + remainder + " nghìn";
                } else {
                    return millions + " triệu";
                }
            } else if (amount >= 1000) {
                long thousands = amount / 1000;
                long remainder = amount % 1000;
                
                if (remainder > 0) {
                    return thousands + " nghìn " + remainder;
                } else {
                    return thousands + " nghìn";
                }
            } else {
                return amount + "";
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Lỗi khi định dạng số tiền: " + e.getMessage());
            return cleanAmount;
        }
    }
    
    private String getApplicationNameFromPackage(String packageName) {
        try {
            return getPackageManager().getApplicationLabel(
                getPackageManager().getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) {
            return packageName;
        }
    }
    
    private String getCurrentDate() {
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        return df.format("dd/MM/yyyy", new java.util.Date()).toString();
    }

    private void updateAmountDisplay() {
        String formattedAmount = String.format("%,d VND", totalAmountReceived);
        amountReceivedTextView.setText(formattedAmount);
    }
    
    private void updateTotalAmountFromTransactions() {
        totalAmountReceived = 0;
        
        for (Map<String, String> transaction : transactionsList) {
            try {
                String amountStr = transaction.get("amount");
                if (amountStr != null) {
                    String cleanAmount = amountStr.replace(",", "")
                            .replace(".", "")
                            .replace(" ", "")
                            .replace("VND", "")
                            .trim();
                    long amount = Long.parseLong(cleanAmount);
                    totalAmountReceived += amount;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing amount: " + e.getMessage());
            }
        }
        
        updateAmountDisplay();
    }
}
