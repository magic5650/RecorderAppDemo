package com.magicrecoder.recoderapp;

import android.content.Context;
import android.os.Environment;

import com.magicrecoder.greendao.DaoMaster;
import com.magicrecoder.greendao.DaoSession;

import java.io.File;

/**
 * Created by Administrator on 2016-09-05.
 */
public class Session {
    private static DaoMaster daoMaster;
    private static DaoSession daoSession;
    private final static String DB_NAME = Environment.getExternalStorageDirectory()+"/RecorderInfo/recorder_db";
    private Session() {
    }
    /**
     * 初始化Dao的master，session对象
     * @param context Context
     */
    public static void init(Context context){
        if (daoMaster == null) {
            File DBPath = new File(Environment.getExternalStorageDirectory(), "/RecorderInfo");
            if (!DBPath.exists()) {
                DBPath.getParentFile().mkdirs();
            }
            DaoMaster.OpenHelper helper;
            helper = new DaoMaster.DevOpenHelper(context,
                    DB_NAME, null);
            daoMaster = new DaoMaster(helper.getWritableDatabase());
        }
        daoSession = daoMaster.newSession();
    }
    /**
     * 得到DaoMaster对象
     * @param context Context
     * @return DaoMaster
     */
    private static DaoMaster getDaoMaster (Context context) {
        if (daoMaster == null) {
            DaoMaster.OpenHelper helper;
            helper = new DaoMaster.DevOpenHelper(context, DB_NAME, null);
            daoMaster = new DaoMaster(helper.getWritableDatabase());
        }
        return daoMaster;
    }
    /**
     * 得到DaoSession对象
     * @param context Context
     * @return DaoSession
     */
    private static DaoSession getDaoSession (Context context) {
        if (daoSession == null) {
            DaoMaster daoMaster = getDaoMaster(context);
            daoSession = daoMaster.newSession();
        }
        return daoSession;
    }
    /**
     * 得到DaoSession，保证应用只有一个Session对象 <br/>
     * note: 如果没有其他需求，获取DaoSession对象只有此唯一方式
     * @param context Context
     * @return DaoSession
     */
    public static DaoSession getInstance(Context context) {
        if (daoSession == null) {
            daoSession = getDaoSession(context);
        }
        return daoSession;
    }
}
