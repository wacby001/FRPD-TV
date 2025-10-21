package com.example.droidfrpd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ConfigEditorActivity extends Activity {
    
    private EditText configEditText;
    private String configFileName;
    private File configFile;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_editor);
        
        configEditText = findViewById(R.id.config_edit_text);
        
        Intent intent = getIntent();
        configFileName = intent.getStringExtra("config_file");
        if (configFileName == null) {
            configFileName = "frpc.toml";
        }
        
        // 确保使用.toml扩展名
        if (!configFileName.endsWith(".toml")) {
            int lastDot = configFileName.lastIndexOf('.');
            if (lastDot > 0) {
                configFileName = configFileName.substring(0, lastDot) + ".toml";
            } else {
                configFileName = configFileName + ".toml";
            }
        }
        
        configFile = new File(getFilesDir(), configFileName);
        loadConfigFile();
    }
    
    private void loadConfigFile() {
        try {
            if (configFile.exists()) {
                FileInputStream fis = new FileInputStream(configFile);
                byte[] data = new byte[(int) configFile.length()];
                fis.read(data);
                fis.close();
                String configContent = new String(data);
                configEditText.setText(configContent);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error reading config file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void saveConfigFile() {
        try {
            FileOutputStream fos = new FileOutputStream(configFile);
            String configContent = configEditText.getText().toString();
            fos.write(configContent.getBytes());
            fos.close();
            Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving config file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.config_editor_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveConfigFile();
                return true;
            case R.id.action_close:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}