package com.example.hikerview.ui.download.util;

import java.util.UUID;

/**
 * Created by xm on 15/4/23.
 */
public class UUIDUtil {

    public static String genUUID(){
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
