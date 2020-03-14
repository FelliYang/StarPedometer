package cc.xuziyang.startpedometer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.security.PublicKey;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Random;

public class MDataBase extends SQLiteOpenHelper {
    private final String tableName = "History";
    private final String CREATE_TABLE = "create table History(" +
            "id integer primary key autoincrement," +
            "mDate date," +
            "steps integer)";
    private final String TAG = MDataBase.class.getName();
    private SQLiteDatabase db;

    private Context mContext;
    public MDataBase(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        Toast.makeText(mContext, "数据库创建成功", Toast.LENGTH_LONG).show();
    }

    public void setDb(SQLiteDatabase _db){
        db = _db;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("drop table if exists History");
        onCreate(db);
    }

    public void insert(String dat, int steps){
        if(dat==null){
            dat = getDateNow();
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("mDate", dat);
        contentValues.put("steps",steps);
        db.insert(tableName, null, contentValues);

    }

    public void update(int steps){
        String dat = getDateNow();

        db.execSQL("update History set steps = ? where mDate = ?", new String[]{""+steps, dat});
//        logcat("update  History set steps = ? where mDate = ? "+ steps+" "+dat);
    }

    public int query(String dat){
        if(dat==null){
            dat = getDateNow();
        }
        Cursor cursor = db.rawQuery("select * from History where mDate=?", new String[]{dat});
//        logcat("queryNow: "+cursor.getCount());
        return cursor.getCount();
    }

    public static Item[]allData;

    public void queryAll(){
//        Cursor cursor = db.query(tableName, null,null,null,null,null,null);
        Cursor cursor = db.rawQuery("select * from History order by mDate desc",null);
        allData = new Item[30];
        int i=0;
        if(cursor.moveToFirst()){
            do{
                // 遍历
                String date = cursor.getString(cursor.getColumnIndex("mDate"));
                int steps = cursor.getInt(cursor.getColumnIndex("steps"));
                Item item = new Item(date, steps);
                if (i==30)
                    break;
                allData[i]=item;
                logcat(""+i+": "+item.date+" \t"+item.steps+'\n');
                i++;
                cursor.moveToNext();
            }while (!cursor.isAfterLast());
        }
        logcat("size:  "+cursor.getCount());
        cursor.close();
    }
    public class Item{
        public Item(String _date, int _steps){
            date = _date;
            steps = _steps;
        }
        public String date;
        public int steps;
    }
    public String getDateNow(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// HH:mm:ss
        //获取当前时间
        Date date = new Date(System.currentTimeMillis());
        String dat = simpleDateFormat.format(date);
        dat = dat.substring(0, 10);
        return dat;
    }
    private void logcat(String s){
        Log.v(TAG, s);
    }

    private final int [] dayamouth = {31, 29, 31,30,31,30,31,31,30,31,30,31};
    public void makeData(){
        String dat = getDateNow();
        int mouth = Integer.parseInt(dat.substring(5,7));
        int day = Integer.parseInt(dat.substring(8));
        Random random = new Random();
        Formatter formatter;
        int meter;
        for (int i=0;i <29 ;i++){
            meter = random.nextInt(8000)+1000;
            if(day-1 == 0){
                mouth -= 1;
                day = dayamouth[mouth];
            }else{
                day --;
            }
            formatter = new Formatter();
            formatter.format("2020-%02d-%02d", mouth, day);
//            logcat(formatter.toString()+"  "+meter);
            dat = formatter.toString();
            if(query(dat)==0){
                insert(dat, meter);
            }
        }

    }
}
