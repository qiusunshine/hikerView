package com.example.hikerview.service.http;

import com.lzy.okgo.callback.AbsCallback;

import okhttp3.Response;

/**
 * 作者：By 15968
 * 日期：On 2019/10/4
 * 时间：At 10:31
 */
public abstract class CharsetStringCallback extends AbsCallback<String> {

    private CharsetStringConvert convert;

    public CharsetStringCallback(String charset) {
        convert = new CharsetStringConvert(charset);
    }

    @Override
    public String convertResponse(Response response) throws Throwable {
        String s = null;
        try {
            s = convert.convertResponse(response);
        } finally {
            response.close();
        }
        return s;
    }
}
