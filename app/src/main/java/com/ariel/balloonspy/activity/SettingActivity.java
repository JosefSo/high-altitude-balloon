package com.ariel.balloonspy.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CheckBox;
import android.widget.EditText;

import com.ariel.balloonspy.R;

public class SettingActivity extends AppCompatActivity {

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        mPrefs = getSharedPreferences("SpyBalloon", MODE_PRIVATE);
        mEditor = mPrefs.edit();

        // Alt 1
        EditText alt1TextEdit = (EditText)findViewById(R.id.alt1);
        alt1TextEdit.setText(mPrefs.getString("alt1", ""));

        // Diff 1
        EditText diff1TextEdit = (EditText)findViewById(R.id.diff1);
        diff1TextEdit.setText(mPrefs.getString("diff1", ""));

        // Alt 2
        EditText alt2TextEdit = (EditText)findViewById(R.id.alt2);
        alt2TextEdit.setText(mPrefs.getString("alt2", ""));

        // Diff 2
        EditText diff2TextEdit = (EditText)findViewById(R.id.diff2);
        diff2TextEdit.setText(mPrefs.getString("diff2", ""));

        // Alt 3
        EditText alt3TextEdit = (EditText)findViewById(R.id.alt3);
        alt3TextEdit.setText(mPrefs.getString("alt3", ""));

        // Diff 3
        EditText diff3TextEdit = (EditText)findViewById(R.id.diff3);
        diff3TextEdit.setText(mPrefs.getString("diff3", ""));

        // TextNow CheckBox
        //CheckBox textNowCheckBox = (CheckBox)findViewById(R.id.textNowCheckBox);
        //textNowCheckBox.setChecked(mPrefs.getBoolean("textNowCheckBox", false));

        // LogSize
        EditText logSizeTextEdit = (EditText)findViewById(R.id.logSize);
        logSizeTextEdit.setText(mPrefs.getString("logSize", ""));

        // minGPSUpdate
        EditText minGPSUpdateTextEdit = (EditText)findViewById(R.id.minGPSUpdate);
        minGPSUpdateTextEdit.setText(mPrefs.getString("minGPSUpdate", ""));

        // minCaptureVid
        EditText minCaptureVidTextEdit = (EditText)findViewById(R.id.minCaptureVid);
        minCaptureVidTextEdit.setText(mPrefs.getString("minCaptureVid", ""));

        // showToast CheckBox
        CheckBox showToastCheckBox = (CheckBox)findViewById(R.id.showToastCheckBox);
        showToastCheckBox.setChecked(mPrefs.getBoolean("showToastCheckBox", true));

        // batLevel
        EditText batLevelTextEdit = (EditText)findViewById(R.id.batLevel);
        batLevelTextEdit.setText(mPrefs.getString("batLevel", ""));

        // IPAddr CheckBox
        CheckBox ipAddrCheckBox = (CheckBox)findViewById(R.id.ipAddrCheckBox);
        ipAddrCheckBox.setChecked(mPrefs.getBoolean("ipAddrCheckBox", true));

        // ipAddr
        EditText ipAddrTextEdit = (EditText)findViewById(R.id.ipAddr);
        ipAddrTextEdit.setText(mPrefs.getString("ipAddr", ""));

        // geigerSnsCheckBox
        CheckBox geigerSnsCheckBox = (CheckBox)findViewById(R.id.geigerSnsCheckBox);
        geigerSnsCheckBox.setChecked(mPrefs.getBoolean("geigerSnsCheckBox", true));

        // picTime
        EditText picTimeTextEdit = (EditText)findViewById(R.id.picTime);
        picTimeTextEdit.setText(mPrefs.getString("picTime", ""));

        // vidTime
        EditText vidTimeTextEdit = (EditText)findViewById(R.id.vidTime);
        vidTimeTextEdit.setText(mPrefs.getString("vidTime", ""));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Alt 1
        EditText alt1EditText = (EditText)findViewById(R.id.alt1);
        mEditor.putString("alt1", alt1EditText.getText().toString());

        // Diff 1
        EditText diff1EditText = (EditText)findViewById(R.id.diff1);
        mEditor.putString("diff1", diff1EditText.getText().toString());

        // Alt 2
        EditText alt2EditText = (EditText)findViewById(R.id.alt2);
        mEditor.putString("alt2", alt2EditText.getText().toString());

        // Diff 2
        EditText diff2EditText = (EditText)findViewById(R.id.diff2);
        mEditor.putString("diff2", diff2EditText.getText().toString());

        // Alt 3
        EditText alt3EditText = (EditText)findViewById(R.id.alt3);
        mEditor.putString("alt3", alt3EditText.getText().toString());

        // Diff 3
        EditText diff3EditText = (EditText)findViewById(R.id.diff3);
        mEditor.putString("diff3", diff3EditText.getText().toString());

        // TextNow CheckBox
        //CheckBox textNowCheckBox = (CheckBox)findViewById(R.id.textNowCheckBox);
        //mEditor.putBoolean("textNowCheckBox",textNowCheckBox.isChecked());

        // LogSize
        EditText logSizeEditText = (EditText)findViewById(R.id.logSize);
        mEditor.putString("logSize", logSizeEditText.getText().toString());

        // minGPSUpdate
        EditText minGPSUpdateEditText = (EditText)findViewById(R.id.minGPSUpdate);
        mEditor.putString("minGPSUpdate", minGPSUpdateEditText.getText().toString());

        // minCaptureVid
        EditText minCaptureVidEditText = (EditText)findViewById(R.id.minCaptureVid);
        mEditor.putString("minCaptureVid", minCaptureVidEditText.getText().toString());

        // showToast CheckBox
        CheckBox showToastCheckBox = (CheckBox)findViewById(R.id.showToastCheckBox);
        mEditor.putBoolean("showToastCheckBox",showToastCheckBox.isChecked());

        // batLevel
        EditText batLevelEditText = (EditText)findViewById(R.id.batLevel);
        mEditor.putString("batLevel", batLevelEditText.getText().toString());

        // IPAddr CheckBox
        CheckBox ipAddrCheckBox = (CheckBox)findViewById(R.id.ipAddrCheckBox);
        mEditor.putBoolean("ipAddrCheckBox",ipAddrCheckBox.isChecked());

        // ipAddr
        EditText ipAddrEditText = (EditText)findViewById(R.id.ipAddr);
        mEditor.putString("ipAddr", ipAddrEditText.getText().toString());

        // IPAddr CheckBox
        CheckBox geigerSnsCheckBox = (CheckBox)findViewById(R.id.geigerSnsCheckBox);
        mEditor.putBoolean("geigerSnsCheckBox",geigerSnsCheckBox.isChecked());

        // picTime
        EditText picTimeEditText = (EditText)findViewById(R.id.picTime);
        mEditor.putString("picTime", picTimeEditText.getText().toString());

        // vidTime
        EditText vidTimeEditText = (EditText)findViewById(R.id.vidTime);
        mEditor.putString("vidTime", vidTimeEditText.getText().toString());

        // Save the settings here
        mEditor.commit();
    }
}
