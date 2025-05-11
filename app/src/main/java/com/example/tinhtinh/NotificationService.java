package com.example.tinhtinh;

import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationService extends NotificationListenerService {
    private static final String TAG = "NotificationService";
    public static final String ACTION_NOTIFICATION_RECEIVED = "com.example.tinhtinh.NOTIFICATION_RECEIVED";
    public static final String EXTRA_NOTIFICATION_TITLE = "notification_title";
    public static final String EXTRA_NOTIFICATION_TEXT = "notification_text";
    public static final String EXTRA_NOTIFICATION_PACKAGE = "notification_package";

    private static final String[] BALANCE_KEYWORDS = {
        "biến động số dư", 
        "thông báo số dư",
        "thông báo biến động số dư",
        "số dư tài khoản",
        "tài khoản của bạn", 
        "nhận tiền", 
        "chuyển khoản", 
        "giao dịch thành công",
        "GD:", // MB Bank thường sử dụng "GD:" cho giao dịch
        "TK", // Tài khoản trong MB Bank
        "SD:" // Số dư trong MB Bank
    };
    
    private static final String[] BANK_PACKAGES = {
        "com.VCB", // Vietcombank
        "com.vietinbank.ipay", // VietinBank
        "com.bidv.smartbanking", // BIDV
        "com.tpb.mb.gprsandroid", // TPBank
        "vn.com.techcombank.bb.app", // Techcombank
        "com.scb.scbmobilebanking", // SCB
        "com.vnpay.hdbank", // HD Bank
        "com.mbmobile", // MB Bank
        "com.mbbank", // MB Bank alternative package
        "vn.mbbank.mbappcust", // MB Bank custom package
        "org.android.receiver", // Cho các tests
        "com.facebook.orca", // Messenger cho tests
        "com.facebook.katana", // Facebook cho tests
        "com.google.android.apps.messaging" // Tin nhắn cho tests
    };
    
    private TTSManager ttsManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Notification Listener Service created");
        
        // Khởi tạo TTSManager
        ttsManager = TTSManager.getInstance(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Notification Listener Service bound");
        return super.onBind(intent);
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) {
            return;
        }
        
        Bundle extras = notification.extras;
        String title = extras.getString(Notification.EXTRA_TITLE, "");
        String text = extras.getString(Notification.EXTRA_TEXT, "");
        String packageName = sbn.getPackageName();
        
        Log.d(TAG, "Notification received: " + packageName + " - " + title + " - " + text);
        
        // Xử lý đặc biệt cho MB Bank
        boolean isMBBank = title != null && (title.contains("MB Bank") || title.contains("MBBank"));
        
        // Kiểm tra thông báo từ ứng dụng ngân hàng
        boolean isFromBank = isFromBankApp(packageName) || isMBBank;
        
        // Kiểm tra nội dung thông báo chứa từ khóa biến động số dư
        boolean hasBalanceKeywords = containsBalanceKeyword(title) || containsBalanceKeyword(text);
        
        // Kiểm tra cụ thể cho MB Bank
        boolean isMBBankTransaction = isMBBank && 
                                     (text != null && 
                                     (text.contains("GD:") || 
                                      text.contains("Thông báo biến động số dư") || 
                                      text.contains("TK")));
        
        if (isFromBank && (hasBalanceKeywords || isMBBankTransaction)) {
            Log.d(TAG, "Balance change notification detected: " + title + " - " + text);
            
            // Trực tiếp phát TTS từ service để đảm bảo hoạt động khi màn hình tắt
            String amountStr = extractAmount(text);
            if (!amountStr.isEmpty()) {
                String cleanAmount = formatAmountForSpeech(amountStr);
                
                // Lấy tiền tố thông báo từ cài đặt
                SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
                String notificationPrefix = prefs.getString(MainActivity.KEY_NOTIFICATION_PREFIX, "Bạn đã nhận được");
                
                // Phát thông báo bằng TTS
                String speechMessage = notificationPrefix + " " + cleanAmount + " đồng";
                ttsManager.speak(speechMessage);
                
                Log.d(TAG, "TTS trong service: " + speechMessage);
            }
            
            // Gửi broadcast cho MainActivity nếu đang mở
            Intent intent = new Intent(ACTION_NOTIFICATION_RECEIVED);
            intent.putExtra(EXTRA_NOTIFICATION_TITLE, title);
            intent.putExtra(EXTRA_NOTIFICATION_TEXT, text);
            intent.putExtra(EXTRA_NOTIFICATION_PACKAGE, packageName);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }
    
    private boolean isFromBankApp(String packageName) {
        for (String bankPackage : BANK_PACKAGES) {
            if (packageName.equals(bankPackage)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean containsBalanceKeyword(String text) {
        if (text == null) return false;
        
        String lowerText = text.toLowerCase();
        for (String keyword : BALANCE_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        
        if (lowerText.matches(".*\\d+.*") && 
            (lowerText.contains("vnd") || 
             lowerText.contains("đồng") || 
             lowerText.contains("₫") || 
             lowerText.contains("tiền") || 
             lowerText.contains("nhận") || 
             lowerText.contains("chuyển"))) {
            return true;
        }
        
        return false;
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
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(text);
            
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
} 