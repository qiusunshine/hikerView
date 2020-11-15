package com.example.viewlibrary.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AudioVisualConverter {

    private static final String TAG = AudioVisualConverter.class.getSimpleName();

    private static final int MAX_SIZE = 180;

    public AudioVisualConverter() {
    }

    /**
     * 处理音频数据
     *
     * @param data
     * @return
     */
    public byte[] converter(byte[] data) {
        if (data == null || data.length < MAX_SIZE) {
            return null;
        }

        List<Byte> readyData = readyData(data);
        Map<Integer, Byte> altData = pickAltData(readyData);
        Map<Integer, Byte> adornData = adornData(altData);
        return genWaveDate(adornData);
    }


    /**
     * 预处理数据
     *
     * @return
     */
    private List<Byte> readyData(byte[] data) {
        List<Byte> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            list.add((byte) (128 - Math.abs(data[i])));
        }
        return list;
    }

    /**
     * 预处理数据
     *
     * @return
     */
    public byte[] readyDataByte(byte[] data) {
        byte[] newData = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            newData[i] = (byte) (128 - Math.abs(data[i]));
        }
        return newData;
    }


    /**
     * 处理音频数据
     *
     * @param data
     * @return
     */
    public byte[] converterFft(byte[] data) {
        byte[] newData = new byte[data.length];
        byte[] readyData = readyFftDataByte(data);
        //去高音
        for (int i = 0; i < readyData.length / 2; i++) {
            newData[i * 2] = readyData[i];
            if (i != readyData.length / 2 - 1) {
                newData[i * 2 + 1] = (byte) (newData[i * 2] * Math.random());
            }
        }
        return newData;
    }

    /**
     * 预处理数据
     *
     * @return
     */
    public byte[] readyFftDataByte(byte[] data) {
        byte[] newData = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            newData[i] = (byte) (Math.abs(data[i]));
        }
        return newData;
    }

    /**
     * 预处理数据
     *
     * @return
     */
    public int[] getVoiceSizes(byte[] data) {
        byte[] newData = readyFftDataByte(data);
        int[] voices = new int[newData.length / 8];
        for (int i = 0; i < voices.length; i++) {
            int maxValue = 0;
            for (int j = i * 8; j < i * 8 + 8; j++) {
                if (newData[j] > maxValue) {
                    maxValue = newData[j];
                }
            }
            voices[i] = maxValue;
        }
        return voices;
    }


    /**
     * 预处理数据
     *
     * @return
     */
    public int getVoiceSize(byte[] data) {
        byte[] newData = readyFftDataByte(data);
        int[] voices = getVoiceSizes(newData);

        int sum = 0;
        for (int i = 0; i < 3; i++) {
            if (i == 0) {
                sum += voices[i];
            } else if (i == 1) {
                sum += voices[i];
            } else if (i == 2) {
                sum += voices[i];
            }
        }

        return sum / 3;
    }

    /**
     * 提取有效数据
     *
     * @return
     */
    private Map<Integer, Byte> pickAltData(List<Byte> list) {
        Map<Integer, Byte> maxValueMap = new HashMap<>();
        byte max = 0;
        int maxIndex = 0;
        byte last = 0;
        int laseIndex = 0;

        //前32位
        for (int i = 0; i < 32; i++) {
            Byte value = list.get(i);
            if (value > max) {
                max = value;
                maxIndex = i;
            }
        }
        maxValueMap.put(maxIndex, max);
        max = 0;
        maxIndex = 0;


        //后96
        for (int i = 32; i < list.size(); i++) {
            Byte value = list.get(i);
            if (value > max) {
                max = value;
                maxIndex = i;
            } else {
                if (value < last) {
                    if (maxIndex - laseIndex > 26) {  //TODO:降低数据采样
                        maxValueMap.put(maxIndex, (byte) (max));
                        laseIndex = maxIndex;
                    }
                    max = 0;
                }
            }
            last = value;
        }

        return maxValueMap;
    }


    /**
     * 数据加工
     *
     * @return
     */
    private Map<Integer, Byte> adornData(Map<Integer, Byte> maxValueMap) {
        Map<Integer, Byte> newValueMap = new HashMap<>();

        byte max = 0;
        int maxPosition = 0;
        for (Integer key : maxValueMap.keySet()) {
            Byte value = maxValueMap.get(key);
            if (value > max) {
                max = value;
                maxPosition = key;
            }
        }
        byte value = (byte) (max * 2);
        //maxValueMap.put(maxPosition, value <= 0 ? (byte) (MAX_SIZE - 1) : value);


        Iterator<Map.Entry<Integer, Byte>> iterator = maxValueMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Byte> entry = iterator.next();
            Integer maxIndex = entry.getKey();
            byte maxValue = entry.getValue();

            byte maxValue11 = (byte) (maxValue * Math.random());
            byte maxValue12 = (byte) (maxValue * Math.random());
            byte maxValue21 = (byte) (maxValue11 * Math.random());
            byte maxValue22 = (byte) (maxValue12 * Math.random());
            if (maxIndex - 2 >= 0) {
                newValueMap.put(maxIndex - 2, maxValue21);
            }
            if (maxIndex - 1 >= 0) {
                newValueMap.put(maxIndex - 1, maxValue11);
            }
            if (maxIndex + 1 < MAX_SIZE) {
                newValueMap.put(maxIndex + 1, maxValue12);
            }
            if (maxIndex + 2 < MAX_SIZE) {
                newValueMap.put(maxIndex + 2, maxValue22);
            }
        }

        maxValueMap.putAll(newValueMap);

        return maxValueMap;
    }


    private byte[] genWaveDate(Map<Integer, Byte> maxValueMap) {
        byte[] data = new byte[MAX_SIZE];

        for (int i = 0; i < MAX_SIZE; i++) {
            Byte value = maxValueMap.get(i);
            data[i] = value == null ? 0 : value;
        }

        return data;
    }
}