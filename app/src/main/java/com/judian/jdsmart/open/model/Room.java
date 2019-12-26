package com.judian.jdsmart.open.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)//去掉警告
public class Room {

    private int roomId;
    private String name;
    private String title;
    private String cover;
    private int imageId;
    private long id;


}
