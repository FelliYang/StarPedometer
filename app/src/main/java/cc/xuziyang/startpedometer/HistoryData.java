package cc.xuziyang.startpedometer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.material.tabs.TabLayout;
import com.jaeger.library.StatusBarUtil;

import java.util.ArrayList;
import java.util.List;

public class HistoryData extends AppCompatActivity implements OnChartValueSelectedListener{

    private LineChart chart;
    public MDataBase.Item[] allData;
    private final String TAG = HistoryData.class.getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_data);
//        //启用黑色状态栏字体
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        StatusBarUtil.setTranslucent(this, 99);

        Toolbar toolbar2 = findViewById(R.id.toolbar2);
        toolbar2.setTitle("");
        setSupportActionBar(toolbar2);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.back2);
        }

        {   // // Chart Style // //
            chart = findViewById(R.id.chart);

            // background color
            chart.setBackgroundColor(0x00000000); //全透明，使用布局的背景
            chart.setDrawGridBackground(false);

            // disable description text
            Description desc = new Description();
            desc.setText("运动记录");
            desc.setTextColor(0x66FFFFFF);
            desc.setPosition(1000,100);
            chart.setDescription(desc);
            chart.getDescription().setEnabled(true);

            // enable touch gestures
            chart.setTouchEnabled(true);
            // set listeners
            chart.setOnChartValueSelectedListener(this);

            // enable scaling and dragging
            chart.setDragEnabled(true);
            chart.setScaleEnabled(true);

            //forbit x scale and fortbit y scale
            chart.setScaleXEnabled(true);
            chart.setScaleYEnabled(false);
            // forbid pinch zoom along both axis
            chart.setPinchZoom(false);
        }

        XAxis xAxis;
        {   // // X-Axis Style // //
            xAxis = chart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextColor(0x66FFFFFF);
            //don't draw the line vertically
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(false);
            // 设置x刻度
//            xAxis.setLabelCount(10,false);
            // 让x轴上自定义的值和折线上相对应
//            xAxis.setGranularity(1);
        }

        YAxis yAxis;
        {   // // Y-Axis Style // //
            yAxis = chart.getAxisLeft();
            yAxis.setDrawZeroLine(false);
            yAxis.setTextColor(0x66FFFFFF);
            // disable dual axis (only use LEFT axis)
            chart.getAxisRight().setEnabled(false);
        }

        allData = MDataBase.allData;
        logcat(""+allData.length);
        setData();


        // draw points over time
        chart.animateY(1500);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        // draw legend entries as lines
        l.setForm(Legend.LegendForm.LINE);

    }

    private void setData() {

        ArrayList<Entry> values = new ArrayList<>();
        int range = 180;
        /*在此处添加历史数据*/
        for (int i = allData.length-1; i >=0; i--) {
//        for( int i = 0; i<allData.length; i++){
            int val = allData[i].steps;
            values.add(new Entry(29-i, val));
        }

        LineDataSet set1;

        if (chart.getData() != null &&
                chart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            set1.notifyDataSetChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(values,"");
            set1.setDrawIcons(true);

            // set the shape of line
            set1.disableDashedLine();
            set1.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

            // set the Color of line
            set1.setColor(0xAA03A9F4);

            // line thickness and point size
            set1.setLineWidth(1f);

            //set the text value Color
            set1.setValueTextColor(0x66FFFFFF);

            // text size of values
            set1.setValueTextSize(9f);

            //disable the text value
            set1.setDrawValues(false);

            // customize legend entry
            set1.setFormLineWidth(10f);
//            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(0);


            // draw selection line as dashed
            set1.enableDashedHighlightLine(10f, 5f, 0f);

            // set the filled area
            set1.setDrawFilled(true);
            set1.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return chart.getAxisLeft().getAxisMinimum();
                }
            });

            // set color of filled area
            if (Utils.getSDKInt() >= 18) {
                // drawables only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_blue);
                set1.setFillDrawable(drawable);
            } else {
                set1.setFillColor(Color.BLACK);
            }

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the data sets

            // create a data object with the data sets
            LineData data = new LineData(dataSets);

            // set data
            chart.setData(data);
        }
    }

    /*在此处更新上方的textView*/

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        //选中chart中的点
        Toast.makeText(this, "选中了一个点", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected() {
        //取消选中
        Toast.makeText(this, "取消选中", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }
    private void logcat(String s){
        Log.v(TAG, s);
    }
}
