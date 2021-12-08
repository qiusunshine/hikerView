package com.example.hikerview.service.http;

import com.lzy.okgo.callback.AbsCallback;

import okhttp3.Response;

/**
 * 作者：By 15968
 * 日期：On 2019/10/4
 * 时间：At 10:31
 */
public abstract class BytesCallback extends AbsCallback<byte[]> {

    @Override
    public byte[] convertResponse(Response response) throws Throwable {
        return new BytesConvert().convertResponse(response);
    }
}
