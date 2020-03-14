package cc.xuziyang.startpedometer;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;

public class MeterDetect implements SensorEventListener {
    private final String TAG = MeterDetect.class.getName();
    // 数据库
    private MDataBase dbHelper;
    SQLiteDatabase db;

    private int SENSER_DELAY =  50;             // 控制采样间隔
    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
    private Context mContext;

    // 上次采样时间
    private long lastSampleTime=0;
    // 当前加速度
    public static float acceleration = 0;
    //上次的加速度
    float lastAcceleration = 0;
    //是否上升的标志位
    boolean isDirectionUp = false;
    //持续上升次数
    int continueUpCount = 0;
    //上一点的持续上升的次数，为了记录波峰的上升次数
    int continueUpFormerCount = 0;
    //上一点的状态，上升还是下降
    boolean lastStatus = false;

    //波峰值
    float peakOfWave = 0;
    //波谷值
    float valleyOfWave = 0;
    //此次波峰的时间
//    long timeOfThisPeak = 0;
    //上次波峰的时间
    long timeOfLastPeak = 0;
    //系统当前的时间
    long timeOfNow = 0;

    //初始阈(yu)值
    final float initialThreshold = (float) 1.7;
    // 动态阈值需要动态的数据，这个值用于这些动态数据的阈值
    float threadThreshold = (float) 2.0;

    /* 参数 */
    //用于存放计算阈值的波峰波谷差值
    final int valueNum = 5;
    float[] tempValue = new float[valueNum];
    int tempCount = 0;

    //初始范围
    float minValue = 11f;
    float maxValue = 19.6f;

    //步数
    public static int CURRENT_SETP=0;
    public static int TEMP_STEP = 0;
    private int lastStep = -1;

    private int pedometerState = 0;
    private Timer timer;
    // 倒计时2.0秒，2.0秒内不会显示计步，用于屏蔽细微波动
    private long duration = 2000;
    private TimeCount timeCount;

    /**
     * 自定义的接口，实时向外传递步数
     */
    OnSensorChangeListener onSensorChangeListener;
    private Handler handler;
    public interface OnSensorChangeListener {
        //当步数改变时,通知外部更新UI
        void onStepsListenerChange(int steps);
    }
    public MeterDetect(OnSensorChangeListener sensorChangeListener, Handler _handler){
        mContext = MApplication.getContext();
        handler = _handler;
        mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.setOnSensorChangeListener(sensorChangeListener);
        // 数据库初始化
        initDB();

    }

    private void initDB() {
        dbHelper = new MDataBase(mContext, "history.db", null, 1);
        db = dbHelper.getWritableDatabase();
        dbHelper.setDb(db);
        dbHelper.makeData();

        if(dbHelper.query( null)==0){
            logcat("not exist");
            dbHelper.insert(null,CURRENT_SETP);
        }else{
            logcat("exist");
//            dbHelper.update(CURRENT_SETP);
        }

        dbHelper.queryAll();

        CURRENT_SETP = MDataBase.allData[0].steps;

        Message message = new Message();
        message.what = MainActivity.SET_STEP;
        message.arg1 = CURRENT_SETP;
        logcat("CURRENT_STEP: "+CURRENT_SETP);
        handler.sendMessageDelayed(message,200);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //忽略加速度方向，取绝对值
        long time = System.currentTimeMillis();
        if (time-lastSampleTime<SENSER_DELAY){
            return;
        }
        lastSampleTime=time;
        acceleration = (float) Math.sqrt(Math.pow(sensorEvent.values[0], 2)
                + Math.pow(sensorEvent.values[1], 2) + Math.pow(sensorEvent.values[2], 2));
        timeOfNow = time;
        detectorNewStep(acceleration);
    }

    private void detectorNewStep(float values) {
        if (lastAcceleration == 0) {
            lastAcceleration = values;
        } else{
            if (DetectorPeak(values, lastAcceleration)){
                if((timeOfNow - timeOfLastPeak >200) &&
                        (peakOfWave-valleyOfWave>threadThreshold)&&(timeOfNow-timeOfLastPeak)<=2000){
                    timeOfLastPeak = timeOfNow;
                    perStep();
                }
                if((timeOfNow-timeOfLastPeak>200)&&
                        (peakOfWave-valleyOfWave>=initialThreshold)){
                    timeOfLastPeak = timeOfNow;
                    threadThreshold =  Peak_Valley_Thread(peakOfWave - valleyOfWave);
                }
            }
        }
        lastAcceleration = values;
    }

