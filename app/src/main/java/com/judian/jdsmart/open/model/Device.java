package com.judian.jdsmart.open.model;

import com.judian.jdsmart.common.entity.JdSmartDevice;

import java.util.ArrayList;
import java.util.List;

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

    public JdSmartDevice toJdSmartDevice() {
        JdSmartDevice jdSmartDevice = new JdSmartDevice();
        jdSmartDevice.setDeviceId(this.getDeviceId() + "");
        jdSmartDevice.setDeviceName(this.getName());
        jdSmartDevice.setOnline(1);
        jdSmartDevice.setDeviceType(this.getDeviceType());
        return jdSmartDevice;
    }


    public List<JdSmartDevice> toJdSmartDeviceList(List<Device> deviceList) {
        List<JdSmartDevice> jdSmartDeviceList = new ArrayList<>();
        for (Device device : deviceList) {
            jdSmartDeviceList.add(device.toJdSmartDevice());
        }
        return jdSmartDeviceList;
    }

}
