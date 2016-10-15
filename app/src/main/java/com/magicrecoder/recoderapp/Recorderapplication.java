package com.magicrecoder.recoderapp;

import android.app.Application;
import android.util.Log;

import com.magicrecoder.greendao.DaoSession;
import com.magicrecoder.greendao.RecorderInfoDao;



/**
 * Created by Administrator on 2016-09-05.
 */
public class Recorderapplication extends Application {
    public Session mSession;
    public DaoSession daoSession;
    public RecorderInfoDao recorderinfoDao;
    private static final String TAG = "Lifecycle";
    @Override
    public void onCreate(){
        super.onCreate();
        mSession.init(this.getApplicationContext());
        daoSession = mSession.getInstance(this.getApplicationContext());
        recorderinfoDao = daoSession.getRecorderInfoDao();
    }
}
