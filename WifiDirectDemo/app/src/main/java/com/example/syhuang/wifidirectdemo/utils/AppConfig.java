package com.example.syhuang.wifidirectdemo.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.syhuang.wifidirectdemo.app.AppContext;

/**
 * Created by huangshuyuan on 2017/7/12.
 */

public class AppConfig {
    private static AppConfig appConfig;

    private SharedPreferences preferences;

//    /**
//     * App根目录.
//     */
//    public String APP_PATH_ROOT;

    private AppConfig() {
        preferences = AppContext.getAppContext().getSharedPreferences("skypine", Context.MODE_PRIVATE);

//        APP_PATH_ROOT = FileUtil.getRootPath(SkyPineApplication.getInstance()).getAbsolutePath() + File.separator +
//                "skypine";
    }

    //    public static AppConfig getInstance() {
    //        if (appConfig == null)
    //            synchronized (AppConfig.class) {
    //                if (appConfig == null)
    //                    appConfig = new AppConfig();
    //            }
    //        return appConfig;
    //    }
    public synchronized static AppConfig getInstance() {
        if (appConfig == null) {
            appConfig = new AppConfig();
        }
        return appConfig;
    }

/*
    public void initialize() {
        IOUtils.createFolder(APP_PATH_ROOT);
    }*/

    public void putInt(String key, int value) {
        preferences.edit().putInt(key, value).commit();
    }

    public int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    public void putString(String key, String value) {
        preferences.edit().putString(key, value).commit();
    }

    public String getString(String key, String defValue) {
        return preferences.getString(key, defValue);
    }

    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).commit();
    }



    public boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    public void putLong(String key, long value) {
        preferences.edit().putLong(key, value).commit();
    }

    public long getLong(String key, long defValue) {
        return preferences.getLong(key, defValue);
    }

    public void putFloat(String key, float value) {
        preferences.edit().putFloat(key, value).commit();
    }

    public float getFloat(String key, float defValue) {
        return preferences.getFloat(key, defValue);
    }


}
