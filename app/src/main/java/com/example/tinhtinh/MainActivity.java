package com.example.tinhtinh;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    public static final String PREFS_NAME = "TinhTinhPrefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    public static final String KEY_NOTIFICATION_PREFIX = "notification_prefix";
    private static final String DEFAULT_NOTIFICATION_PREFIX = "Bạn đã nhận được";
    
    private TextView amountReceivedTextView;
    private RecyclerView transactionsListView;
    private FloatingActionButton permissionButton;
    private List<Map<String, String>> transactionsList;
    private TransactionAdapter adapter;
    private BroadcastReceiver notificationReceiver;
    
    private TextToSpeech tts;
    private TTSManager ttsManager;

    private long totalAmountReceived = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startBackgroundService();

        tts = new TextToSpeech(this, this);

        ttsManager = TTSManager.getInstance(this);

        amountReceivedTextView = findViewById(R.id.amountReceivedTextView);
        transactionsListView = findViewById(R.id.transactionsListView);
        permissionButton = findViewById(R.id.permissionButton);
        FloatingActionButton settingsFab = findViewById(R.id.settingsFab);
        TextView statusTextView = findViewById(R.id.statusTextView);
        TextView emptyNotificationText = findViewById(R.id.emptyNotificationText);
        
        amountReceivedTextView.setText("Sẵn sàng theo dõi biến động số dư");
        
        permissionButton.setOnClickListener(v -> {
            requestNotificationPermission();
        });
        
        settingsFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        
        Button clearNotificationsButton = findViewById(R.id.clearNotificationsButton);
        clearNotificationsButton.setOnClickListener(v -> {
            if (!transactionsList.isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle("Xóa tất cả thông báo?")
                    .setMessage("Bạn có chắc chắn muốn xóa tất cả thông báo đã nhận không?")
                    .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                        transactionsList.clear();
                        adapter.notifyDataSetChanged();
                        totalAmountReceived = 0;
                        amountReceivedTextView.setText("0 VND");
                        updateEmptyState();
                        showSnackbar("Đã xóa tất cả thông báo");
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            } else {
                showSnackbar("Không có thông báo nào để xóa");
            }
        });
        
        transactionsList = new ArrayList<>();
        
        adapter = new TransactionAdapter(this, transactionsList, (position) -> {
            if (position >= 0 && position < transactionsList.size()) {
                transactionsList.remove(position);
                adapter.notifyDataSetChanged();
                updateTotalAmountFromTransactions();
                updateEmptyState();
                showSnackbar("Đã xóa thông báo");
            }
        });
        
        transactionsListView.setAdapter(adapter);
        transactionsListView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        updateEmptyState();
        
        checkNotificationListenerPermission();
        
        registerNotificationReceiver();
        
        checkFirstLaunch();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
    
    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        
        if (isFirstLaunch) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_FIRST_LAUNCH, false);
            editor.apply();
            
            speakWelcomeMessage();
            
            showWelcomeDialog();
        }
    }
    
    private void speakWelcomeMessage() {

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
            int result = tts.setLanguage(new Locale("vi", "VN")); // Tiếng Việt
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Ngôn ngữ tiếng Việt không được hỗ trợ, hiển thị dialog cài đặt");
                new MaterialAlertDialogBuilder(this)
                    .setTitle("Cần cài đặt tiếng Việt")
                    .setMessage("Ứng dụng cần giọng nói tiếng Việt để hoạt động tốt nhất. Bạn có muốn cài đặt ngay không?")
                    .setPositiveButton("Cài đặt ngay", (dialog, which) -> {
                        try {
                            Intent intent = new Intent();
                            intent.setAction("com.samsung.SMT.ACTION_MAIN");
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            try {
                                Intent intent = new Intent();
                                intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                                startActivity(intent);
                            } catch (ActivityNotFoundException ex) {
                                Intent intent = new Intent();
                                intent.setAction("android.settings.TTS_SETTINGS");
                                startActivity(intent);
                            }
                        }
                    })
                    .setNegativeButton("Để sau", (dialog, which) -> {
                        tts.setLanguage(Locale.US);
                        showSnackbar("Đang sử dụng tiếng Anh tạm thời");
                    })
                    .setCancelable(false)
                    .show();
            } else {
                tts.setPitch(1.0f);
                tts.setSpeechRate(0.9f);
                
                Log.d(TAG, "TextToSpeech đã được khởi tạo thành công với tiếng Việt");
            }
        } else {
            Log.e(TAG, "Không thể khởi tạo TextToSpeech");
            showSnackbar("Không thể khởi tạo tính năng đọc thông báo");
        }
    }

    private void speakNotification(String text) {
        ttsManager.speak(text);
    }
    
    private void checkNotificationListenerPermission() {
        String enabledNotificationListeners = Settings.Secure.getString(
                getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        
        String packageName = getPackageName();
        
        TextView statusTextView = findViewById(R.id.statusTextView);
        if (enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName)) {
            permissionButton.setVisibility(View.GONE);
            statusTextView.setText("Đang lắng nghe thông báo từ các ngân hàng");
        } else {
            permissionButton.setVisibility(View.VISIBLE);
            permissionButton.setImageDrawable(getDrawable(android.R.drawable.ic_dialog_alert));
            permissionButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(R.color.primary)));
            statusTextView.setText("Chưa có quyền đọc thông báo. Nhấn vào nút dưới để cấp quyền");
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
        Log.d(TAG, "Xử lý thông báo: " + title + " - " + text);
        
        boolean isBankNotification = false;
        
        String[] bankKeywords = {
            "biến động số dư", "số dư tài khoản", "tài khoản của bạn", 
            "nhận tiền", "chuyển tiền", "giao dịch", "GD:", "TK", 
            "MB Bank", "MBBank", "Vietcombank", "Techcombank", "BIDV", 
            "TPBank", "VietinBank", "ACB", "Sacombank"
        };
        
        for (String keyword : bankKeywords) {
            if ((title != null && title.toLowerCase().contains(keyword.toLowerCase())) ||
                (text != null && text.toLowerCase().contains(keyword.toLowerCase()))) {
                isBankNotification = true;
                break;
            }
        }
        
        if (!isBankNotification) {
            Log.d(TAG, "Không phải thông báo ngân hàng: " + title);
            return;
        }
        
        Map<String, String> notification = new HashMap<>();
        
        String bankName;
        if (title != null && (title.contains("MB Bank") || title.contains("MBBank"))) {
            bankName = "MB Bank";
        } else {
            bankName = getApplicationNameFromPackage(packageName);
        }
        notification.put("sender", bankName);
        
        String amountStr = extractAmount(text);
        String cleanAmount = formatAmountForSpeech(amountStr);
        notification.put("amount", amountStr);
        
        notification.put("date", getCurrentDate());
        
        notification.put("message", text);
        
        transactionsList.add(0, notification);
        
        adapter.notifyDataSetChanged();
        
        updateEmptyState();
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String notificationPrefix = prefs.getString(KEY_NOTIFICATION_PREFIX, DEFAULT_NOTIFICATION_PREFIX);
        
        updateTotalAmountFromTransactions();
        
        if (!amountStr.isEmpty()) {
            String displayMsg = notificationPrefix + " " + amountStr;
            showSnackbar(displayMsg);
            
            String speechMsg = notificationPrefix + " " + cleanAmount + " đồng";
            speakNotification(speechMsg);
            
            Log.d(TAG, "Đã phát thông báo: " + speechMsg);
        } else {
            showSnackbar("Có biến động số dư từ: " + bankName);
            Log.d(TAG, "Không trích xuất được số tiền từ: " + text);
        }
    }
    
    private String extractAmount(String text) {
        if (text == null) return "";

        if (text.contains("MB Bank") || text.contains("MBBank") || text.contains("Thông báo biến động số dư")) {
            Pattern mbPattern = Pattern.compile("GD:\\s*\\+?([\\d,]+)VND");
            Matcher mbMatcher = mbPattern.matcher(text);
            if (mbMatcher.find()) {
                String amount = mbMatcher.group(1);
                return amount + " VND";
            }
        }

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
    
    private void updateEmptyState() {
        TextView emptyNotificationText = findViewById(R.id.emptyNotificationText);
        if (transactionsList.isEmpty()) {
            emptyNotificationText.setVisibility(View.VISIBLE);
            transactionsListView.setVisibility(View.GONE);
        } else {
            emptyNotificationText.setVisibility(View.GONE);
            transactionsListView.setVisibility(View.VISIBLE);
        }
    }
    
    private void startBackgroundService() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isBackgroundServiceEnabled = prefs.getBoolean(SettingsActivity.KEY_BACKGROUND_SERVICE_ENABLED, true);
        
        if (!isBackgroundServiceEnabled) {
            Log.d(TAG, "Dịch vụ chạy ngầm đã bị tắt trong cài đặt");
            return;
        }
        
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Log.d(TAG, "Đã khởi động BackgroundService");
    }
    
    private void stopBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        stopService(serviceIntent);
        Log.d(TAG, "Đã dừng BackgroundService");
    }
}
