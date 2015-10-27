package com.baoyz.smirk;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends AppCompatActivity {

    private Plugin mPlugin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File file = new File(getFilesDir(), "plugin.dex");
        try {
            FileOutputStream out = new FileOutputStream(file);
            FileInputStream in = new FileInputStream("/sdcard/plugin.dex");
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File dexPath = new File(Environment.getExternalStorageDirectory(), "Plugin.dex");

        Smirk smirk = new Smirk.Builder(this).addDexPath(file).build();
        mPlugin = smirk.create(Plugin.class);
        mPlugin.onCreate(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlugin.onDestory(this);
    }
}
