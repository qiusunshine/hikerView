package com.example.hikerview.service.parser;

/**
 * 作者：By 15968
 * 日期：On 2021/1/3
 * 时间：At 11:45
 */

public interface BaseParseCallback<T> {
    void start();
    /**
     * 当数据请求成功后，调用此接口显示数据
     * @param data 数据源
     */
    void success(T data);
    /**
     * 显示请求错误提示
     */
    void error(String msg);
}
