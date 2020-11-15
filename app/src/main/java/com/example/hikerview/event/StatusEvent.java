package com.example.hikerview.event;

/**
 * 作者：By 15968
 * 日期：On 2020/2/5
 * 时间：At 13:15
 */
public class StatusEvent {
    private int enginePos;

    public StatusEvent(int enginePos, String group) {
        this.enginePos = enginePos;
        this.group = group;
    }

    private String group;

    public StatusEvent(int enginePos) {
        this.enginePos = enginePos;
    }

    public StatusEvent() {
    }

    public int getEnginePos() {
        return enginePos;
    }

    public void setEnginePos(int enginePos) {
        this.enginePos = enginePos;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
