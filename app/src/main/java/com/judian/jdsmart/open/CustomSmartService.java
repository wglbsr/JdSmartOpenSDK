package com.judian.jdsmart.open;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.judian.jdsmart.common.JdSmartConstant;
import com.judian.jdsmart.common.Vendor;
import com.judian.jdsmart.common.entity.JdSmartAccount;
import com.judian.jdsmart.common.entity.JdSmartCtrlCmd;
import com.judian.jdsmart.common.entity.JdSmartDevices;
import com.judian.jdsmart.common.entity.JdSmartScene;
import com.judian.jdsmart.common.virtualhost.JdSmartHostActionConstant;
import com.judian.support.jdbase.JdbaseAidlServer;
import com.judian.support.jdbase.JdbaseCallback;
import com.judian.support.jdbase.JdbaseContant;
import com.judian.support.jdbase.aidl.IAidlCallback;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Timer;


/**
 * Created by Luke on 16-11-30.
 */

public class CustomSmartService extends JdbaseAidlServer {
    private static final String TAG = "CustomSmartService";
    private CustomSmartHost mCustomSmartHost;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");

        mCustomSmartHost = new CustomSmartHost();
        mCustomSmartHost.registerDeviceChange(new JdbaseCallback() {
            @Override
            public void onResult(int code, String s, String s1) {
                notifyCallBackDataChange(code, s, s1);
            }
        });

        bindSmartVendor();

