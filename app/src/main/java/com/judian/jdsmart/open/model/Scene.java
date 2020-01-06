package com.judian.jdsmart.open.model;

import com.judian.jdsmart.common.entity.JdSmartScene;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: wglbs
 * @Date: 27/11/2019 14:39
 * @Description:
 * @Version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Scene {
    private int iconResId;
    private int sceneId;
    private String hostAddress;
    private String icon;
    private String name;
    private String time_created;
    private int sort;


    public JdSmartScene toJdSmartScene() {
        JdSmartScene jdSmartScene = new JdSmartScene();
        jdSmartScene.setSceneName(getName());
        jdSmartScene.setSceneNo(getSceneId() + "");
        return jdSmartScene;
    }

    public List<JdSmartScene> toJdSmartScene(List<Scene> sceneList) {
        List<JdSmartScene> jdSmartSceneList = new ArrayList<>();
        for (Scene scene : sceneList) {
            jdSmartSceneList.add(scene.toJdSmartScene());
        }
        return jdSmartSceneList;
    }
}
