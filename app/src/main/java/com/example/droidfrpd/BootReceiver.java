package com.example.droidfrpd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    private static final String PREFS_NAME = "FRPPrefs";
    private static final String PREF_AUTO_START = "auto_start";
    private static final String PREF_MODE = "mode";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive called with intent: " + intent.getAction());
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed received");
            
            // 检查是否启用开机自启
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean autoStart = prefs.getBoolean(PREF_AUTO_START, false);
            final String mode = prefs.getString(PREF_MODE, "frpc");
            final Context appContext = context.getApplicationContext();
            
            Log.d(TAG, "Loaded preferences - autoStart: " + autoStart + ", mode: " + mode);
            
            if (autoStart) {
                Log.i(TAG, "Auto-start enabled, scheduling service start in 10 seconds for mode: " + mode);
                
                // 使用主线程Handler确保在主线程中执行
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent serviceIntent = new Intent(appContext, FRPService.class);
                            serviceIntent.putExtra("mode", mode);
                            
                            // 在Android 8.0及以上版本中启动前台服务
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                Log.d(TAG, "Starting foreground service for Android O+");
                                appContext.startForegroundService(serviceIntent);
                            } else {
                                Log.d(TAG, "Starting service for Android < O");
                                appContext.startService(serviceIntent);
                            }
                            
                            Log.i(TAG, "FRP service auto-started in " + mode + " mode");
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to start FRP service", e);
                        }
                    }
                }, 10000); // 延迟10秒启动
            } else {
                Log.d(TAG, "Auto-start disabled, not starting service");
            }
        } else {
            Log.w(TAG, "Received unexpected intent: " + intent.getAction());
        }
    }
}