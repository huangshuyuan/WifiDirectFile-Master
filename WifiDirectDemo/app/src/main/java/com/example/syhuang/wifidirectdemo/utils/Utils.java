package com.example.syhuang.wifidirectdemo.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */

public class Utils {
    static String TAG = "Utils";

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }

    /**
     * 格式化文件大小
     *
     * @param length
     * @return
     */
    public static String getFormatFileSize(long length) {
        DecimalFormat df = new DecimalFormat("#0.0");
        double size = ((double) length) / (1 << 30);
        if (size >= 1) {
            return df.format(size) + "GB";
        }
        size = ((double) length) / (1 << 20);
        if (size >= 1) {
            return df.format(size) + "MB";
        }
        size = ((double) length) / (1 << 10);
        if (size >= 1) {
            return df.format(size) + "KB";
        }
        return length + "B";
    }

    //将数据保存到本地
    public static void saveObject(String key, Object object) {

        try {
            String path = Environment.getExternalStorageDirectory() + File.separator + key + ".data";//设置路径
            FileOutputStream fileOutputStream;//打开文件输出流
            ObjectOutputStream objectOutputStream;//打开对象输出流
            File file = new File(path);//新建文件
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            fileOutputStream = new FileOutputStream(file.toString());//将新建的文件写入文件输出流
            objectOutputStream = new ObjectOutputStream(fileOutputStream);//向对象输出流写入文件输出流
            objectOutputStream.writeObject(object);//将序列化后的对象写入对象输出流
            objectOutputStream.close();//关闭对象输出流
            fileOutputStream.close();//关闭文件输出流
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //读取key 读取Wifi对象
    public static Object getObject(String key) {
        try {
            String path = Environment.getExternalStorageDirectory() + File.separator + key + ".data";//设置路径
            Object object;//声明对象
            File file = new File(path);//新建文件
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.exists()) {
                //如果文件存在
                FileInputStream fileInputStream;//打开文件输入流
                ObjectInputStream objectInputStream;//打开对象输入流
                fileInputStream = new FileInputStream(file.toString());//将新建的文件写入文件输入流
                objectInputStream = new ObjectInputStream(fileInputStream);//将文件输入流写入对象输入流
                object = (Object) objectInputStream.readObject();//获取对象输入流的对象
                objectInputStream.close();//关闭对象输入流
                fileInputStream.close();//关闭文件输入流
                return object;
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
