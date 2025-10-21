package com.example.droidfrpd;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LogActivity extends Activity {
    
    private ListView logListView;
    private Button clearLogButton;
    private Spinner logLevelSpinner;
    private ArrayAdapter<String> logAdapter;
    private List<String> logList = new ArrayList<>();
    private FRPService frpService;
    private boolean bound = false;
    private Handler handler = new Handler();
    private Runnable logUpdater;
    private SharedPreferences prefs;
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            FRPService.LocalBinder binder = (FRPService.LocalBinder) service;
            frpService = binder.getService();
            bound = true;
            updateLogs();
        }
        
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        
        initViews();
        setupListeners();
        
        prefs = getSharedPreferences("FRPPrefs", MODE_PRIVATE);
        
        // 绑定到FRP服务
        Intent intent = new Intent(this, FRPService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        // 设置日志级别选择器
        String[] logLevels = {"trace", "debug", "info", "warn", "error"};
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, logLevels);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        logLevelSpinner.setAdapter(levelAdapter);
        
        // 设置默认日志级别
        String currentLogLevel = prefs.getString("log_level", "info");
        for (int i = 0; i < logLevels.length; i++) {
            if (logLevels[i].equals(currentLogLevel)) {
                logLevelSpinner.setSelection(i);
                break;
            }
        }
        
        // 定期更新日志
        logUpdater = new Runnable() {
            @Override
            public void run() {
                updateLogs();
                handler.postDelayed(this, 1000); // 每秒更新一次
            }
        };
        handler.post(logUpdater);
    }
    
    private void initViews() {
        logListView = findViewById(R.id.log_listview);
        clearLogButton = findViewById(R.id.clear_log_button);
        logLevelSpinner = findViewById(R.id.log_level_spinner);
        
        logAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logList);
        logListView.setAdapter(logAdapter);
    }
    
    private void setupListeners() {
        clearLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLogs();
            }
        });
    }
    
    private void updateLogs() {
        if (bound && frpService != null) {
            ConcurrentLinkedQueue<String> logQueue = frpService.getLogQueue();
            boolean updated = false;
            
            while (!logQueue.isEmpty()) {
                String logEntry = logQueue.poll();
                logList.add(logEntry);
                updated = true;
            }
            
            // 限制日志条目数量
            while (logList.size() > 500) {
                logList.remove(0);
                updated = true;
            }
            
            if (updated) {
                logAdapter.notifyDataSetChanged();
                // 自动滚动到底部
                logListView.setSelection(logAdapter.getCount() - 1);
            }
        }
    }
    
    private void clearLogs() {
        logList.clear();
        logAdapter.notifyDataSetChanged();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
        if (handler != null && logUpdater != null) {
            handler.removeCallbacks(logUpdater);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.log_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_log_level:
                saveLogLevel();
                return true;
            case R.id.action_close:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void saveLogLevel() {
        String selectedLevel = (String) logLevelSpinner.getSelectedItem();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("log_level", selectedLevel);
        editor.apply();
    }
}