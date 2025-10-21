package com.example.droidfrpd;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FRPService extends Service {
    
    private static final String TAG = "FRPService";
    private static final String FRPC = "frpc";
    private static final String FRPS = "frps";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FRPServiceChannel";
    
    private Process frpProcess;
    private String currentMode = FRPC; // 默认为客户端模式
    private LogCollector logCollector;
    private LogCollector errorLogCollector;
    private ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private final IBinder binder = new LocalBinder();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable keepAliveRunnable;
    
    public class LocalBinder extends Binder {
        FRPService getService() {
            return FRPService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "FRPService onCreate");
        createNotificationChannel();
        setupKeepAlive();
        setupFRPClient();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "FRPService onStartCommand");
        if (intent != null) {
            currentMode = intent.getStringExtra("mode");
            if (currentMode == null) {
                currentMode = FRPC;
            }
            Log.d(TAG, "Mode: " + currentMode);
        }
        
        // 启动前台服务以提高持久性
        startForeground(NOTIFICATION_ID, createNotification());
        
        startFRPClient();
        return START_STICKY; // Restart service if killed
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "FRPService onDestroy");
        stopKeepAlive();
        stopForeground(true);
        stopFRPClient();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "FRP Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    
    private Notification createNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        
        builder.setContentTitle("FRP Service")
               .setContentText("FRP " + currentMode + " is running")
               .setSmallIcon(R.drawable.ic_notification)
               .setOngoing(true); // 持续通知
        
        return builder.build();
    }
    
    // 设置保活机制
    private void setupKeepAlive() {
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                // 定期检查FRP进程状态
                if (frpProcess != null) {
                    try {
                        // 检查进程是否仍在运行
                        int exitCode = frpProcess.exitValue();
                        Log.w(TAG, "FRP process exited with code: " + exitCode);
                        addLog("Warning: FRP process exited with code: " + exitCode);
                        // 重新启动进程
                        startFRPClient();
                    } catch (IllegalThreadStateException e) {
                        // 进程仍在运行
                        Log.d(TAG, "FRP process is still running");
                    }
                } else {
                    // 如果进程为null，尝试重新启动
                    startFRPClient();
                }
                
                // 每30秒检查一次
                handler.postDelayed(this, 30000);
            }
        };
        
        // 启动保活检查
        handler.postDelayed(keepAliveRunnable, 30000);
    }
    
    private void stopKeepAlive() {
        if (keepAliveRunnable != null) {
            handler.removeCallbacks(keepAliveRunnable);
        }
    }
    
    private void setupFRPClient() {
        try {
            Log.d(TAG, "Setting up FRP client");
            // Copy FRP binaries from assets to app's private directory
            File frpcBinary = new File(getFilesDir(), FRPC);
            File frpsBinary = new File(getFilesDir(), FRPS);
            
            if (!frpcBinary.exists()) {
                Log.d(TAG, "Copying frpc binary");
                copyFRPBinaryFromAssets(FRPC, frpcBinary);
            }
            
            if (!frpsBinary.exists()) {
                Log.d(TAG, "Copying frps binary");
                copyFRPBinaryFromAssets(FRPS, frpsBinary);
            }
            
            // Make the binaries executable
            Log.d(TAG, "Making binaries executable");
            boolean frpcExec = frpcBinary.setExecutable(true);
            boolean frpsExec = frpsBinary.setExecutable(true);
            Log.d(TAG, "frpc executable: " + frpcExec + ", frps executable: " + frpsExec);
            
            // Create default config files if not exists
            File frpcConfigFile = new File(getFilesDir(), "frpc.toml");
            File frpsConfigFile = new File(getFilesDir(), "frps.toml");
            
            if (!frpcConfigFile.exists()) {
                Log.d(TAG, "Creating default frpc config");
                createDefaultFRPCConfig(frpcConfigFile);
            }
            
            if (!frpsConfigFile.exists()) {
                Log.d(TAG, "Creating default frps config");
                createDefaultFRPSConfig(frpsConfigFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up FRP client", e);
            addLog("Error: " + e.getMessage());
        }
    }
    
    private void copyFRPBinaryFromAssets(String assetName, File targetFile) throws IOException {
        Log.d(TAG, "Copying " + assetName + " to " + targetFile.getAbsolutePath());
        InputStream inputStream = getAssets().open(assetName);
        OutputStream outputStream = new FileOutputStream(targetFile);
        
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        
        inputStream.close();
        outputStream.close();
        Log.d(TAG, "Copy completed for " + assetName);
    }
    
    private void createDefaultFRPCConfig(File configFile) throws IOException {
        String defaultConfig = "# frpc.toml\n" +
                "[common]\n" +
                "server_addr = \"your-frp-server.com\"\n" +
                "server_port = 7000\n" +
                "token = \"your-token\"\n" +
                "\n" +
                "[ssh]\n" +
                "type = \"tcp\"\n" +
                "local_ip = \"127.0.0.1\"\n" +
                "local_port = 22\n" +
                "remote_port = 6000\n";
        
        FileOutputStream fos = new FileOutputStream(configFile);
        fos.write(defaultConfig.getBytes());
        fos.close();
        Log.d(TAG, "Created default frpc config at " + configFile.getAbsolutePath());
    }
    
    private void createDefaultFRPSConfig(File configFile) throws IOException {
        String defaultConfig = "# frps.toml\n" +
                "[common]\n" +
                "bind_port = 7000\n" +
                "token = \"your-token\"\n";
        
        FileOutputStream fos = new FileOutputStream(configFile);
        fos.write(defaultConfig.getBytes());
        fos.close();
        Log.d(TAG, "Created default frps config at " + configFile.getAbsolutePath());
    }
    
    private void startFRPClient() {
        try {
            Log.d(TAG, "Starting FRP client, mode: " + currentMode);
            File frpBinary = new File(getFilesDir(), currentMode);
            String configFileName = currentMode + ".toml";
            File configFile = new File(getFilesDir(), configFileName);
            
            // Check if binary exists
            if (!frpBinary.exists()) {
                Log.e(TAG, "FRP binary does not exist: " + frpBinary.getAbsolutePath());
                addLog("Error: FRP binary does not exist: " + frpBinary.getAbsolutePath());
                return;
            }
            
            // Check if config file exists
            if (!configFile.exists()) {
                Log.e(TAG, "Config file does not exist: " + configFile.getAbsolutePath());
                addLog("Error: Config file does not exist: " + configFile.getAbsolutePath());
                return;
            }
            
            // Check if binary is executable
            if (!frpBinary.canExecute()) {
                Log.e(TAG, "FRP binary is not executable: " + frpBinary.getAbsolutePath());
                addLog("Error: FRP binary is not executable: " + frpBinary.getAbsolutePath());
                return;
            }
            
            Log.d(TAG, "Binary path: " + frpBinary.getAbsolutePath());
            Log.d(TAG, "Config path: " + configFile.getAbsolutePath());
            
            // 构建命令（移除--log-level参数，因为当前版本不支持）
            String[] command = new String[]{
                frpBinary.getAbsolutePath(),
                "-c",
                configFile.getAbsolutePath()
            };
            
            // 兼容Android 4.4.2的String.join替代方法
            StringBuilder commandStr = new StringBuilder();
            for (int i = 0; i < command.length; i++) {
                if (i > 0) commandStr.append(" ");
                commandStr.append(command[i]);
            }
            
            Log.d(TAG, "Executing command: " + commandStr.toString());
            addLog("Starting " + currentMode + " with command: " + commandStr.toString());
            
            frpProcess = Runtime.getRuntime().exec(command);
            
            // 启动日志收集器
            logCollector = new LogCollector(frpProcess.getInputStream(), "OUT");
            logCollector.start();
            
            // 启动错误日志收集器
            errorLogCollector = new LogCollector(frpProcess.getErrorStream(), "ERR");
            errorLogCollector.start();
            
            // 检查进程是否正常启动
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000); // 等待2秒
                        if (frpProcess != null) {
                            int exitValue = -1;
                            try {
                                exitValue = frpProcess.exitValue();
                                Log.e(TAG, currentMode + " exited immediately with code: " + exitValue);
                                addLog("Error: " + currentMode + " exited immediately with code: " + exitValue);
                            } catch (IllegalThreadStateException e) {
                                // 进程仍在运行
                                Log.d(TAG, currentMode + " is still running");
                                addLog(currentMode.toUpperCase() + " started successfully");
                                // 更新通知显示运行状态
                                updateNotification();
                            }
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Thread interrupted", e);
                    }
                }
            }).start();
            
            Log.i(TAG, currentMode + " started process");
        } catch (Exception e) {
            Log.e(TAG, "Error starting " + currentMode, e);
            addLog("Error starting " + currentMode + ": " + e.getMessage());
        }
    }
    
    private void updateNotification() {
        Notification notification = createNotification();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    private void stopFRPClient() {
        Log.d(TAG, "Stopping FRP client");
        if (logCollector != null) {
            logCollector.interrupt();
            logCollector = null;
        }
        
        if (errorLogCollector != null) {
            errorLogCollector.interrupt();
            errorLogCollector = null;
        }
        
        if (frpProcess != null) {
            frpProcess.destroy();
            frpProcess = null;
            Log.i(TAG, currentMode + " stopped");
            addLog(currentMode.toUpperCase() + " stopped");
        }
    }
    
    public String getCurrentMode() {
        return currentMode;
    }
    
    public ConcurrentLinkedQueue<String> getLogQueue() {
        return logQueue;
    }
    
    private void addLog(String message) {
        logQueue.offer("[" + System.currentTimeMillis() + "] " + message);
        // 限制队列大小，避免占用过多内存
        if (logQueue.size() > 1000) {
            logQueue.poll();
        }
    }
    
    // 日志收集线程
    private class LogCollector extends Thread {
        private InputStream inputStream;
        private String tag;
        
        public LogCollector(InputStream inputStream, String tag) {
            this.inputStream = inputStream;
            this.tag = tag;
        }
        
        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null && !isInterrupted()) {
                    String logMsg = "[" + tag + "] " + line;
                    Log.d(TAG, logMsg);
                    addLog(logMsg);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading " + tag + " log", e);
            }
        }
    }
}