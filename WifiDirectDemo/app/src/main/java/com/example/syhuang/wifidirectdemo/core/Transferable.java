package com.example.syhuang.wifidirectdemo.core;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */
public interface Transferable {


    /**
     * @throws Exception
     */
    void init() throws Exception;


    /**
     * @throws Exception
     */
    void parseHeader() throws Exception;


    /**
     * @throws Exception
     */
    void parseBody() throws Exception;


    /**
     * @throws Exception
     */
    void finish() throws Exception;
}
