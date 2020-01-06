package com.judian.jdsmart.open.model;

import com.judian.jdsmart.common.entity.JdSmartFamily;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Auther: wglbs
 * @Date: 2019-09-20 10:25
 * @Description:
 * @Version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)//去掉警告
public class Host {
    private long id;
    private String clientId;
    private String name;
    private String manufacture;
    private String ruleText;
    private String address;
    private Integer pushRing = 1;


    public JdSmartFamily toFamily() {
        JdSmartFamily jdSmartFamily = new JdSmartFamily();
        jdSmartFamily.setName(this.getName());
        jdSmartFamily.setId(this.getId() + "");
        return jdSmartFamily;
    }


    public static List<JdSmartFamily> toFamilyList(List<Host> hosts) {
        List<JdSmartFamily> jdSmartFamilyList = new ArrayList<>();
        for (Host host : hosts) {
            jdSmartFamilyList.add(host.toFamily());
        }
        return jdSmartFamilyList;
    }
}
