package com.judian.jdsmart.open.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Auther: wglbs
 * @Date: 2019-10-30 09:44
 * @Description:
 * @Version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)//去掉警告
public class Device {
    private String conditionType;
    private long id;
    private int deviceId;
    private String name;
    private String address;
    private String deviceType;
    private String deviceDesc;
    private String modelId;
    private String deviceStatus;
    private int enabled = 0;
    private String manufacturer;

}