    private float Peak_Valley_Thread(float value) {
        float tempThreshold =threadThreshold;
        if(tempCount<valueNum){
            tempValue[tempCount]=value;
            tempCount++;
        }else{
            tempThreshold = averageValue(tempValue, valueNum);
            for (int i = 1; i < valueNum; i++) {
                tempValue[i - 1] = tempValue[i];
            }
            tempValue[valueNum - 1] = value;
        }
        return tempThreshold;
    }

    public float averageValue(float value[], int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / valueNum;
        if (ave >= 8) {
            ave = (float) 4.3;
        } else if (ave >= 7 && ave < 8) {
            ave = (float) 3.3;
        } else if (ave >= 4 && ave < 7) {
            ave = (float) 2.3;
        } else if (ave >= 3 && ave < 4) {
            ave = (float) 2.0;
        } else {
            ave = (float) 1.7;
        }
        return ave;
    }

    private void perStep() {

        if(pedometerState==0){
            timeCount = new TimeCount(duration, 1000);
            timeCount.start();
            logcat("计时器开启");
            lastStep = -1;
            TEMP_STEP = 0;
//            Toast.makeText(mContext,"检测到运动，计时器开启",Toast.LENGTH_LONG).show();
            pedometerState=1;
        }else if(pedometerState==1){
            // 待定状态
            TEMP_STEP++;
//            Toast.makeText(mContext,"暂时计步中...",Toast.LENGTH_LONG).show();
            logcat("暂时计步中 "+TEMP_STEP);
        }else if(pedometerState==2){
            CURRENT_SETP++;
            logcat(""+CURRENT_SETP);
//            Toast.makeText(mContext,"计步中...",Toast.LENGTH_LONG).show();
            if(null !=onSensorChangeListener)
                onSensorChangeListener.onStepsListenerChange(CURRENT_SETP);
        }
    }

    private boolean DetectorPeak(float newValue, float oldValue) {
        lastStatus = isDirectionUp;
        if(newValue>oldValue){
            isDirectionUp=true;
            continueUpCount++;
        }else{
            continueUpFormerCount = continueUpCount;
            continueUpCount=0;
            isDirectionUp=false;
        }
        if(!isDirectionUp&&lastStatus&&
                (continueUpFormerCount>=2&& (oldValue >= minValue && oldValue < maxValue))){
            peakOfWave = oldValue;
            return true;
        }else if(!lastStatus&&isDirectionUp){
            valleyOfWave = oldValue;
            return false;
        }else{
            return false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }



    private void logcat(String s){
        Log.v(TAG, s);
    }

    public void onResume() {
        mSensorManager.registerListener(this, mAccelerometer ,SENSER_DELAY);
    }

    public void onStop(){
//        mSensorManager.unregisterListener(this);
        dbHelper.update(CURRENT_SETP);
        logcat("onStop");
    }
    //设置监听，传入回调对象
    public void setOnSensorChangeListener(
            OnSensorChangeListener onSensorChangeListener) {
        this.onSensorChangeListener = onSensorChangeListener;
    }

    /**定时执行在一段时间后停止的倒计时，在倒计时执行过程中会在固定间隔时间得到通知。
     * :param  millisInFuture 倒计时时间，也就是从start到finish 的时间
     * :param countDownInterval, 从start开始，每隔一段时间调用onTick
     */
    class TimeCount extends CountDownTimer{
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);

        }

        @Override
        public void onTick(long l) {
            if(lastStep==TEMP_STEP){
                logcat("检测到没有持续移动，停止检测" + TEMP_STEP);
//                MainActivity.setInfoText("停止计步");
                timeCount.cancel();
                pedometerState = 0;
                lastStep=-1;
                TEMP_STEP = 0;
            }else {
                lastStep=TEMP_STEP;
            }
        }

        @Override
        public void onFinish() {
            timeCount.cancel();
            CURRENT_SETP+=TEMP_STEP;
            lastStep=-1;
            logcat("检测到持续移动，倒计时结束");
            timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    if(lastStep==CURRENT_SETP){
                        timer.cancel();
                        pedometerState = 0;
                        lastStep=-1;
                        TEMP_STEP=0;
                        logcat("停止计步");
//                        MainActivity.setInfoText("停止计步");
                    }else{
                        lastStep = CURRENT_SETP;
                    }
                }
            };
            timer.schedule(timerTask,0, 2000);
            pedometerState = 2;

        }
    }
    private void showToast(String s){
        Toast.makeText(mContext, s, Toast.LENGTH_LONG).show();
    }

}

