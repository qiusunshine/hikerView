package com.example.hikerview.service.parser;

/**
 * 作者：By hdy
 * 日期：On 2018/12/10
 * 时间：At 12:18
 */
public interface SearchJsCallBack<T> {
    /**
     * 当数据请求成功后，调用此接口显示数据
     * @param data 数据源
     */
    void showData(T data);
    /**
     * 显示请求错误提示
     */
    void showErr(String msg);
}
