package com.example.droidfrpd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    
    private static final String TAG = "MainActivity";
    private static final int PICK_CONFIG_FILE_REQUEST = 1;
    
    private Button startButton;
    private Button stopButton;
    private Button importConfigButton;
    private Button editConfigButton;
    private Button viewLogsButton;
    private RadioButton clientModeRadio;
    private RadioButton serverModeRadio;
    private ToggleButton autoStartToggle;
    private TextView statusText;
    private FRPService frpService;
    private boolean isServiceBound = false;
    
    private static final String PREFS_NAME = "FRPPrefs";
    private static final String PREF_AUTO_START = "auto_start";
    private static final String PREF_MODE = "mode";
    
    private String currentMode = "frpc"; // 默认为客户端模式
    
    // 服务连接用于检查服务状态
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            FRPService.LocalBinder binder = (FRPService.LocalBinder) service;
            frpService = binder.getService();
            isServiceBound = true;
            updateServiceStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            isServiceBound = false;
            frpService = null;
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        
        initViews();
        setupListeners();
        loadPreferences();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        // 绑定到服务以检查其状态
        Intent intent = new Intent(this, FRPService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // 解绑服务
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
    
    private void initViews() {
        Log.d(TAG, "initViews");
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        importConfigButton = findViewById(R.id.import_config_button);
        editConfigButton = findViewById(R.id.edit_config_button);
        viewLogsButton = findViewById(R.id.view_logs_button);
        clientModeRadio = findViewById(R.id.client_mode_radio);
        serverModeRadio = findViewById(R.id.server_mode_radio);
        autoStartToggle = findViewById(R.id.auto_start_toggle);
        statusText = findViewById(R.id.status_text);
    }
    
    private void setupListeners() {
        Log.d(TAG, "setupListeners");
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Start button clicked");
                startFRPService();
            }
        });
        
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Stop button clicked");
                stopFRPService();
            }
        });
        
        importConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Import config button clicked");
                importConfigFile();
            }
        });
        
        editConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Edit config button clicked");
                editConfigFile();
            }
        });
        
        viewLogsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "View logs button clicked");
                viewLogs();
            }
        });
        
        clientModeRadio.setOnCheckedChangeListener(new RadioButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "Client mode radio changed: " + isChecked);
                if (isChecked) {
                    currentMode = "frpc";
                    savePreferences();
                }
            }
        });
        
        serverModeRadio.setOnCheckedChangeListener(new RadioButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "Server mode radio changed: " + isChecked);
                if (isChecked) {
                    currentMode = "frps";
                    savePreferences();
                }
            }
        });
        
        autoStartToggle.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "Auto start toggle changed: " + isChecked);
                savePreferences();
                if (isChecked) {
                    Toast.makeText(MainActivity.this, "Auto-start enabled", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Auto-start enabled for " + currentMode);
                } else {
                    Toast.makeText(MainActivity.this, "Auto-start disabled", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Auto-start disabled");
                }
            }
        });
    }
    
    private void updateServiceStatus() {
        Log.d(TAG, "updateServiceStatus");
        if (isServiceBound && frpService != null) {
            // 检查服务中的FRP进程是否正在运行
            if (frpService.isRunning()) {
                statusText.setText(frpService.getCurrentMode().toUpperCase() + " Service Running");
            } else {
                statusText.setText(currentMode.toUpperCase() + " Service Stopped");
            }
        } else {
            statusText.setText(currentMode.toUpperCase() + " Service Stopped");
        }
    }
    
    private void startFRPService() {
        Log.d(TAG, "startFRPService, mode: " + currentMode);
        Intent intent = new Intent(this, FRPService.class);
        intent.putExtra("mode", currentMode);
        startService(intent);
        statusText.setText(currentMode.toUpperCase() + " Service Started");
        Toast.makeText(this, currentMode.toUpperCase() + " Service Started", Toast.LENGTH_SHORT).show();
    }
    
    private void stopFRPService() {
        Log.d(TAG, "stopFRPService, mode: " + currentMode);
        Intent intent = new Intent(this, FRPService.class);
        stopService(intent);
        statusText.setText(currentMode.toUpperCase() + " Service Stopped");
        Toast.makeText(this, currentMode.toUpperCase() + " Service Stopped", Toast.LENGTH_SHORT).show();
    }
    
    private void importConfigFile() {
        Log.d(TAG, "importConfigFile, mode: " + currentMode);
        String configFileName = currentMode + ".toml";
        
        // 创建文件选择意图
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(
                Intent.createChooser(intent, "Select Configuration File"),
                PICK_CONFIG_FILE_REQUEST
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a file manager", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_CONFIG_FILE_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    importConfigurationFile(uri);
                }
            }
        }
    }
    
    private void importConfigurationFile(Uri uri) {
        try {
            // 获取目标文件路径
            String configFileName = currentMode + ".toml";
            final File configFile = new File(getFilesDir(), configFileName);
            final Uri finalUri = uri;
            
            // 显示确认对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm Import");
            builder.setMessage("Are you sure you want to import this configuration file? This will overwrite your current " + configFileName + " file.");
            builder.setPositiveButton("Import", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        // 复制文件
                        InputStream inputStream = getContentResolver().openInputStream(finalUri);
                        OutputStream outputStream = new FileOutputStream(configFile);
                        
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                        
                        inputStream.close();
                        outputStream.close();
                        
                        Toast.makeText(MainActivity.this, "Configuration imported successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Configuration imported successfully to " + configFile.getAbsolutePath());
                    } catch (IOException e) {
                        Log.e(TAG, "Error importing configuration file", e);
                        Toast.makeText(MainActivity.this, "Error importing configuration: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error importing configuration file", e);
            Toast.makeText(this, "Error importing configuration: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void editConfigFile() {
        Log.d(TAG, "editConfigFile, mode: " + currentMode);
        String configFileName = currentMode + ".toml";
        Intent intent = new Intent(this, ConfigEditorActivity.class);
        intent.putExtra("config_file", configFileName);
        startActivity(intent);
    }
    
    private void viewLogs() {
        Log.d(TAG, "viewLogs");
        Intent intent = new Intent(this, LogActivity.class);
        startActivity(intent);
    }
    
    private void savePreferences() {
        Log.d(TAG, "savePreferences, mode: " + currentMode);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_AUTO_START, autoStartToggle.isChecked());
        editor.putString(PREF_MODE, currentMode);
        editor.apply();
        
        Log.d(TAG, "Preferences saved - autoStart: " + autoStartToggle.isChecked() + ", mode: " + currentMode);
    }
    
    private void loadPreferences() {
        Log.d(TAG, "loadPreferences");
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean autoStart = prefs.getBoolean(PREF_AUTO_START, false);
        currentMode = prefs.getString(PREF_MODE, "frpc");
        
        Log.d(TAG, "Loaded preferences - autoStart: " + autoStart + ", mode: " + currentMode);
        
        autoStartToggle.setChecked(autoStart);
        
        if (currentMode.equals("frpc")) {
            clientModeRadio.setChecked(true);
        } else {
            serverModeRadio.setChecked(true);
        }
        
        // 显示当前设置
        if (autoStart) {
            Toast.makeText(this, "Auto-start is enabled for " + currentMode, Toast.LENGTH_SHORT).show();
        }
    }
    
    // 开机自启相关方法
    public boolean isAutoStartEnabled() {
        return autoStartToggle.isChecked();
    }
    
    public String getCurrentMode() {
        return currentMode;
    }
}