        Timer timer = new Timer();
        timer.schedule(new UpgradeTask(), 1000, 1000 * 60 * 60 * 6); //6hour
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public String doAidlAction(int action, String format, final String data, final IAidlCallback callback) {
        String a = "aa";

        Log.d(TAG, "doAidlAction=" + action + ", format=" + format + ", data=" + data);

        if (mCustomSmartHost == null) {
            try {
                if (callback != null) {
                    callback.onResult(JdbaseContant.RESULT_FAIL, "Virtual host is null", "");
                }
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
            return "Virtual host is null";
        }


        switch (action) {
            case JdSmartHostActionConstant.ACTION_INIT_HOST:
                mCustomSmartHost.init(this);
                break;

            case JdSmartHostActionConstant.ACTION_GET_HOST_INFORMATION:
                return JSON.toJSONString(mCustomSmartHost.getHostInfo());

            case JdSmartHostActionConstant.ACTION_GET_LOGIN_STATE:
                return mCustomSmartHost.getLoginState();

            case JdSmartHostActionConstant.ACTION_GET_ACCOUNT:
                JdSmartAccount account = mCustomSmartHost.getAccount();
                if (account != null) {
                    account.setVendor(Vendor.VENDOR_CUSTOM);
                    return JSON.toJSONString(mCustomSmartHost.getAccount());
                } else {
                    JdSmartAccount ret = new JdSmartAccount();
                    ret.setVendor(Vendor.VENDOR_CUSTOM);
                    ret.setName("");
                    return JSON.toJSONString(ret);
                }

            case JdSmartHostActionConstant.ACTION_LOGIN:
                mCustomSmartHost.login(format, data, new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_SELECT_FAMILY:
                mCustomSmartHost.selectFamily(data, new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;



            case JdSmartHostActionConstant.ACTION_LOGOUT:
                mCustomSmartHost.logout(new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_SEARCH_AND_BIND_DEVICE:
                mCustomSmartHost.searchAndBindHost(format.contains("true"), new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_UNBIND_FROM_HOST:
                mCustomSmartHost.unbindHost(new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_CREATE_SCENE:
                mCustomSmartHost.createScene(JSON.parseObject(format, JdSmartScene.class), new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_DELETE_SCENE:
                mCustomSmartHost.deleteScene(JSON.parseObject(format, JdSmartScene.class), new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_UPDATE_SCENE:
                mCustomSmartHost.updateScene(JSON.parseObject(format, JdSmartScene.class), new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_GET_SCENES:
                mCustomSmartHost.getScenes(new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_CREATE_SCENE_BIND:
                mCustomSmartHost.createSceneBind(JSON.parseArray(format, JdSmartCtrlCmd.class), new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_DELETE_SCENE_BIND:
                mCustomSmartHost.deleteSceneBind(JSON.parseArray(format, JdSmartCtrlCmd.class), new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_UPDATE_SCENE_BIND:
                mCustomSmartHost.updateSceneBind(JSON.parseArray(format, JdSmartCtrlCmd.class), new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_GET_SCENE_BIND:
                mCustomSmartHost.getSceneBind(JSON.parseObject(format, JdSmartScene.class), new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_GET_ALL_DEVICE:
                mCustomSmartHost.getAllDevices(new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            Log.d(TAG, "ACTION_GET_ALL_DEVICE string=" + s);
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_GET_ALL_DEVICE_BY_TYPE:
                JdSmartDevices devs = mCustomSmartHost.getDevicesByType(Integer.parseInt(format));
                return devs != null ? JSON.toJSONString(devs) : null;

            case JdSmartHostActionConstant.ACTION_GET_ALL_DEVICE_TYPE:
                mCustomSmartHost.getAllDeviceType(new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_GET_DEVICE_DETAIL:
                mCustomSmartHost.getDeviceDetail(format, new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_CONTROL_DEVICE:
                mCustomSmartHost.controlDevice(JSON.parseObject(format, JdSmartCtrlCmd.class), new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_CONTROL_SCENE:
                mCustomSmartHost.controlScene(JSON.parseObject(format, JdSmartScene.class), new JdbaseCallback() {
                    @Override
                    public void onResult(int resultCode, String s, String s1) {
                        try {
                            callback.onResult(resultCode, s, s1);
                        } catch (RemoteException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
                break;

            case JdSmartHostActionConstant.ACTION_GET_SENSOR_RECORD:
                try {
                    JSONObject jobj = JSON.parseObject(format);
                    String deviceid = jobj.getString("deviceid");
                    int pageIndex = Integer.parseInt(jobj.getString("pageindex"));
                    int pageSize = Integer.parseInt(jobj.getString("pagesize"));
                    mCustomSmartHost.getSensorRecord(deviceid, pageIndex, pageSize, new JdbaseCallback() {
                        @Override
                        public void onResult(int resultCode, String s, String s1) {
                            try {
                                callback.onResult(resultCode, s, s1);
                            } catch (RemoteException e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "sensor record execption=" + e.toString());
                }
                break;

            case JdSmartHostActionConstant.ACTION_SET_VOICE_TEXT:
                mCustomSmartHost.setVoiceText(data, callback);
                break;

            case JdSmartHostActionConstant.ACTION_OTHER_API_SUB_CMD_REFRESH_DEVICE:
                mCustomSmartHost.refreshDevice();
                break;

            case JdSmartHostActionConstant.ACTION_RESPONSE_DATA_FROM_SYS:
                mCustomSmartHost.responseDataFromSys(format, data);
                break;

            default:
                Log.e(TAG, "Unsupport action!!!!");
                break;
        }

        return null;
    }


    public static boolean isConnectingToInternet(Context context) {
        return getConnectNetworkInfo(context) != null;
    }

    public static NetworkInfo getConnectNetworkInfo(Context context) {
        ConnectivityManager connectivity =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return info[i];
                    }
                }
        }
        return null;
    }

    private int getSignHash(Context context, String packageName) {
        PackageInfo packageInfo = null;
        int ret = 0;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            Signature[] signs = packageInfo.signatures;
            Signature sign = signs[0];
            ret = sign.hashCode();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public String getPid() {
        String result = null;
        try {
            String className = "android.os.SystemProperties";
            Class<?> testClass = Class.forName(className);
            Method saddMethod2 = testClass.getMethod("get", new Class[]{String.class,String.class});
            result = saddMethod2.invoke(null,new Object[]{"ro.judian.pid", "0"}).toString();
            Log.d(TAG, "getPid: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String accountId = null;

    public String getAccountId(Context context) {
        if (accountId == null) {
            AccountManager accountManager = AccountManager.get(context);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            Account[] accounts = accountManager.getAccountsByType("com.judian.jdmediarender.account");
            for (int i = 0; i < accounts.length; i++) {
                //Log.v(TAG, "getAccountId type:" + accounts[i].type + " name:" + accounts[i].name);
                accountId = accounts[i].name;
                return accountId;
            }
            return null;
        }
        else {
            return accountId;
        }
    }

    private void bindSmartVendor() {
        final String appId = mCustomSmartHost.getAppId();
        final String packageName = getPackageName();
        final Context context = this.getApplicationContext();

        if (TextUtils.isEmpty(appId) || appId.length() > 32) {
            Log.e(TAG, "JdSmartOpen appid error.");
            Toast.makeText(this.getApplicationContext(), "Error: JdSmartOpen appid error.", Toast.LENGTH_LONG).show();
            return;
        }

        final String deviceId = getAccountId(context);
        if (TextUtils.isEmpty(deviceId)) {
            Log.e(TAG, "JdSmartOpen getAccountId error.");
            return;
        }

        final String pid = getPid();
        if (TextUtils.isEmpty(pid)) {
            Log.e(TAG, "JdSmartOpen getPid error.");
            return;
        }

        final String signHash = String.valueOf(getSignHash(context, packageName));

        new Thread(new Runnable(){
            public void run(){
                int tryCount = 0;
                while (true) {
                    if (isConnectingToInternet(context)) {

                        boolean ret = requestBindSmartAppid(pid, deviceId, appId, signHash);
                        tryCount++;
                        if (ret == true || tryCount == 3) {
                            break;
                        }
                    }

                    try {
                        Thread.sleep(5 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.d(TAG, "bindSmartVendor finish");
            }}).start();
    }

    private boolean requestBindSmartAppid(String pid, String id, String appId, String signHash) {
        Log.d(TAG, "requestBindSmartAppid appId: " + appId);

        String path = "http://device.aispeaker.com/bindSmartOpenAppid?appid=" + appId + "&sign=" + signHash + "&id=" + id + "&pid=" + pid;
        try {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                //请求成功

                InputStream is = conn.getInputStream();   //获取输入流，此时才真正建立链接
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader bufferReader = new BufferedReader(isr);
                String result = "", line;
                while((line = bufferReader.readLine()) != null){
                    result += line;
                }

                Log.d(TAG, "requestBindSmartAppid result: " + result);
                JSONObject jobj = JSON.parseObject(result);
                int code = jobj.getIntValue("code");
                if (code > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    class UpgradeTask extends java.util.TimerTask{
        @Override
        public void run() {
            int localVersion = getAppVersion(getApplicationContext());
            Log.d(TAG, "UpgradeTask is runing, current version=" + localVersion);

            Beta.checkUpgrade(false, true);
            UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();

            if (upgradeInfo == null) {
                Log.d(TAG, "upgradeInfo is null");
                return;
            }
            StringBuilder info = new StringBuilder();
            info.append("id: ").append(upgradeInfo.id).append("\n");
            info.append("标题: ").append(upgradeInfo.title).append("\n");
            info.append("升级说明: ").append(upgradeInfo.newFeature).append("\n");
            info.append("versionCode: ").append(upgradeInfo.versionCode).append("\n");
            info.append("versionName: ").append(upgradeInfo.versionName).append("\n");
            info.append("发布时间: ").append(upgradeInfo.publishTime).append("\n");
            info.append("安装包Md5: ").append(upgradeInfo.apkMd5).append("\n");
            info.append("安装包下载地址: ").append(upgradeInfo.apkUrl).append("\n");
            info.append("安装包大小: ").append(upgradeInfo.fileSize).append("\n");
            info.append("弹窗间隔（ms）: ").append(upgradeInfo.popInterval).append("\n");
            info.append("弹窗次数: ").append(upgradeInfo.popTimes).append("\n");
            info.append("发布类型（0:测试 1:正式）: ").append(upgradeInfo.publishType).append("\n");
            info.append("弹窗类型（1:建议 2:强制 3:手工）: ").append(upgradeInfo.upgradeType).append("\n");
            info.append("图片地址：").append(upgradeInfo.imageUrl);
            Log.d(TAG, "get upgradeInfo=" + info.toString());

            if (localVersion < upgradeInfo.versionCode) {
                Log.d(TAG, "start upgrade");
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                stopService(new Intent("com.judian.service.CustomSmartService"));
            }
        }
    }

    public static int getAppVersion(Context context) {
        int version = 0;
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

}
