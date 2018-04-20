package com.example.syhuang.wifidirectdemo.core.entity;

import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */
public class FileInfo implements Serializable {

    /**
     * 常见文件拓展名
     */
    public static final String EXTEND_APK  = ".apk";
    public static final String EXTEND_JPEG = ".jpeg";
    public static final String EXTEND_JPG  = ".jpg";
    public static final String EXTEND_PNG  = ".png";
    public static final String EXTEND_MP3  = ".mp3";
    public static final String EXTEND_MP4  = ".mp4";

    /**
     * 自定义文件类型
     */
    public static final int TYPE_APK = 1;
    public static final int TYPE_JPG = 2;
    public static final int TYPE_MP3 = 3;
    public static final int TYPE_MP4 = 4;


    /**
     * 文件传输的标识
     */
    // 1 成功  -1 失败
    public static final int FLAG_SUCCESS = 1;
    public static final int FLAG_DEFAULT = 0;
    public static final int FLAG_FAILURE = -1;
    /**
     * 文件id
     */
    private String id;


    //必要属性
    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件类型
     */
    private int fileType;

    /**
     * 文件大小
     */
    private long size;

    //非必要属性
    /**
     * 文件显示名称
     */
    private String name;

    /**
     * 文件大小描述
     */
    private String sizeDesc;

    /**
     * 文件缩略图 (mp4与apk可能需要)
     */
    private Bitmap bitmap;

    /**
     * 文件额外信息
     */
    private String extra;


    /**
     * 已经处理的（读或者写）
     */
    private long procceed;

    /**
     * 文件传送的结果
     */
    private int result;


    public FileInfo() {

    }

    public FileInfo(String filePath, long size) {
        this.filePath = filePath;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSizeDesc() {
        return sizeDesc;
    }

    public void setSizeDesc(String sizeDesc) {
        this.sizeDesc = sizeDesc;
    }

    public long getProcceed() {
        return procceed;
    }

    public void setProcceed(long procceed) {
        this.procceed = procceed;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public static String toJsonStr(FileInfo fileInfo) {
        String jsonStr = "";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fileId", fileInfo.getId());
            jsonObject.put("fileName", fileInfo.getName());
            jsonObject.put("filePath", fileInfo.getFilePath());
            jsonObject.put("fileType", fileInfo.getFileType());
            jsonObject.put("size", fileInfo.getSize());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    public static FileInfo toObject(String jsonStr) {
        FileInfo fileInfo = new FileInfo();
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            String id = (String) jsonObject.get("fileId");
            String fileName = (String) jsonObject.get("fileName");
            String filePath = (String) jsonObject.get("filePath");
            long size = jsonObject.getLong("size");
            int type = jsonObject.getInt("fileType");
            fileInfo.setId(id);
            fileInfo.setName(fileName);
            fileInfo.setFilePath(filePath);
            fileInfo.setSize(size);
            fileInfo.setFileType(type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return fileInfo;
    }

    public static String toJsonArrayStr(List<FileInfo> fileInfoList) {
        JSONArray jsonArray = new JSONArray();
        if (fileInfoList != null) {
            for (FileInfo fileInfo : fileInfoList) {
                if (fileInfo != null) {
                    try {
                        jsonArray.put(new JSONObject(toJsonStr(fileInfo)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return jsonArray.toString();
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "filePath='" + filePath + '\'' +
                ", fileType=" + fileType +
                ", size=" + size +
                '}';
    }
}
