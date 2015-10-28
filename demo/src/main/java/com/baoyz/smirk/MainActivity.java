package com.baoyz.smirk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.baoyz.smirk.demo.extension.TextExtension;
import com.baoyz.smirk.demo.extension.ToastExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private TextView mTvLog;
    private Smirk mSmirk;
    private TextExtension mTextExtension;
    private ToastExtension mToastExtension;

    private File mExtFile1;
    private File mExtFile2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initExtension();

        // 加载ext1.dex
        loadExtension(mExtFile1);

        mTvLog = (TextView) findViewById(R.id.tv_log);

        findViewById(R.id.bt_execute_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 执行Text扩展功能
                mTextExtension.showText(mTvLog);
            }
        });

        findViewById(R.id.bt_execute_toast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 执行Toast扩展功能
                mToastExtension.showToast(getApplicationContext());
            }
        });

        findViewById(R.id.bt_load_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 加载ext1.dex
                loadExtension(mExtFile1);
            }
        });

        findViewById(R.id.bt_load_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 加载ext2.dex
                loadExtension(mExtFile2);
            }
        });

        findViewById(R.id.bt_load_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 加载目录中所有的dex
                loadExtension(getCacheDir());
            }
        });
    }

    /**
     * 加载扩展组件
     */
    private void loadExtension(File dexPath) {
        mSmirk = new Smirk.Builder(this)
                .addDexPath(dexPath)
                .build();
        mTextExtension = mSmirk.create(TextExtension.class);
        mToastExtension = mSmirk.create(ToastExtension.class);
    }

    /**
     * 初始化扩展组件
     *
     * @return
     */
    private void initExtension() {
        // 为了方便演示，将提前放在assets目录中的dex扩展文件copy到cache目录，正常应该是从网络下载的dex文件
        try {
            mExtFile1 = createFileFromInputStream(getAssets().open("extensions/ext1.dex"), "ext1.dex");
            mExtFile2 = createFileFromInputStream(getAssets().open("extensions/ext2.dex"), "ext2.dex");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createFileFromInputStream(InputStream inputStream, String outName) {

        try {
            File file = new File(getCacheDir(), outName);
            if (file.exists())
                return file;
            OutputStream outputStream = new FileOutputStream(file);
            byte buffer[] = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
