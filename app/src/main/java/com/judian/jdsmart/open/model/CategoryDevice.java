package com.judian.jdsmart.open.model;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Auther: wglbs
 * @Date: 2019-10-30 14:41
 * @Description:
 * @Version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CategoryDevice {
    private String categoryName;
    private String name;
    private List<Device> deviceList;

}
