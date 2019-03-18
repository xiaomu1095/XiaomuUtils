package com.dyz.pumei.xiaomuutilapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.dyz.pumei.zxinglibrary.CaptureActivity;

/**
 * @author xiaom
 * Time: 2019/3/15 13:28
 * Author: Lin.Li
 * Description:
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent();
        intent.setClass(this, CaptureActivity.class);
        startActivity(intent);
    }

}
