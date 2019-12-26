package com.judian.jdsmart.open;

import java.util.Date;

import lombok.Data;

/**
 * @Auther: wglbs
 * @Date: 2019-09-11 09:59
 * @Description:
 * @Version 1.0.0
 */
@Data
public class UserInfo implements Cloneable {
    private Integer userId;//Number用户ID;
    private String userName;//String用户名;
    private String nickName;// String用户昵称;
    private String email;// Stringe-mail地址;
    private String phone;//String手机号码;
    private String avator;// String用户头像;
    private Integer sex;//Number性别ID标识;
    private String sexText;//String性别名称;
    private Integer status;//Number账号状态;
    private String statusText;//String账号状态描述;
    private String currHostAddress;//String用户当前主机;
    private Date timeCreated;//Datetime注册时间;
    private Integer isBindDueros;
    private String duerosOpenid;
    private String duerosBindHostAddress;
    private String ruleText = null;
    private String guid;
    private Boolean isCurrentUser = false;


}
