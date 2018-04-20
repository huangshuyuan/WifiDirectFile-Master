package com.example.syhuang.wifidirectdemo.utils;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */
public class ToastUtils {

    static Toast toast = null;
    public static void show(Context context, String text) {
        try {
            if(toast!=null){
                toast.setText(text);
            }else{
                toast= Toast.makeText(context, text, Toast.LENGTH_SHORT);
            }
            toast.show();
        } catch (Exception e) {//子线程中Toast异常情况处理
            Looper.prepare();
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }
}
