package cc.xuziyang.startpedometer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.jaeger.library.StatusBarUtil;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.class.getName();
    private SharedPreferences preferences;

    private int target=100;   //目标步数
    static final int SET_STEP = 1;
    static final int SET_TARGET = 2;
    private MeterDetect meterDetect;
    private ProgressBar progressBar;
    private TextView stepTextView;
//    private int stepNum;
    public Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case SET_STEP:
                    int steps = msg.arg1;
                    progressBar.setProgress(steps*100/target);
                    stepTextView.setText(""+steps);
                    break;
                case SET_TARGET:
                    target = msg.arg1;
                    progressBar.setProgress(MeterDetect.CURRENT_SETP*100/target);
                    logcat(""+MeterDetect.CURRENT_SETP*100/target+"  "+target);
                    break;
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        //沉浸式设置
        TextView a = findViewById(R.id.textView2);
        StatusBarUtil.setTranslucentForImageView(this, 0, a);
        StatusBarUtil.setTranslucentForImageView(this, 0, toolbar);

        //
        initUI();
        //
        initConfig();
        // 设置计步器
        meterDetect = new MeterDetect(new MeterDetect.OnSensorChangeListener() {
            @Override
            public void onStepsListenerChange(int steps) {
                progressBar.setProgress(steps*100/target);
                stepTextView.setText(""+steps);
//                stepNum = steps;
            }
        }, handler);
        meterDetect.onResume();
    }

    private void initConfig() {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        target = preferences.getInt("target", 100);
    }

    private void initUI() {
        progressBar = findViewById(R.id.progressBar);
        stepTextView = findViewById(R.id.step_text);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (@NonNull MenuItem item){
        switch (item.getItemId()) {
            case R.id.setting:
                setTargetMeter();
                break;
            case R.id.history:
                Intent intent = new Intent(MainActivity.this, HistoryData.class);
                startActivity(intent);
                break;
        }
        return true;
    }


    private void setTargetMeter () {
        /**获取引用setting.xml配置文件中的视图组件*/
        final View view = getLayoutInflater().inflate(R.layout.setting, null);

        /**这里使用链式写法创建了一个AlertDialog对话框,并且把应用到得视图view放入到其中*/

        /**添加AlertDialog的按钮,并且设置按钮响应事件*/
        new AlertDialog.Builder(MainActivity.this).setTitle("设置目标步数").setView(view)
                .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        /**获取用户输入的步数*/
                        EditText nameEditText = (EditText) view.findViewById(R.id.target_meter);
                        /**保存用户输入的步数*/
                        String meters = nameEditText.getText().toString();
                        Message message = new Message();
                        message.what = SET_TARGET;
                        message.arg1 = Integer.parseInt(meters);
                        handler.sendMessage(message);
                        Toast.makeText(MainActivity.this, "目标步数" + meters, Toast.LENGTH_SHORT).show();
                        // 保存用户设置
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt("target", Integer.parseInt(meters));
                        editor.apply();
                    }
                    /**添加对话框的退出按钮,并且设置按钮响应事件*/
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Toast.makeText(MainActivity.this, "取消设置", Toast.LENGTH_LONG).show();
            }
        }).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        meterDetect.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        meterDetect.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        meterDetect.onResume();
//        progressBar.setProgress(stepNum/target*100);
//        stepTextView.setText(""+stepNum);
    }
    private void logcat(String s){
        Log.v(TAG, s);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

