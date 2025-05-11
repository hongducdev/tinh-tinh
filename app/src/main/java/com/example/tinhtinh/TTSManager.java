package com.example.tinhtinh;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

public class TTSManager {
    private static final String TAG = "TTSManager";
    private static TTSManager instance;
    
    private TextToSpeech textToSpeech;
    private boolean isInitialized = false;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private Context context;
    private PowerManager.WakeLock wakeLock;
    
    private TTSManager(Context context) {
        this.context = context.getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        // Tạo WakeLock để giữ CPU hoạt động khi đọc thông báo
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, 
                "TinhTinh:TTSWakeLock");
        
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("vi", "VN"));
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Tiếng Việt không được hỗ trợ, sử dụng tiếng Anh");
                    textToSpeech.setLanguage(Locale.US);
                }
                
                textToSpeech.setPitch(1.0f);
                textToSpeech.setSpeechRate(0.9f);
                
                // Thiết lập Listener để theo dõi tiến trình TTS
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "TTS đang phát: " + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.d(TAG, "TTS đã phát xong: " + utteranceId);
                        abandonAudioFocus();
                        
                        // Giải phóng WakeLock
                        if (wakeLock.isHeld()) {
                            wakeLock.release();
                            Log.d(TAG, "WakeLock released after TTS");
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(TAG, "Lỗi khi phát TTS: " + utteranceId);
                        abandonAudioFocus();
                        
                        // Giải phóng WakeLock
                        if (wakeLock.isHeld()) {
                            wakeLock.release();
                            Log.d(TAG, "WakeLock released after TTS error");
                        }
                    }
                });
                
                isInitialized = true;
                Log.d(TAG, "TTS được khởi tạo thành công");
                
                // Kiểm thử TTS để đảm bảo nó hoạt động
                speak("TTS đã sẵn sàng");
            } else {
                Log.e(TAG, "Không thể khởi tạo TTS: " + status);
            }
        });
    }
    
    public static synchronized TTSManager getInstance(Context context) {
        if (instance == null) {
            instance = new TTSManager(context.getApplicationContext());
        }
        return instance;
    }
    
    public void speak(String text) {
        if (!isInitialized) {
            Log.e(TAG, "TTS chưa được khởi tạo");
            return;
        }
        
        // Kiểm tra xem TTS có được bật trong cài đặt không
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isTtsEnabled = prefs.getBoolean(SettingsActivity.KEY_TTS_ENABLED, true);
        
        if (!isTtsEnabled) {
            Log.d(TAG, "TTS đã bị tắt trong cài đặt");
            return;
        }
        
        // Kiểm tra giới hạn thời gian
        if (!SettingsActivity.isInTimeRange(context)) {
            Log.d(TAG, "Ngoài thời gian cho phép đọc thông báo");
            return;
        }
        
        Log.d(TAG, "Chuẩn bị phát TTS: " + text);
        
        // Lấy WakeLock để đảm bảo CPU không sleep trong quá trình TTS
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(60000); // 60 giây max
            Log.d(TAG, "WakeLock acquired for TTS");
        }
        
        // Yêu cầu audio focus
        requestAudioFocus();
        
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "tinhtinh_tts_" + System.currentTimeMillis());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tinhtinh_tts_" + System.currentTimeMillis());
            Log.d(TAG, "TTS speak result (Lollipop+): " + result);
        } else {
            int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
            Log.d(TAG, "TTS speak result (Pre-Lollipop): " + result);
        }
    }
    
    private void requestAudioFocus() {
        Log.d(TAG, "Requesting audio focus");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChange -> {
                        Log.d(TAG, "Audio focus changed: " + focusChange);
                    })
                    .build();
            
            int result = audioManager.requestAudioFocus(audioFocusRequest);
            Log.d(TAG, "Audio focus request result: " + result);
        } else {
            int result = audioManager.requestAudioFocus(null, AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            Log.d(TAG, "Audio focus request result (legacy): " + result);
        }
    }
    
    private void abandonAudioFocus() {
        Log.d(TAG, "Abandoning audio focus");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(null);
        }
    }
    
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            abandonAudioFocus();
            
            // Giải phóng WakeLock nếu đang giữ
            if (wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "WakeLock released on shutdown");
            }
        }
        instance = null;
    }
} 