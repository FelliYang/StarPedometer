package cc.xuziyang.startpedometer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements StepSensor.StepCallBack{
    private TextView stepText;
    private StepSensor stepSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stepText = (TextView) findViewById(R.id.step_text);

        // 开启计步监听, 分为加速度传感器、或计步传感器
        stepSensor = new StepSensorPedometer(this, this);
        if (!stepSensor.countStep()) {
            Toast.makeText(this, "计步传传感器不可用！", Toast.LENGTH_SHORT).show();
            stepSensor = new StepSensorAcceleration(this, this);
            if (!stepSensor.countStep()) {
                Toast.makeText(this, "加速度传感器不可用！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if(stepSensor.isAvailable) {
            stepSensor.unregisterStep();
        }
        super.onDestroy();
    }

    @Override
    public void Step(int stepNum) {
        //  计步回调
        stepText.setText("步数:" + stepNum);
    }
}
