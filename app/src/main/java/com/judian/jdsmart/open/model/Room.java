package com.judian.jdsmart.open.model;

import com.judian.jdsmart.common.entity.JdSmartRoomBase;
import com.judian.jdsmart.common.entity.JdSmartScene;
import com.judian.jdsmart.common.entity.v2.JdSmartRoomV2;

import java.util.ArrayList;
import java.util.List;

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
//    public JdSmartRoom toJdSmartScene() {
//        JdSmartScene jdSmartScene = new JdSmartScene();
//        jdSmartScene.setSceneName(getName());
//        jdSmartScene.setSceneNo(getSceneId() + "");
//        return jdSmartScene;
//    }
//
//    public List<JdSmartScene> toJdSmartScene(List<Scene> sceneList) {
//        List<JdSmartScene> jdSmartSceneList = new ArrayList<>();
//        for (Scene scene : sceneList) {
//            jdSmartSceneList.add(scene.toJdSmartScene());
//        }
//        return jdSmartSceneList;
//    }

}
