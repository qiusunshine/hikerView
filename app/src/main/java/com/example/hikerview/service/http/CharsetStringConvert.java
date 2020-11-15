package com.example.hikerview.service.http;

import android.text.TextUtils;

import com.lzy.okgo.convert.Converter;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 作者：By 15968
 * 日期：On 2019/10/4
 * 时间：At 10:30
 */
public class CharsetStringConvert implements Converter<String> {

    private String charset;

    public CharsetStringConvert(String charset) {
        this.charset = charset;
    }

    @Override
    public String convertResponse(Response response) throws Throwable {
        try (ResponseBody body = response.body()){
            if (TextUtils.isEmpty(charset)) {
                if (body != null) {
                    return body.string();
                }
                return "";
            }
            byte[] b = new byte[0];
            if (body != null) {
                b = body.bytes();
            }
            return new String(b, charset);
        }
    }
}
