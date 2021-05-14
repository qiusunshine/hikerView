package com.example.hikerview.service.http;

import android.text.TextUtils;

import com.lzy.okgo.convert.Converter;

import java.io.IOException;

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
        try (ResponseBody body = response.body()) {
            if (TextUtils.isEmpty(charset)) {
                if (body != null) {
                    try {
                        return body.string();
                    } catch (IOException e) {
                        e.printStackTrace();
                        //部分情况下会抛异常
                        return "";
                    }
                }
                return "";
            }
            byte[] b = new byte[0];
            if (body != null) {
                try {
                    b = body.bytes();
                    //自动识别charset
//                    MediaType mediaType = body.contentType();
//                    if (mediaType != null && mediaType.charset() != null) {
//                        return new String(b, mediaType.charset());
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    //部分情况下获取byte会抛异常
                    return "";
                }
            }
            return new String(b, charset);
        }
    }
}
