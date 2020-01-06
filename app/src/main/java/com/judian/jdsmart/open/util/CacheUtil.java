package com.judian.jdsmart.open.util;

import com.alibaba.fastjson.JSONObject;
import com.judian.jdsmart.open.model.UserInfo;

/**
 * @Author: wglbs
 * @Date: 31/12/2019 10:10
 * @Description:
 * @Version 1.0.0
 */
public class CacheUtil {
    private static final String KEY_LOGIN_USER = "";
    private static final String KEY_LOGIN_TOKEN = "";

//    public static String getLoginToken() {
//
//    }
//
//    public static UserInfo getLoginUser() {
//
//    }

    public static void saveUser(UserInfo userInfo) {
        save(KEY_LOGIN_USER,userInfo);
    }

    public static void save(String key, Object o) {
        DataKeeper.saveCache(key, JSONObject.toJSONString(o, true));
    }

    public static <T> T save(String key, Class<T> T) {
        String result = DataKeeper.getCache(key);
        return JSONObject.parseObject(result, T);
    }
}
