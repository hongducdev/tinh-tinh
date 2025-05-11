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
    
    // Đối tượng Text-to-Speech
    private TextToSpeech tts;
    private TTSManager ttsManager;
    
    // Tổng số tiền nhận được
    private long totalAmountReceived = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Thiết lập toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Khởi động service nền
        startBackgroundService();
        
        // Khởi tạo Text-to-Speech
        tts = new TextToSpeech(this, this);
        
        // Khởi tạo TTSManager
        ttsManager = TTSManager.getInstance(this);

        // Khởi tạo các view
        amountReceivedTextView = findViewById(R.id.amountReceivedTextView);
        transactionsListView = findViewById(R.id.transactionsListView);
        permissionButton = findViewById(R.id.permissionButton);
        FloatingActionButton settingsFab = findViewById(R.id.settingsFab);
        TextView statusTextView = findViewById(R.id.statusTextView);
        TextView emptyNotificationText = findViewById(R.id.emptyNotificationText);
        
        // Thiết lập thông báo trạng thái
        amountReceivedTextView.setText("Sẵn sàng theo dõi biến động số dư");
        
        // Thiết lập sự kiện click cho nút yêu cầu quyền (FAB)
        permissionButton.setOnClickListener(v -> {
            // Mở màn hình cài đặt để người dùng cho phép ứng dụng đọc thông báo
            requestNotificationPermission();
        });
        
        // Thiết lập sự kiện click cho nút cài đặt (FAB)
        settingsFab.setOnClickListener(v -> {
            // Mở màn hình cài đặt
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        
        // Thiết lập nút xóa tất cả thông báo
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
        
        // Tạo danh sách trống cho các thông báo
        transactionsList = new ArrayList<>();
        
        // Tạo adapter cho ListView với chức năng xóa thông báo đơn lẻ
        adapter = new TransactionAdapter(this, transactionsList, (position) -> {
            // Xóa thông báo tại vị trí position
            if (position >= 0 && position < transactionsList.size()) {
                transactionsList.remove(position);
                adapter.notifyDataSetChanged();
                updateTotalAmountFromTransactions();
                updateEmptyState();
                showSnackbar("Đã xóa thông báo");
            }
        });
        
        // Thiết lập adapter cho RecyclerView
        transactionsListView.setAdapter(adapter);
        transactionsListView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        // Hiển thị trạng thái không có thông báo nếu cần
        updateEmptyState();
        
        // Kiểm tra xem quyền đọc thông báo đã được cấp chưa
        checkNotificationListenerPermission();
        
        // Đăng ký BroadcastReceiver để nhận thông báo từ NotificationService
        registerNotificationReceiver();
        
        // Kiểm tra lần đầu chạy ứng dụng
        checkFirstLaunch();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Không cần inflate menu vì đã có nút cài đặt trên toolbar
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Không cần xử lý menu item cài đặt nữa
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Kiểm tra xem đây có phải là lần đầu chạy ứng dụng không
     */
    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        
        if (isFirstLaunch) {
            // Lưu trạng thái đã mở ứng dụng
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_FIRST_LAUNCH, false);
            editor.apply();
            
            // Hiển thị hộp thoại chào mừng và yêu cầu quyền
            showWelcomeDialog();
        }
    }
    
    /**
     * Hiển thị hộp thoại chào mừng người dùng và yêu cầu cấp quyền
     */
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
    
    /**
     * Mở màn hình cài đặt để người dùng cấp quyền
     */
    private void requestNotificationPermission() {
        // Mở màn hình cài đặt để người dùng cho phép ứng dụng đọc thông báo
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        startActivity(intent);
        
        // Hiển thị hướng dẫn
        showSnackbar("Hãy bật quyền cho Tinh Tinh trong danh sách");
    }
    
    /**
     * Hiển thị snackbar
     */
    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra lại quyền khi người dùng quay lại ứng dụng
        checkNotificationListenerPermission();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký BroadcastReceiver khi activity bị hủy
        if (notificationReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
        }
        
        // Giải phóng tài nguyên Text-to-Speech
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        
        // Không dừng BackgroundService khi activity bị hủy, để nó tiếp tục chạy ngầm
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
        // Sử dụng TTSManager để phát âm ngay cả khi màn hình tắt
        ttsManager.speak(text);
    }
    
    /**
     * Kiểm tra xem quyền đọc thông báo đã được cấp chưa
     */
    private void checkNotificationListenerPermission() {
        String enabledNotificationListeners = Settings.Secure.getString(
                getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        
        String packageName = getPackageName();
        
        // Ẩn/hiện nút yêu cầu quyền và cập nhật trạng thái
        TextView statusTextView = findViewById(R.id.statusTextView);
        if (enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName)) {
            // Quyền đã được cấp - ẩn nút cấp quyền
            permissionButton.setVisibility(View.GONE);
            statusTextView.setText("Đang lắng nghe thông báo từ các ngân hàng");
        } else {
            // Quyền chưa được cấp - hiện nút cấp quyền
            permissionButton.setVisibility(View.VISIBLE);
            permissionButton.setImageDrawable(getDrawable(android.R.drawable.ic_dialog_alert));
            permissionButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(R.color.primary)));
            statusTextView.setText("Chưa có quyền đọc thông báo. Nhấn vào nút dưới để cấp quyền");
        }
    }
    
    /**
     * Đăng ký BroadcastReceiver để nhận thông báo từ NotificationService
     */
    private void registerNotificationReceiver() {
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (NotificationService.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
                    String title = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TITLE);
                    String text = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TEXT);
                    String packageName = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_PACKAGE);
                    
                    Log.d(TAG, "Notification received: " + title + " - " + text);
                    
                    // Xử lý thông báo
                    processNotification(title, text, packageName);
                }
            }
        };
        
        // Đăng ký BroadcastReceiver với LocalBroadcastManager
        IntentFilter filter = new IntentFilter(NotificationService.ACTION_NOTIFICATION_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, filter);
    }
    
    /**
     * Xử lý thông báo nhận được
     */
    private void processNotification(String title, String text, String packageName) {
        Log.d(TAG, "Xử lý thông báo: " + title + " - " + text);
        
        // Kiểm tra xem có phải thông báo biến động số dư không
        boolean isBankNotification = false;
        
        // Danh sách từ khóa của thông báo ngân hàng
        String[] bankKeywords = {
            "biến động số dư", "số dư tài khoản", "tài khoản của bạn", 
            "nhận tiền", "chuyển tiền", "giao dịch", "GD:", "TK", 
            "MB Bank", "MBBank", "Vietcombank", "Techcombank", "BIDV", 
            "TPBank", "VietinBank", "ACB", "Sacombank"
        };
        
        // Kiểm tra title và text có chứa từ khóa ngân hàng không
        for (String keyword : bankKeywords) {
            if ((title != null && title.toLowerCase().contains(keyword.toLowerCase())) ||
                (text != null && text.toLowerCase().contains(keyword.toLowerCase()))) {
                isBankNotification = true;
                break;
            }
        }
        
        // Nếu không phải thông báo ngân hàng thì bỏ qua
        if (!isBankNotification) {
            Log.d(TAG, "Không phải thông báo ngân hàng: " + title);
            return;
        }
        
        // Tạo một giao dịch mới từ thông báo biến động số dư
        Map<String, String> notification = new HashMap<>();
        
        // Lấy tên ngân hàng từ package hoặc title
        String bankName;
        if (title != null && (title.contains("MB Bank") || title.contains("MBBank"))) {
            bankName = "MB Bank";
        } else {
            bankName = getApplicationNameFromPackage(packageName);
        }
        notification.put("sender", bankName);
        
        // Trích xuất số tiền từ nội dung thông báo (nếu có)
        String amountStr = extractAmount(text);
        String cleanAmount = formatAmountForSpeech(amountStr);
        notification.put("amount", amountStr);
        
        // Lấy thời gian hiện tại
        notification.put("date", getCurrentDate());
        
        // Lấy nội dung thông báo
        notification.put("message", text);
        
        // Thêm thông báo mới vào đầu danh sách
        transactionsList.add(0, notification);
        
        // Cập nhật ListView
        adapter.notifyDataSetChanged();
        
        // Cập nhật hiển thị trạng thái trống
        updateEmptyState();
        
        // Lấy tiền tố thông báo từ cài đặt
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String notificationPrefix = prefs.getString(KEY_NOTIFICATION_PREFIX, DEFAULT_NOTIFICATION_PREFIX);
        
        // Cập nhật tổng số tiền
        updateTotalAmountFromTransactions();
        
        // Hiển thị thông báo "Bạn đã nhận được [số tiền]"
        if (!amountStr.isEmpty()) {
            // Hiển thị với định dạng gốc (cho Toast và TextView)
            String displayMsg = notificationPrefix + " " + amountStr;
            showSnackbar(displayMsg);
            
            // Đọc với định dạng đơn giản hơn
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

        // Xử lý đặc biệt cho MB Bank
        if (text.contains("MB Bank") || text.contains("MBBank") || text.contains("Thông báo biến động số dư")) {
            // Pattern cho MB Bank: GD: +2,000VND hoặc GD: +2,000VND
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
    
    /**
     * Cập nhật hiển thị khi không có thông báo
     */
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
    
    /**
     * Khởi động service nền để ứng dụng tiếp tục hoạt động khi màn hình tắt
     */
    private void startBackgroundService() {
        // Kiểm tra cài đặt dịch vụ chạy ngầm
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
    
    /**
     * Dừng service nền (chỉ gọi khi cần dừng hẳn ứng dụng)
     */
    private void stopBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        stopService(serviceIntent);
        Log.d(TAG, "Đã dừng BackgroundService");
    }
}
