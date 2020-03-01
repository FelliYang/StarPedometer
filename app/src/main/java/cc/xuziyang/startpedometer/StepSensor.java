package cc.xuziyang.startpedometer;

import android.app.Service;
import android.content.Context;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;

/**
 * 计步传感器抽象类，子类分为加速度传感器、或计步传感器
 */
public abstract class StepSensor implements SensorEventListener {
    private Context context;
    protected StepCallBack stepCallBack;
    protected SensorManager sensorManager;
    protected boolean isAvailable = false;

    public StepSensor(Context context, StepCallBack stepCallBack) {
        this.context = context;
        this.stepCallBack = stepCallBack;
    }

    public interface StepCallBack {
        /**
         * 计步回调
         */
        void Step(int stepNum);
    }

    /**
     * 开启计步
     */
    public boolean countStep() {
        sensorManager = (SensorManager)context.getSystemService(context.SENSOR_SERVICE);
        registerStepListener();
        return isAvailable;
    }

    /**
     * 注册计步监听器
     */
    protected abstract void registerStepListener();

    /**
     * 注销计步监听器
     */
    public abstract void unregisterStep();
}