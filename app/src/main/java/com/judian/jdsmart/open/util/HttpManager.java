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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpManager {
    private static final String TAG = "HttpManager";


    private Context context;

    public interface OnHttpResponseListener {
        void onHttpResponse(int requestCode, String resultJson, Exception e);
    }

    private HttpManager(Context context) {
        this.context = context;
    }

    private static HttpManager instance;// 单例

    public static HttpManager getInstance(Context context) {
        if (instance == null) {
            synchronized (HttpManager.class) {
                if (instance == null) {
                    instance = new HttpManager(context);
                }
            }
        }
        return instance;
    }


    public static final MediaType TYPE_JSON = MediaType.parse("application/json; charset=utf-8");


    public void post(final Map<String, Object> request, final String url, final boolean isJson
            , final int requestCode, final boolean useCache, final OnHttpResponseListener listener) {
        new AsyncTask<Void, Void, Exception>() {

            String result;
            //使用hashCode作为KEY来保存到缓存
            int cacheKey = request.hashCode() + url.hashCode() + requestCode + (isJson ? 1 : 0);

            @Override
            protected Exception doInBackground(Void... params) {
                if (useCache) {
//                    result = CacheManager.getInstance().get(String.class, cacheKey);
                }
                if (StringUtil.isEmpty(result)) {
                    try {
                        OkHttpClient client = getHttpClient(url);
                        if (client == null) {
                            return new Exception(TAG + ".post  AsyncTask.doInBackground  client == null >> return;");
                        }

                        RequestBody requestBody;
                        if (isJson) {
                            String body = JSONObject.toJSONString(request);
                            Log.d(TAG, "\n\n<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n post  url = " + url + "\n request = \n" + body);
                            requestBody = RequestBody.create(TYPE_JSON, body);
                        } else {
                            FormBody.Builder builder = new FormBody.Builder();
                            Set<Map.Entry<String, Object>> set = request == null ? null : request.entrySet();
                            if (set != null) {
                                for (Map.Entry<String, Object> entry : set) {
                                    builder.add(StringUtil.trim(entry.getKey()), StringUtil.trim(entry.getValue()));
                                }
                            }

                            requestBody = builder.build();
                        }
                        result = getResponseJson(
                                client,
                                new Request.Builder()
                                        .url(url)
                                        .post(requestBody)
                                        .build()
                        );
                        Log.d(TAG, "\n post  result = \n" + result + "\n >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n\n");
//                        CacheManager.getInstance().save(String.class, result, cacheKey, KEY_GROUP_HTTP_CACHE);
                    } catch (Exception e) {
                        Log.e(TAG, "post  AsyncTask.doInBackground  try {  result = getResponseJson(..." +
                                "} catch (Exception e) {\n" + e.getMessage());
                        return e;
                    }

                }
                return null;
            }


            @Override
            protected void onPostExecute(Exception exception) {
                super.onPostExecute(exception);
                listener.onHttpResponse(requestCode, result, exception);
            }

        }.execute();
    }


    //httpGet/httpPost 内调用方法 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    /**
     * @param url
     * @return
     */
    public OkHttpClient getHttpClient(String url) {
        Log.i(TAG, "getHttpClient  url = " + url);
        if (StringUtil.isEmpty(url)) {
            Log.e(TAG, "getHttpClient  StringUtil.isEmpty(url) >> return null;");
            return null;
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .cookieJar(new CookieJar() {

                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        Map<String, String> map = new LinkedHashMap<>();
                        if (cookies != null) {
                            for (Cookie c : cookies) {
                                if (c != null && c.name() != null && c.value() != null) {
                                    map.put(c.name(), StringUtil.get(c.value()));
                                }
                            }
                        }
                        saveCookie(url == null ? null : url.host(), JSONObject.toJSONString(map));//default constructor not found  cookies));
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        String host = url == null ? null : url.host();
                        Map<String, String> map = host == null ? null : JSONObject.parseObject(getCookie(host), HashMap.class);

                        List<Cookie> list = new ArrayList<>();

                        Set<Map.Entry<String, String>> set = map == null ? null : map.entrySet();
                        if (set != null) {
                            for (Map.Entry<String, String> entry : set) {
                                if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                                    list.add(new Cookie.Builder().domain(host).name(entry.getKey()).value(entry.getValue()).build());
                                }
                            }
                        }

                        return list;
                    }
                });
        return builder.build();
    }


    public static final String KEY_COOKIE = "cookie";

    /**
     * @param host
     * @return
     */
    public String getCookie(String host) {
        if (host == null) {
            Log.e(TAG, "getCookie  host == null >> return \"\"");
            return "";
        }
        return context.getSharedPreferences(KEY_COOKIE, Context.MODE_PRIVATE).getString(host, "");
    }

    /**
     * @param host
     * @param value
     */
    public void saveCookie(String host, String value) {
        if (host == null) {
            Log.e(TAG, "saveCookie  host == null >> return;");
            return;
        }
        context.getSharedPreferences(KEY_COOKIE, Context.MODE_PRIVATE)
                .edit()
                .remove(host)
                .putString(host, value)
                .commit();
    }


    /**
     * @param client
     * @param request
     * @return
     * @throws Exception
     */
    public String getResponseJson(OkHttpClient client, Request request) throws Exception {
        if (client == null || request == null) {
            Log.e(TAG, "getResponseJson  client == null || request == null >> return null;");
            return null;
        }
        Response response = client.newCall(request).execute();
        return response.isSuccessful() ? response.body().string() : null;
    }


    //httpGet/httpPost 内调用方法 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>


}