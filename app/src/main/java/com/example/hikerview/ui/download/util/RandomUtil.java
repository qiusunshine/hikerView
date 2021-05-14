package com.example.hikerview.ui.download.util;

import java.util.Random;

/**
 * Created by xm on 17-8-21.
 */
public class RandomUtil {
    public static int getRandom(int min, int max){
        Random random = new Random();
        return random.nextInt(max) % (max - min + 1) + min;
    }
}
