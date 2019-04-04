package com.xiaomu.xiaomuutilapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.xiaomu.zxinglib.CaptureActivity;

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

    }

    public void clickCamera(View view){
        Intent intent = new Intent();
        intent.setClass(this, CaptureActivity.class);
        startActivity(intent);
    }

}
