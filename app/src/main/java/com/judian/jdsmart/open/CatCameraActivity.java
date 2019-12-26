package com.judian.jdsmart.open;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.judian.jdsmart.common.JdSmartConstant;

public class CatCameraActivity extends AppCompatActivity {
    private static final String TAG = "CatCameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cat_camera);
        getIntentExtra();
    }

    private void getIntentExtra(){
        if(getIntent().hasExtra(JdSmartConstant.EXTRA_STRING_FOR_JUDIAN_GOTO_CAMERA)){
            String cameraId = getIntent().getStringExtra(JdSmartConstant.EXTRA_STRING_FOR_JUDIAN_GOTO_CAMERA);
            Log.d(TAG, "cameraId=" + cameraId);
        }
    }
}
