/*Copyright ©2015 TommyLemon(https://github.com/TommyLemon)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package com.judian.jdsmart.open.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import com.alibaba.fastjson.JSONObject;
import com.judian.jdsmart.open.Constant;
import com.judian.jdsmart.open.model.UserInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HttpRequestYniot {


    public static String token = "";
    public static String hostAddress = "";
    public static String username = "";
    public static String password = "";
    public static UserInfo userInfo = null;
    public static int userId = -1;

    private static Map<String, Object> getParams(String uuid, String token, Map<String, Object> data) {
        data.put(Constant.KEY_TOKEN, token);
        if (StringUtil.isEmpty(uuid)) {
            return data;
        }
        Map<String, Object> params = new HashMap<>();
        params.put(Constant.KEY_UUID, uuid);
        params.put(Constant.KEY_DATA, data);
        return params;
    }


    static void post(Context context, String uuid, String token, Map<String, Object> data, String url, int requestCode, HttpManager.OnHttpResponseListener listener) {
        HttpManager.getInstance(context).post(getParams(uuid, token, data), Constant.URL_BASE + url, true, requestCode, false, filter(listener));
    }

    static void post(Context context, String uuid, String token, Map<String, Object> data, String url, int requestCode, boolean useCache, HttpManager.OnHttpResponseListener listener) {
        HttpManager.getInstance(context).post(getParams(uuid, token, data), Constant.URL_BASE + url, true, requestCode, useCache, filter(listener));
    }

    static void post(Context context, Map<String, Object> data, String url, int requestCode, HttpManager.OnHttpResponseListener listener) {
        HttpManager.getInstance(context).post(getParams(Constant.VAL_UUID_DEFAULT, getTOKEN(), data), Constant.URL_BASE + url, true, requestCode, false, filter(listener));
    }

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String post(String url, String json) throws IOException {
        final MediaType JSON
                = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }


    }

    private static HttpManager.OnHttpResponseListener filter(final HttpManager.OnHttpResponseListener listener) {
        return new HttpManager.OnHttpResponseListener() {
            @Override
            public void onHttpResponse(int requestCode, String resultJson, Exception e) {
                JSONObject result = JSONObject.parseObject(resultJson);
                if (result != null && result.containsKey(Constant.KEY_ERROR_CODE)) {
                    //100022	Token值不合法
                    //100023	Token已过期,请重新登录
                    String errorCode = result.getString(Constant.KEY_ERROR_CODE);
                    if ("100022".equals(errorCode) || "100023".equals(errorCode) || "100032".equals(errorCode)) {
                        //跳转到登陆界面或者直接提示登陆
                        return;
                    }
                }
                listener.onHttpResponse(requestCode, resultJson, e);

            }
        };
    }

    private static String getTOKEN() {
        return token;
    }


    /**
     * @return void
     * @Author wanggl(lane)
     * @Description //TODO
     * @Date 14:55 2019-09-10
     * @Param [username, password, requestCode(同步用), listener]
     **/
    public static void login(Context context, final String username, final String password,
                             final int requestCode, final HttpManager.OnHttpResponseListener listener) {
        Map<String, Object> params = new HashMap<>();
        params.put(Constant.KEY_USERNAME, username);
        params.put(Constant.KEY_PASSWORD, password);
        post(context, params, "/user/login", requestCode, listener);
    }


    /**
     * @return void
     * @Author wanggl(lane)
     * @Description //TODO 获取用户信息
     * @Date 16:08 2019-09-11
     * @Param [userId, requestCode, listener]
     **/
    public static void getUserInfo(Context context, int userId, final int requestCode, final HttpManager.OnHttpResponseListener listener) {
        Map<String, Object> params = new HashMap<>();
        params.put(Constant.KEY_USER_ID, userId);
        post(context, Constant.VAL_UUID_DEFAULT, getTOKEN(), params, "/user/info", requestCode, listener);
    }

    /**
     * @return void
     * @Author wanggl(lane)
     * @Description //TODO 获取主机列表,可选择是否使用缓存
     * @Date 10:57 2019-09-25
     * @Param [userId, paged, useCache, listener]
     **/
    public static void getHostList(Context context, int userId, final int paged, boolean useCache, final HttpManager.OnHttpResponseListener listener) {
        int pageNum = paged + 1;
        Map<String, Object> data = new HashMap<>();
        data.put(Constant.KEY_USER_ID, userId);
        data.put(Constant.KEY_PAGED, pageNum);
        post(context, Constant.VAL_UUID_DEFAULT, getTOKEN(), data, "/device/host_list", paged, useCache, listener);
    }

    /**
     * @return void
     * @Author wanggl(lane)
     * @Description //TODO 获取单个主机
     * @Date 9:16 2019-11-05
     * @Param [userId, hostAddress, paged, useCache, listener]
     **/
    public static void getHost(Context context, int userId, String hostAddress, final int paged, boolean useCache, final HttpManager.OnHttpResponseListener listener) {
        int pageNum = paged + 1;
        Map<String, Object> data = new HashMap<>();
        data.put(Constant.KEY_USER_ID, userId);
        data.put(Constant.KEY_PAGED, pageNum);
        data.put(Constant.KEY_HOST_ADDRESS, hostAddress);
        post(context, Constant.VAL_UUID_DEFAULT, getTOKEN(), data, "/device/host_list", paged, useCache, listener);
    }


    /*******************************安防相关**********************************************************/


    public static void getDeviceList(Context context, String hostAddress, String category, String situation, final int requestCode, final HttpManager.OnHttpResponseListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put(Constant.KEY_HOST_ADDRESS, hostAddress);
        data.put(Constant.KEY_DEVICE_CATEGORY, category);
        if (!StringUtil.isEmpty(situation)) {
            data.put(Constant.KEY_DEVICE_SITUATION, situation);
        }
        post(context, Constant.VAL_UUID_DEFAULT, getTOKEN(), data, "/device/device_list", requestCode, listener);
    }


    /**
     * @return void
     * @Author wanggl(lane)
     * @Description //TODO
     * @Date 15:25 5/12/2019
     * @Param [hostAddress, requestCode, useCache, listener]
     **/
    public static void getSceneList(Context context, String hostAddress,
                                    final int requestCode, boolean useCache, final HttpManager.OnHttpResponseListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put(Constant.KEY_HOST_ADDRESS, hostAddress);
        post(context, Constant.VAL_UUID_DEFAULT, getTOKEN(), data, "/scene/scene_list", requestCode, useCache, listener);
    }


    public static void getRoomList(Context context, String hostAddress, String roomType,
                                   final int requestCode, final HttpManager.OnHttpResponseListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put(Constant.KEY_ROOM_TYPE, hostAddress);
        data.put(Constant.KEY_HOST_ADDRESS, roomType);
        post(context, Constant.VAL_UUID_DEFAULT, getTOKEN(), data, "/room/room_list", requestCode, listener);
    }

}