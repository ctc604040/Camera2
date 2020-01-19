package com.example.tcs15034.camera;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.content.SharedPreferences;

/**
 * Created by tcs15034 on 2019/10/22.
 */

public class MenuActivity extends FragmentActivity {

    private Switch mFlashSwitch;
    private Button mOkButton;
    private Spinner mBrightnessSpinner;
    private Spinner mResolutionSpinner;
    public SharedPreferences mDataFlash;
    public SharedPreferences mDataBrightness;
    public SharedPreferences mDataResolution;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        mFlashSwitch = findViewById(R.id.switch_flash);
        mDataFlash = getSharedPreferences("DataFlash", MODE_PRIVATE);
        if (mDataFlash != null) {
            boolean dataFlashBoolean = mDataFlash.getBoolean("FlashStatus", true);
            mFlashSwitch.setChecked(dataFlashBoolean);
        }
        mFlashSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SharedPreferences.Editor editor = mDataFlash.edit();
                        editor.putBoolean("FlashStatus", isChecked);
                        editor.commit();
                    }
                }
        );

        mBrightnessSpinner = findViewById(R.id.spinner_brightness);
        mDataBrightness = getSharedPreferences("DataBrightness", MODE_PRIVATE);
        if (mDataBrightness != null) {
            int dataBrightnessInt = mDataBrightness.getInt("BrightnessStatus", 0);
            mBrightnessSpinner.setSelection(dataBrightnessInt);
        }
        mBrightnessSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = mDataBrightness.edit();
                editor.putInt("BrightnessStatus", position);
                editor.commit();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mResolutionSpinner = findViewById(R.id.spinner_resolution);
        mDataResolution = getSharedPreferences("DataResolution", MODE_PRIVATE);
        if (mDataResolution != null) {
            int dataResolutionInt = mDataResolution.getInt("ResolutionStatus", 0);
            mResolutionSpinner.setSelection(dataResolutionInt);
        }
        mResolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences.Editor editor = mDataResolution.edit();
                editor.putInt("ResolutionStatus", position);
                editor.commit();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mOkButton = findViewById(R.id.button_ok);
        mOkButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick (View v) {
                        finish();
                    }
                }
        );
    }
}
