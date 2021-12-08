package com.example.hikerview.service.http;

import com.lzy.okgo.convert.Converter;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 作者：By 15968
 * 日期：On 2019/10/4
 * 时间：At 10:30
 */
public class BytesConvert implements Converter<byte[]> {

    @Override
    public byte[] convertResponse(Response response) throws Throwable {
        try (ResponseBody body = response.body()) {
            return body == null ? new byte[]{} : body.bytes();
        }
    }
}
