package com.example.tinhtinh;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Notification Listener Service created");
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
} 