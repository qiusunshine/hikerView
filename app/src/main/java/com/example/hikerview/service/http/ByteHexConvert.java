package com.example.hikerview.service.http;

import com.example.hikerview.utils.StringUtil;
import com.lzy.okgo.convert.Converter;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 作者：By 15968
 * 日期：On 2019/10/4
 * 时间：At 10:30
 */
public class ByteHexConvert implements Converter<String> {

    public ByteHexConvert() {
    }

    @Override
    public String convertResponse(Response response) throws Throwable {
        try (ResponseBody body = response.body()) {
            if (body != null) {
                return StringUtil.bytesToHexString(body.bytes());
            }
            return "";
        }
    }
}
