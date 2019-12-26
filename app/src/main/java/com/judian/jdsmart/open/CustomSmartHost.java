package com.judian.jdsmart.open;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.judian.jdsmart.common.JdSmartConstant;
import com.judian.jdsmart.common.Vendor;
import com.judian.jdsmart.common.entity.JdSmartAccount;
import com.judian.jdsmart.common.entity.JdSmartCtrlCmd;
import com.judian.jdsmart.common.entity.JdSmartDevice;
import com.judian.jdsmart.common.entity.JdSmartDeviceOrder;
import com.judian.jdsmart.common.entity.JdSmartDeviceType;
import com.judian.jdsmart.common.entity.JdSmartDevices;
import com.judian.jdsmart.common.entity.JdSmartFamily;
import com.judian.jdsmart.common.entity.JdSmartHostInfo;
import com.judian.jdsmart.common.entity.JdSmartIRConstant;
import com.judian.jdsmart.common.entity.JdSmartLoginConstant;
import com.judian.jdsmart.common.entity.JdSmartRet;
import com.judian.jdsmart.common.entity.JdSmartScene;
import com.judian.jdsmart.common.entity.JdSmartSceneBind;
import com.judian.jdsmart.common.entity.JdSmartScenes;
import com.judian.jdsmart.common.entity.JdSmartSensorRecord;
import com.judian.jdsmart.common.entity.JdSmartSensorRecordGroup;
import com.judian.jdsmart.common.virtualhost.IJdSmartHost;
import com.judian.jdsmart.common.virtualhost.JdSmartHostActionConstant;
import com.judian.support.jdbase.JdbaseCallback;
import com.judian.support.jdbase.JdbaseContant;
import com.judian.support.jdbase.aidl.IAidlCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Luke on 16-11-30.
 */

public class CustomSmartHost implements IJdSmartHost {

    private static final String TAG = "CustomSmartHost";
    private String mUserName = "";
    private Context mContext;
    private JdbaseCallback mUpdateDeviceCallback = null;
    private JdSmartHostInfo mJdSmartHostInfo;

    private JdSmartDevices mJdsmartDevices = new JdSmartDevices();
    private HashMap<String, JdSmartDevice> mMapDevices = new HashMap<String, JdSmartDevice>();

    JdSmartScenes mJdSmartScenes = new JdSmartScenes();
    private HashMap<String, JdSmartScene> mMapScenes = new HashMap<String, JdSmartScene>();

    private List<JdSmartSceneBind> mListBind = new ArrayList<JdSmartSceneBind>();
    private HashMap<String, JdSmartSceneBind> mMapBind = new HashMap<String, JdSmartSceneBind>();

    //存储智能空调状态
    private JSONObject mAirConditionJson = new JSONObject();
    //存储红外空调状态
    private JSONObject irAirGroupData = new JSONObject();
    //存储登录状态
    private JdSmartRet mLoginResult = new JdSmartRet(0, "");


    //SOS record
    List<JdSmartSensorRecordGroup> mSOSGroup = new ArrayList<JdSmartSensorRecordGroup>();
    List<JdSmartSensorRecordGroup> mGasGroup = new ArrayList<JdSmartSensorRecordGroup>();
    List<JdSmartSensorRecordGroup> mInfraredGroup = new ArrayList<JdSmartSensorRecordGroup>();
    List<JdSmartSensorRecordGroup> mWindowGroup = new ArrayList<JdSmartSensorRecordGroup>();
    List<JdSmartFamily> mFamilyList = new ArrayList<>();

    private UpdateDeviceHandler mUpdateDeviceHandler;

    @Override
    public void init(Context context) {
        mContext = context;

        mJdSmartHostInfo = new JdSmartHostInfo();
        mJdSmartHostInfo.setLoginPrompt("有你物联");
        //是否支持场景编辑功能，这里指场景绑定设备
        mJdSmartHostInfo.setEnableSceneEdit(false);
        //是否支持传感器类设备显示记录
        mJdSmartHostInfo.setShowSensorDetail(true);
        //是否支持选择家庭
        mJdSmartHostInfo.setEnableSelectFamily(true);
        //version不设置，默认是0；设为1表示支持在进入房间管理页前显示登录状态页（如果已登录成功，会跳过登录状态页，直接进入房间管理页）
        mJdSmartHostInfo.setVersion(1);
        //设置登录类型
        mJdSmartHostInfo.setLoginType(JdSmartLoginConstant.LOGIN_TYPE_ACCOUNT);
        mJdSmartHostInfo.setSupportLogin(true);
        /*
        若实时更新设备设为真，则当设备信息发生变化（设备名称更改、添加或删除设备）时，
        调用notifyDevicesChange()方法通知上层。
        即可在桌面加载最新设备信息，不用再去房间管理界面点击"重新导入"按钮
        */
        mJdSmartHostInfo.setEnableDisplayInRealTime(false);

        /*
              不支持登录，version和loginType都不用设置
              mJdSmartHostInfo.setSupportLogin(false);
        * */

        Log.d(TAG, "debug init 0");
        initDemoDevices();
        Log.d(TAG, "debug init 1");
        initScenes();
        Log.d(TAG, "debug init 2");
        initSOSSensorRecord();
        Log.d(TAG, "debug init 3");

        HandlerThread threadUpdateDevice = new HandlerThread("Update device");
        threadUpdateDevice.start();
        mUpdateDeviceHandler = new UpdateDeviceHandler(threadUpdateDevice.getLooper());
        Log.d(TAG, "debug init 4");

        //1.开机后，就会运行init, 用户可以在init中连接或搜索主机。
        //2.如果上次用户已登录主机，可在此处用保存的密码进行登陆
        //PreferenceManager.getDefaultSharedPreferences(mContext).getString("UserName", "");
        //3.建议，登陆成功后，立刻调用获取所有设备。
    }

    /**
     * 该函数用于第三方JdSmartOpen App后续升级管理。
     * 为简化管理，我司不分配具体id，为保证全球唯一，建议设置为贵司的域名。
     * 例如：美的集团设置该值为 midea.com
     *
     * @return 返回JdSmartOpen appid
     */
    public String getAppId() {
        return "yn-iot.cn";
    }

    private class UpdateDeviceHandler extends Handler {
        private static final int MSG_UPDATE_DEVICE_INFO = 1;

        UpdateDeviceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_DEVICE_INFO:
                    Bundle bundle = msg.getData();
                    String deviceId = bundle.getString("deviceID");
                    updateDeviceInfo(mMapDevices.get(deviceId));
                    break;
                default:
                    Log.e(TAG, "unknown msg=" + msg.what);
                    break;
            }
        }
    }

    //设置当前登录状态
    private void setLoginState(int state) {
        setLoginState(state, "");
    }

    private void setLoginState(int state, String msg) {
        Log.d(TAG, "setLoginState: " + state + " msg:" + msg);
        if (state != mLoginResult.getRet()) {
            synchronized (this) {
                mLoginResult = new JdSmartRet(state, msg);
            }
            //上报最新登录状态
            mUpdateDeviceCallback.onResult(JdSmartConstant.ACTION_LOGIN_STATE_CHANGE, JSON.toJSONString(mLoginResult), "");
        }
    }

    /**
     * 如下这三个设备被场景绑定了（见initSceneBind函数）。如果要删除设备，注意一起都删除
     * JdSmartDevice light1 = findDeviceById("light_1");
     * JdSmartDevice light2 = findDeviceById("light_2");
     * JdSmartDevice curtain = findDeviceById("curtain_1");
     */
    private void initDemoDevices() {


//        HttpRequestYniot.getDeviceList(mContext, HttpRequestYniot.hostAddress,
//                "light,socket,security,environment,windowCurtains,transponder,other,electric,video",
//                null, 0, new HttpManager.OnHttpResponseListener() {
//                    @Override
//                    public void onHttpResponse(int requestCode, String resultJson, Exception e) {
//
//                    }
//                });


        mJdsmartDevices.setVendor(Vendor.VENDOR_CUSTOM);
        int i;

        ArrayList<String> roomList = new ArrayList<>();
        ArrayList<String> floorList = new ArrayList<>();

        roomList.add("客厅");
        roomList.add("主卧");
        roomList.add("阳台");
        floorList.add("1");
        floorList.add("2"); //楼层设置负数不能识别，最终会被归到1楼，楼层名只能包含中文和数字


        ArrayList<String> lightNameList = new ArrayList<>();
        for (i = 0; i < roomList.size(); i++) { //每个房间放一个大灯
            lightNameList.add("大灯");
        }
        lightNameList.add("餐厅灯"); //房间名+类型名 但放在客厅
        lightNameList.add("大灯");
        //这些测试灯放在主卧
        lightNameList.add("吸顶灯");
        lightNameList.add("吸顶灯"); //测试同名设备
        lightNameList.add("吊灯");
        lightNameList.add("主卧吊灯");
        lightNameList.add("吧台灯");
        lightNameList.add("台灯一");
        lightNameList.add("台灯二");
        lightNameList.add("一号顶灯");
        lightNameList.add("二号顶灯");
        lightNameList.add("大白菜"); //不含灯字的设备

        for (i = 0; i < lightNameList.size(); i++) {
            JdSmartDevice light = new JdSmartDevice();
            light.setVendor(Vendor.VENDOR_CUSTOM);
            light.setDeviceId("light_" + i);
            light.setDeviceName(lightNameList.get(i));
            light.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_LAMP + "");
            light.setOnline(1);
            if (i <= roomList.size()) {
                light.setRoomId(roomList.get(i % roomList.size()));
                light.setZoneId(floorList.get(0));
            } else {
                if (i == roomList.size() + 1) {  //二楼主卧放一个灯
                    light.setRoomId("主卧");
                    light.setZoneId(floorList.get(1));
                } else {
                    light.setRoomId(roomList.get(1));
                    light.setZoneId(floorList.get(0));
                }
            }
            JdSmartCtrlCmd lightCmd = new JdSmartCtrlCmd();
            lightCmd.setVendor(Vendor.VENDOR_CUSTOM);
            lightCmd.setDeviceId(light.getDeviceId());
            lightCmd.setOrder(JdSmartDeviceOrder.OFF);
            lightCmd.setValue1("-1");
            light.setJdSmartCtrlCmd(lightCmd);
            mJdsmartDevices.addDevice(light);
            mMapDevices.put(light.getDeviceId(), light);
        }


        ArrayList<String> curtainNameList = new ArrayList<>();
        curtainNameList.add("窗帘");
        curtainNameList.add("纱帘");
        //虚拟3个窗帘, 窗帘的名字任意，选择设备的时候会显示
        for (i = 0; i < curtainNameList.size(); i++) {
            JdSmartDevice curtain = new JdSmartDevice();
            curtain.setVendor(Vendor.VENDOR_CUSTOM);
            curtain.setDeviceId("curtain_" + i);
            curtain.setDeviceName(curtainNameList.get(i));
            curtain.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_CURTAINS + "");
            curtain.setOnline(1);
            curtain.setRoomId(roomList.get(i));
            curtain.setZoneId(floorList.get(i % floorList.size()));
            JdSmartCtrlCmd curtainCmd = new JdSmartCtrlCmd();
            curtainCmd.setVendor(Vendor.VENDOR_CUSTOM);
            curtainCmd.setDeviceId(curtain.getDeviceId());
            curtainCmd.setOrder(JdSmartDeviceOrder.CLOSE);
            curtainCmd.setValue1("0");
            curtain.setJdSmartCtrlCmd(curtainCmd);
            mJdsmartDevices.addDevice(curtain);
            mMapDevices.put(curtain.getDeviceId(), curtain);
        }

        ArrayList<String> curtainNOPositionNameList = new ArrayList<>();
        curtainNOPositionNameList.add("无进度窗帘");
        //虚拟一个无详细进度的窗帘
        JdSmartDevice curtain = new JdSmartDevice();
        curtain.setVendor(Vendor.VENDOR_CUSTOM);
        curtain.setDeviceId("curtain_no_position_ID_1");
        curtain.setDeviceName(curtainNOPositionNameList.get(0));
        curtain.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_CURTAINS_NO_POSITION + "");
        curtain.setOnline(1);
        curtain.setRoomId(roomList.get(0));
        curtain.setZoneId(floorList.get(0));
        JdSmartCtrlCmd curtainCmd = new JdSmartCtrlCmd();
        curtainCmd.setVendor(Vendor.VENDOR_CUSTOM);
        curtainCmd.setDeviceId(curtain.getDeviceId());
        curtainCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        curtainCmd.setValue1("0");
        curtain.setJdSmartCtrlCmd(curtainCmd);
        mJdsmartDevices.addDevice(curtain);
        mMapDevices.put(curtain.getDeviceId(), curtain);


        ArrayList<String> dimmerNameList = new ArrayList<>();
        dimmerNameList.add("调光灯");
        dimmerNameList.add("无亮度调光灯");
        JdSmartDevice dimmer = new JdSmartDevice();
        dimmer.setVendor(Vendor.VENDOR_CUSTOM);
        dimmer.setDeviceId("dimmer_1");
        dimmer.setDeviceName(dimmerNameList.get(0));
        dimmer.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_DIMMER + "");
        dimmer.setOnline(1);
        dimmer.setRoomId(roomList.get(0));
        dimmer.setZoneId(floorList.get(0));
        JdSmartCtrlCmd dimmerCmd = new JdSmartCtrlCmd();
        dimmerCmd.setVendor(Vendor.VENDOR_CUSTOM);
        dimmerCmd.setDeviceId(dimmer.getDeviceId());
        dimmerCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        dimmerCmd.setValue1("0");
        dimmer.setJdSmartCtrlCmd(dimmerCmd);
        mJdsmartDevices.addDevice(dimmer);
        mMapDevices.put(dimmer.getDeviceId(), dimmer);

        JdSmartDevice dimmer2 = new JdSmartDevice();
        dimmer2.setVendor(Vendor.VENDOR_CUSTOM);
        dimmer2.setDeviceId("dimmer_2");
        dimmer2.setDeviceName(dimmerNameList.get(1));
        dimmer2.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_DIMMER_2 + "");
        dimmer2.setOnline(1);
        dimmer2.setRoomId(roomList.get(0));
        dimmer2.setZoneId(floorList.get(0));
        JdSmartCtrlCmd dimmer2Cmd = new JdSmartCtrlCmd();
        dimmer2Cmd.setVendor(Vendor.VENDOR_CUSTOM);
        dimmer2Cmd.setDeviceId(dimmer2.getDeviceId());
        dimmer2Cmd.setOrder(JdSmartDeviceOrder.CLOSE);
        dimmer2Cmd.setValue1("0");
        dimmer2.setJdSmartCtrlCmd(dimmer2Cmd);
        mJdsmartDevices.addDevice(dimmer2);
        mMapDevices.put(dimmer2.getDeviceId(), dimmer2);


        ArrayList<String> airConditionNameList = new ArrayList<>();
        airConditionNameList.add("红外空调");
        airConditionNameList.add("智能空调");
        //虚拟3个空调，其中一个是红外类型空调，空调的名字任意，选择设备的时候会显示
        for (i = 0; i < 2; i++) {
            JdSmartDevice airCondition = new JdSmartDevice();
            airCondition.setVendor(Vendor.VENDOR_CUSTOM);
            airCondition.setDeviceId("airCondition_" + i);
            airCondition.setDeviceName(airConditionNameList.get(i));
            airCondition.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_AIRCONDITION + "");
            airCondition.setOnline(1);
            airCondition.setRoomId(roomList.get(0));
            airCondition.setZoneId(floorList.get(0));
            JdSmartCtrlCmd airConditionCmd = new JdSmartCtrlCmd();
            airConditionCmd.setVendor(Vendor.VENDOR_CUSTOM);
            airConditionCmd.setDeviceId(airCondition.getDeviceId());
            airCondition.setJdSmartCtrlCmd(airConditionCmd);

            if (0 == i) {
                //如果设备是红外设备，需要增加这个子类型,示例中，仅增加一个红外空调
                airCondition.setDeviceSubType(JdSmartDeviceType.DEVICE_SUB_TYPE_IR + "");
                //设置红外学习了哪些红外键值，本次学习了开，关，制冷，制热，左右扫风，自动扫风，自动风速，
                //制冷16度，17度,18度, 制热16度，17度,18度
                StringBuilder IRKeyList = new StringBuilder(50);
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_ON + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_OFF + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_MODE_COOL + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_MODE_HEAT + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_MODE_DEHUMIDIFY + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_MODE_WIND + ",");

                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_WIND_RATE_LOW + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_WIND_RATE_MIDDLE + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_WIND_RATE_HIGH + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_WIND_DIRECTION_LEFT_RIGHT + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_WIND_DIRECTION_UP_DOWN + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIRCONDITION_WIND_DIRECTION_NO_DIRECTION + ",");

                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_TEMPERATURE_COOL_16 + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_TEMPERATURE_COOL_17 + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_TEMPERATURE_COOL_18 + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_TEMPERATURE_HOT_16 + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_TEMPERATURE_HOT_17 + ",");
                IRKeyList.append(JdSmartIRConstant.IR_KEY_AIR_CONDITION_TEMPERATURE_HOT_18 + ",");
                airCondition.setJdIRkeyList(IRKeyList.toString());
            } else {
                JSONObject jobj = new JSONObject();
                jobj.put(JdSmartDeviceOrder.AIRCONDITION_MODE_TYPE, JdSmartDeviceOrder.AIRCONDITION_MODE_COOL);
                jobj.put(JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_TYPE, JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_LOW);
                jobj.put(JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_TYPE, JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_LEFT_RIGHT);
                jobj.put(JdSmartDeviceOrder.TEMPERATURE, String.valueOf(25 + i)); //空调温度
                airConditionCmd.setGroupData(jobj.toJSONString());
            }
            mJdsmartDevices.addDevice(airCondition);
            mMapDevices.put(airCondition.getDeviceId(), airCondition);
        }

        //color lamp
        JdSmartDevice colorCWLed = new JdSmartDevice();
        colorCWLed.setVendor(Vendor.VENDOR_CUSTOM);
        colorCWLed.setDeviceId("DEVICE_TYPE_COLOR_CW_LAMP_1");
        colorCWLed.setDeviceName("色温灯");
        colorCWLed.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_LAMP + "");
        colorCWLed.setDeviceSubType(JdSmartDeviceType.DEVICE_SUB_LAMP_TYPE_COLOR_CW + "");
        colorCWLed.setOnline(1);
        colorCWLed.setRoomId(roomList.get(2));
        colorCWLed.setZoneId(floorList.get(0));
        JdSmartCtrlCmd colorCWLedCmd = new JdSmartCtrlCmd();
        colorCWLedCmd.setVendor(Vendor.VENDOR_CUSTOM);
        colorCWLedCmd.setDeviceId(colorCWLed.getDeviceId());
        colorCWLedCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        colorCWLedCmd.setValue1("-1");
        JSONObject jsonObjectCW = new JSONObject();
        jsonObjectCW.put(JdSmartDeviceOrder.COLOR_LAMP_MODE, JdSmartDeviceOrder.COLOR_LAMP_MODE_CW);
        jsonObjectCW.put(JdSmartDeviceOrder.COLOR_LAMP_CW_VALUE, "220");
        jsonObjectCW.put(JdSmartDeviceOrder.COLOR_LAMP_CW_BRIGHT, "230");
        colorCWLedCmd.setGroupData(jsonObjectCW.toJSONString());
        colorCWLed.setJdSmartCtrlCmd(colorCWLedCmd);
        mJdsmartDevices.addDevice(colorCWLed);
        mMapDevices.put(colorCWLed.getDeviceId(), colorCWLed);

        JdSmartDevice colorRGBLed = new JdSmartDevice();
        colorRGBLed.setVendor(Vendor.VENDOR_CUSTOM);
        colorRGBLed.setDeviceId("DEVICE_TYPE_COLOR_RGB_LAMP_1");
        colorRGBLed.setDeviceName("色灯");
        colorRGBLed.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_LAMP + "");
        colorRGBLed.setDeviceSubType(JdSmartDeviceType.DEVICE_SUB_LAMP_TYPE_COLOR_RGB + "");
        colorRGBLed.setOnline(1);
        colorRGBLed.setRoomId(roomList.get(2));
        colorRGBLed.setZoneId(floorList.get(0));
        JdSmartCtrlCmd colorRGBLedCmd = new JdSmartCtrlCmd();
        colorRGBLedCmd.setVendor(Vendor.VENDOR_CUSTOM);
        colorRGBLedCmd.setDeviceId(colorRGBLed.getDeviceId());
        colorRGBLedCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        colorRGBLedCmd.setValue1("-1");
        JSONObject jsonObjectRGB = new JSONObject();
        jsonObjectRGB.put(JdSmartDeviceOrder.COLOR_LAMP_MODE, JdSmartDeviceOrder.COLOR_LAMP_MODE_RGB);
        jsonObjectRGB.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_VALUE, "#AABBCC");
        jsonObjectRGB.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_BRIGHT, "230");
        jsonObjectRGB.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_SATURATION, "0");
        colorRGBLedCmd.setGroupData(jsonObjectRGB.toJSONString());
        colorRGBLed.setJdSmartCtrlCmd(colorRGBLedCmd);
        mJdsmartDevices.addDevice(colorRGBLed);
        mMapDevices.put(colorRGBLed.getDeviceId(), colorRGBLed);

        JdSmartDevice colorRGBCWLed = new JdSmartDevice();
        colorRGBCWLed.setVendor(Vendor.VENDOR_CUSTOM);
        colorRGBCWLed.setDeviceId("DEVICE_TYPE_COLOR_RGBCW_LAMP_1");
        colorRGBCWLed.setDeviceName("大灯带");
        colorRGBCWLed.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_LAMP + "");
        colorRGBCWLed.setDeviceSubType(JdSmartDeviceType.DEVICE_SUB_LAMP_TYPE_COLOR_RGBCW + "");
        colorRGBCWLed.setOnline(1);
        colorRGBCWLed.setRoomId(roomList.get(2));
        colorRGBCWLed.setZoneId(floorList.get(0));
        JdSmartCtrlCmd colorLedCmd = new JdSmartCtrlCmd();
        colorLedCmd.setVendor(Vendor.VENDOR_CUSTOM);
        colorLedCmd.setDeviceId(colorRGBCWLed.getDeviceId());
        colorLedCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        colorLedCmd.setValue1("-1");
        JSONObject jsonObjectRGBCW = new JSONObject();
        jsonObjectRGBCW.put(JdSmartDeviceOrder.COLOR_LAMP_MODE, JdSmartDeviceOrder.COLOR_LAMP_MODE_RGB);
        jsonObjectRGBCW.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_VALUE, "#AABBCC");
        jsonObjectRGBCW.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_BRIGHT, "230");
        jsonObjectRGBCW.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_SATURATION, "0");
        jsonObjectRGBCW.put(JdSmartDeviceOrder.COLOR_LAMP_CW_VALUE, "220");
        jsonObjectRGBCW.put(JdSmartDeviceOrder.COLOR_LAMP_CW_BRIGHT, "230");
        colorLedCmd.setGroupData(jsonObjectRGBCW.toJSONString());
        colorRGBCWLed.setJdSmartCtrlCmd(colorLedCmd);
        mJdsmartDevices.addDevice(colorRGBCWLed);
        mMapDevices.put(colorRGBCWLed.getDeviceId(), colorRGBCWLed);


        ArrayList<String> windowControlerNameList = new ArrayList<>();
        windowControlerNameList.add("开窗器");

        //虚拟开窗器, 名字任意，选择设备的时候会显示
        JdSmartDevice windowControlerDev = new JdSmartDevice();
        windowControlerDev.setVendor(Vendor.VENDOR_CUSTOM);
        windowControlerDev.setDeviceId("windowControler_id");
        windowControlerDev.setDeviceName(windowControlerNameList.get(0));
        windowControlerDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_WINDOW_CONTROLER + "");
        windowControlerDev.setOnline(1);
        windowControlerDev.setRoomId(roomList.get(0));
        windowControlerDev.setZoneId(floorList.get(0));
        JdSmartCtrlCmd windowControlerCmd = new JdSmartCtrlCmd();
        windowControlerCmd.setVendor(Vendor.VENDOR_CUSTOM);
        windowControlerCmd.setDeviceId(windowControlerDev.getDeviceId());
        windowControlerCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        windowControlerCmd.setValue1("0");
        windowControlerDev.setJdSmartCtrlCmd(windowControlerCmd);
        mJdsmartDevices.addDevice(windowControlerDev);
        mMapDevices.put(windowControlerDev.getDeviceId(), windowControlerDev);

        ArrayList<String> heaterNameList = new ArrayList<>();
        heaterNameList.add("温控器");
        //虚拟温控器, 名字任意，选择设备的时候会显示
        JdSmartDevice heaterDev = new JdSmartDevice();
        heaterDev.setVendor(Vendor.VENDOR_CUSTOM);
        heaterDev.setDeviceId("heater_id");
        heaterDev.setDeviceName(heaterNameList.get(0));
        heaterDev.setOnline(1);
        heaterDev.setRoomId(roomList.get(0));
        heaterDev.setZoneId(floorList.get(0));
        heaterDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_HEATER + "");
        JdSmartCtrlCmd heaterCmd = new JdSmartCtrlCmd();
        heaterCmd.setVendor(Vendor.VENDOR_CUSTOM);
        heaterCmd.setDeviceId(heaterDev.getDeviceId());
        heaterCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        heaterCmd.setValue1("0");
        heaterDev.setJdSmartCtrlCmd(heaterCmd);
        mJdsmartDevices.addDevice(heaterDev);
        mMapDevices.put(heaterDev.getDeviceId(), heaterDev);


        ArrayList<String> airFloorHeatingNameList = new ArrayList<>();
        airFloorHeatingNameList.add("地暖");
        airFloorHeatingNameList.add("地暖");
        for (i = 0; i < 2; i++) {
            JdSmartDevice floorHeatingDev = new JdSmartDevice();
            floorHeatingDev.setVendor(Vendor.VENDOR_CUSTOM);
            floorHeatingDev.setDeviceId("floor_heating_" + i);
            floorHeatingDev.setDeviceName(airFloorHeatingNameList.get(i));
            floorHeatingDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_FLOOR_HEATING + "");
            floorHeatingDev.setOnline(1);
            floorHeatingDev.setRoomId(roomList.get(i % roomList.size()));
            floorHeatingDev.setZoneId(floorList.get(i % floorList.size()));
            JdSmartCtrlCmd floorheatingCmd = new JdSmartCtrlCmd();
            floorheatingCmd.setVendor(Vendor.VENDOR_CUSTOM);
            floorheatingCmd.setDeviceId(floorHeatingDev.getDeviceId());
            floorheatingCmd.setOrder(JdSmartDeviceOrder.OFF);
            floorheatingCmd.setValue1("0");

            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JdSmartDeviceOrder.FLOOR_HEATING_STATUS_TEMP, "25");
            jsonObject.put(JdSmartDeviceOrder.FLOOR_HEATING_STATUS_MODE, JdSmartDeviceOrder.FLOOR_HEATING_ORDER_MODE_AUTO);
            floorheatingCmd.setGroupData(jsonObject.toJSONString());

            floorHeatingDev.setJdSmartCtrlCmd(floorheatingCmd);
            mJdsmartDevices.addDevice(floorHeatingDev);
            mMapDevices.put(floorHeatingDev.getDeviceId(), floorHeatingDev);
        }

        ArrayList<String> airClothHangerNameList = new ArrayList<>();
        airClothHangerNameList.add("阳台晾衣架");
        airClothHangerNameList.add("晾衣架");
        for (i = 0; i < 2; i++) {
            JdSmartDevice clothesHangerDev = new JdSmartDevice();
            clothesHangerDev.setVendor(Vendor.VENDOR_CUSTOM);
            clothesHangerDev.setDeviceId("cloth_hanger_" + i);
            clothesHangerDev.setDeviceName(airClothHangerNameList.get(i));
            clothesHangerDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_CLOTHES_HANGER + "");
            clothesHangerDev.setOnline(1);
            clothesHangerDev.setRoomId(roomList.get(i % roomList.size()));
            clothesHangerDev.setZoneId(floorList.get(i % floorList.size()));
            JdSmartCtrlCmd clotherHangerCmd = new JdSmartCtrlCmd();
            clotherHangerCmd.setVendor(Vendor.VENDOR_CUSTOM);
            clotherHangerCmd.setDeviceId(clothesHangerDev.getDeviceId());
            clotherHangerCmd.setOrder(JdSmartDeviceOrder.OFF);
            JSONObject jobj = new JSONObject();
            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_MOVE, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_MOVE_STOP);
            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_DESINFECTION, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_DESINFECTION_OFF);
            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_HEAT, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_HEAT_OFF);
            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_LIGHT, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_LIGHT_OFF);
            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_WIND, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_WIND_OFF);
            clotherHangerCmd.setValue1(-1 + "");
            clotherHangerCmd.setGroupData(jobj.toJSONString());
            clothesHangerDev.setJdSmartCtrlCmd(clotherHangerCmd);
            mJdsmartDevices.addDevice(clothesHangerDev);
            mMapDevices.put(clothesHangerDev.getDeviceId(), clothesHangerDev);
        }


        ArrayList<String> airPurifierNameList = new ArrayList<>();
        airPurifierNameList.add("华为空气净化器");
        airPurifierNameList.add("小米空气净化器");
        for (i = 0; i < 1; i++) {
            JdSmartDevice airPurifierDev = new JdSmartDevice();
            airPurifierDev.setVendor(Vendor.VENDOR_CUSTOM);
            airPurifierDev.setDeviceId("air_purifier_" + i);
            airPurifierDev.setDeviceName(airPurifierNameList.get(i));
            airPurifierDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_AIRPURIFIER + "");
            airPurifierDev.setOnline(1);
            airPurifierDev.setRoomId(roomList.get(i % roomList.size()));
            airPurifierDev.setZoneId(floorList.get(i % floorList.size()));
            JdSmartCtrlCmd airPurifierCmd = new JdSmartCtrlCmd();
            airPurifierCmd.setVendor(Vendor.VENDOR_CUSTOM);
            airPurifierCmd.setDeviceId(airPurifierDev.getDeviceId());
            airPurifierCmd.setOrder(JdSmartDeviceOrder.OFF);
            airPurifierCmd.setValue1("0");

            JSONObject jobj = new JSONObject();
            jobj.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_MODE, JdSmartDeviceOrder.AIRPURIFIER_ORDER_MODE_MANUAL);
            jobj.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_WIND, JdSmartDeviceOrder.AIRPURIFIER_ORDER_WIND_MID);
            jobj.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_PM25, "50");
            jobj.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_TEMP, "23");
            jobj.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_HUMIDITY, "60");
            jobj.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_AQI, "一级（优）");
            airPurifierCmd.setGroupData(jobj.toJSONString());
            airPurifierDev.setJdSmartCtrlCmd(airPurifierCmd);
            mJdsmartDevices.addDevice(airPurifierDev);
            mMapDevices.put(airPurifierDev.getDeviceId(), airPurifierDev);
        }

        JdSmartDevice sweeperDev = new JdSmartDevice();
        sweeperDev.setVendor(Vendor.VENDOR_CUSTOM);
        sweeperDev.setDeviceId("Sweeper_machine_" + 0);
        sweeperDev.setDeviceName("扫地机器人");
        sweeperDev.setRoomId(roomList.get(0));
        sweeperDev.setZoneId(floorList.get(0));
        sweeperDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_SWEEPER_MACHINE + "");
        sweeperDev.setOnline(1);
        JdSmartCtrlCmd sweeperCmd = new JdSmartCtrlCmd();
        sweeperCmd.setVendor(Vendor.VENDOR_CUSTOM);
        sweeperCmd.setDeviceId(sweeperDev.getDeviceId());
        sweeperCmd.setOrder(JdSmartDeviceOrder.OFF);
        sweeperCmd.setValue1("-1");
        JSONObject sweeperJson = new JSONObject();
        sweeperJson.put(JdSmartDeviceOrder.SWEEPER_MACHINE_STATUS, JdSmartDeviceOrder.SWEEPER_MACHINE_CHARGE_MODE);
        sweeperCmd.setGroupData(sweeperJson.toJSONString());
        sweeperDev.setJdSmartCtrlCmd(sweeperCmd);
        mJdsmartDevices.addDevice(sweeperDev);
        mMapDevices.put(sweeperDev.getDeviceId(), sweeperDev);


        ArrayList<String> airFreshNameList = new ArrayList<>();
        airFreshNameList.add("华为新风");
        airFreshNameList.add("小米新风");
        for (i = 0; i < 1; i++) {
            JdSmartDevice airFreshDev = new JdSmartDevice();
            airFreshDev.setVendor(Vendor.VENDOR_CUSTOM);
            airFreshDev.setDeviceId("air_fresh_" + i);
            //airFreshDev.setDeviceName("air_fresh " + i);
            airFreshDev.setDeviceName(airFreshNameList.get(i));
            airFreshDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_FRESH_AIR + "");
            airFreshDev.setOnline(1);
            airFreshDev.setRoomId(roomList.get(i % roomList.size()));
            airFreshDev.setZoneId(floorList.get(i % floorList.size()));
            JdSmartCtrlCmd airFreshCmd = new JdSmartCtrlCmd();
            airFreshCmd.setVendor(Vendor.VENDOR_CUSTOM);
            airFreshCmd.setDeviceId(airFreshDev.getDeviceId());
            airFreshCmd.setOrder(JdSmartDeviceOrder.OFF);
            airFreshCmd.setValue1("-1");
            JSONObject jobj = new JSONObject();
            jobj.put(JdSmartDeviceOrder.FRESH_AIR_STATUS_WIND, JdSmartDeviceOrder.FRESH_AIR_ORDER_WIND_HIGH);
            airFreshCmd.setGroupData(jobj.toJSONString());
            airFreshDev.setJdSmartCtrlCmd(airFreshCmd);
            mJdsmartDevices.addDevice(airFreshDev);
            mMapDevices.put(airFreshDev.getDeviceId(), airFreshDev);
        }


        ArrayList<String> switchNameList = new ArrayList<>();
        switchNameList.add("大灯");  //开关类型绑定的是灯设备
        switchNameList.add("电视机");
        switchNameList.add("热水器");
        for (i = 0; i < 3; i++) {
            JdSmartDevice switchDev = new JdSmartDevice();
            switchDev.setVendor(Vendor.VENDOR_CUSTOM);
            switchDev.setDeviceId("switch_" + i);
            switchDev.setDeviceName(switchNameList.get(i));
            switchDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_SWITCH + "");
            switchDev.setOnline(1);
            switchDev.setRoomId("阳台");
            switchDev.setZoneId(floorList.get(0));
            JdSmartCtrlCmd switchCmd = new JdSmartCtrlCmd();
            switchCmd.setVendor(Vendor.VENDOR_CUSTOM);
            switchCmd.setDeviceId(switchDev.getDeviceId());
            switchCmd.setOrder(JdSmartDeviceOrder.OFF);
            switchCmd.setValue1("-1");
            switchDev.setJdSmartCtrlCmd(switchCmd);
            mJdsmartDevices.addDevice(switchDev);
            mMapDevices.put(switchDev.getDeviceId(), switchDev);
        }

        JdSmartDevice switchDev = new JdSmartDevice();
        switchDev.setVendor(Vendor.VENDOR_CUSTOM);
        switchDev.setDeviceId("Fan_" + 0);
        switchDev.setRoomId(roomList.get(0));
        switchDev.setZoneId(floorList.get(0));
        switchDev.setDeviceName("风扇");
        switchDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_FAN + "");
        switchDev.setOnline(1);
        JdSmartCtrlCmd switchCmd = new JdSmartCtrlCmd();
        switchCmd.setVendor(Vendor.VENDOR_CUSTOM);
        switchCmd.setDeviceId(switchDev.getDeviceId());
        switchCmd.setOrder(JdSmartDeviceOrder.OFF);
        switchCmd.setValue1("-1");
        switchDev.setJdSmartCtrlCmd(switchCmd);
        mJdsmartDevices.addDevice(switchDev);
        mMapDevices.put(switchDev.getDeviceId(), switchDev);


        JdSmartDevice doorDev = new JdSmartDevice();
        doorDev.setVendor(Vendor.VENDOR_CUSTOM);
        doorDev.setDeviceId("door_" + 0);
        doorDev.setDeviceName("门");
        doorDev.setRoomId(roomList.get(0));
        doorDev.setZoneId(floorList.get(0));
        doorDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_DOOR + "");
        doorDev.setOnline(1);
        JdSmartCtrlCmd doorCmd = new JdSmartCtrlCmd();
        doorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        doorCmd.setDeviceId(doorDev.getDeviceId());
        doorCmd.setOrder(JdSmartDeviceOrder.OFF);
        doorCmd.setValue1("-1");
        doorDev.setJdSmartCtrlCmd(doorCmd);
        mJdsmartDevices.addDevice(doorDev);
        mMapDevices.put(doorDev.getDeviceId(), doorDev);

        JdSmartDevice projectDev = new JdSmartDevice();
        projectDev.setVendor(Vendor.VENDOR_CUSTOM);
        projectDev.setDeviceId("Project_" + 0);
        projectDev.setDeviceName("投影仪");
        projectDev.setRoomId(roomList.get(0));
        projectDev.setZoneId(floorList.get(0));
        projectDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_PROJECT + "");
        projectDev.setOnline(1);
        JdSmartCtrlCmd projectCmd = new JdSmartCtrlCmd();
        projectCmd.setVendor(Vendor.VENDOR_CUSTOM);
        projectCmd.setDeviceId(projectDev.getDeviceId());
        projectCmd.setOrder(JdSmartDeviceOrder.OFF);
        projectCmd.setValue1("-1");
        projectDev.setJdSmartCtrlCmd(projectCmd);
        mJdsmartDevices.addDevice(projectDev);
        mMapDevices.put(projectDev.getDeviceId(), projectDev);

        JdSmartDevice catCameraDev = new JdSmartDevice();
        catCameraDev.setVendor(Vendor.VENDOR_CUSTOM);
        catCameraDev.setDeviceId("CatCamera_" + 0);
        catCameraDev.setDeviceName("猫眼");
        catCameraDev.setRoomId(roomList.get(0));
        catCameraDev.setZoneId(floorList.get(0));
        catCameraDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_CAT_CAMERA + "");
        catCameraDev.setOnline(1);
        JdSmartCtrlCmd catCameraCmd = new JdSmartCtrlCmd();
        catCameraCmd.setVendor(Vendor.VENDOR_CUSTOM);
        catCameraCmd.setDeviceId(catCameraDev.getDeviceId());
        catCameraCmd.setOrder(JdSmartDeviceOrder.OFF);
        catCameraCmd.setValue1("-1");
        catCameraDev.setJdSmartCtrlCmd(catCameraCmd);
        mJdsmartDevices.addDevice(catCameraDev);
        mMapDevices.put(catCameraDev.getDeviceId(), catCameraDev);


        JdSmartDevice cameraDev = new JdSmartDevice();
        cameraDev.setVendor(Vendor.VENDOR_CUSTOM);
        cameraDev.setDeviceId("Camera_" + 0);
        cameraDev.setDeviceName("摄像头");
        cameraDev.setRoomId(roomList.get(0));
        cameraDev.setZoneId(floorList.get(0));
        cameraDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_CAMERA + "");
        cameraDev.setOnline(1);
        JdSmartCtrlCmd cameraCmd = new JdSmartCtrlCmd();
        cameraCmd.setVendor(Vendor.VENDOR_CUSTOM);
        cameraCmd.setDeviceId(cameraDev.getDeviceId());
        cameraCmd.setOrder(JdSmartDeviceOrder.OFF);
        cameraCmd.setValue1("-1");
        cameraDev.setJdSmartCtrlCmd(cameraCmd);
        mJdsmartDevices.addDevice(cameraDev);
        mMapDevices.put(cameraDev.getDeviceId(), cameraDev);


        ArrayList<String> machineArmNameList = new ArrayList<>();
        machineArmNameList.add("厨房机械臂");
        machineArmNameList.add("煤气机械臂");
        for (i = 0; i < 2; i++) {
            JdSmartDevice machineARMDev = new JdSmartDevice();
            machineARMDev.setVendor(Vendor.VENDOR_CUSTOM);
            machineARMDev.setDeviceId("machine_arm_" + i);
            machineARMDev.setDeviceName(machineArmNameList.get(i));
            machineARMDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_MACHINE_ARM + "");
            machineARMDev.setOnline(1);
            machineARMDev.setRoomId(roomList.get(i % roomList.size()));
            machineARMDev.setZoneId(floorList.get(i % floorList.size()));
            JdSmartCtrlCmd machineARMCmd = new JdSmartCtrlCmd();
            machineARMCmd.setVendor(Vendor.VENDOR_CUSTOM);
            machineARMCmd.setDeviceId(machineARMDev.getDeviceId());
            machineARMCmd.setOrder(JdSmartDeviceOrder.OFF);
            machineARMCmd.setValue1("-1");
            machineARMDev.setJdSmartCtrlCmd(machineARMCmd);
            mJdsmartDevices.addDevice(machineARMDev);
            mMapDevices.put(machineARMDev.getDeviceId(), machineARMDev);
        }

        ArrayList<String> tvNameList = new ArrayList<>();
        tvNameList.add("红外电视");
        //虚拟一个电视，可用红外控制, 名字任意，选择设备的时候会显示
        JdSmartDevice TVDev = new JdSmartDevice();
        TVDev.setVendor(Vendor.VENDOR_CUSTOM);
        TVDev.setDeviceId("tv1_id");
        TVDev.setDeviceName(tvNameList.get(0));
        TVDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_TV + "");
        TVDev.setDeviceSubType(JdSmartDeviceType.DEVICE_SUB_TYPE_IR + "");
        TVDev.setOnline(1);
        TVDev.setRoomId(roomList.get(0));
        TVDev.setZoneId(floorList.get(0));
        //设置tv红外学习了哪些红外键值，本次学习了电视开，关，和静音键
        TVDev.setJdIRkeyList(JdSmartIRConstant.IR_KEY_TV_ON + "," + JdSmartIRConstant.IR_KEY_TV_OFF + "," + JdSmartIRConstant.IR_KEY_TV_MUTE + ",");
        JdSmartCtrlCmd TVCmd = new JdSmartCtrlCmd();
        TVCmd.setVendor(Vendor.VENDOR_CUSTOM);
        TVCmd.setDeviceId(TVDev.getDeviceId());
        TVDev.setJdSmartCtrlCmd(TVCmd);
        mJdsmartDevices.addDevice(TVDev);
        mMapDevices.put(TVDev.getDeviceId(), TVDev);

        ArrayList<String> stbNameList = new ArrayList<>();
        stbNameList.add("红外机顶盒");
        //虚拟一个机顶盒，可用红外控制, 名字任意，选择设备的时候会显示
        JdSmartDevice STBDev = new JdSmartDevice();
        STBDev.setVendor(Vendor.VENDOR_CUSTOM);
        STBDev.setDeviceId("stb1_id");
        STBDev.setDeviceName(stbNameList.get(0));
        STBDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_STB + "");
        STBDev.setDeviceSubType(JdSmartDeviceType.DEVICE_SUB_TYPE_IR + "");
        STBDev.setOnline(1);
        STBDev.setRoomId(roomList.get(0));
        STBDev.setZoneId(floorList.get(0));
        //设置红外学习了哪些红外键值，本次学习了开，关
        STBDev.setJdIRkeyList(JdSmartIRConstant.IR_KEY_STB_ON + "," + JdSmartIRConstant.IR_KEY_STB_OFF + ",");
        JdSmartCtrlCmd STBCmd = new JdSmartCtrlCmd();
        STBCmd.setVendor(Vendor.VENDOR_CUSTOM);
        STBCmd.setDeviceId(STBDev.getDeviceId());
        STBDev.setJdSmartCtrlCmd(STBCmd);
        mJdsmartDevices.addDevice(STBDev);
        mMapDevices.put(STBDev.getDeviceId(), STBDev);

        ArrayList<String> boxNameList = new ArrayList<>();
        boxNameList.add("卷闸门");
        //虚拟一个多功能控制盒，可以接普通电机，名字任意，选择设备的时候会显示
        JdSmartDevice controlBoxDev = new JdSmartDevice();
        controlBoxDev.setVendor(Vendor.VENDOR_CUSTOM);
        controlBoxDev.setDeviceId("box1_id");
        controlBoxDev.setDeviceName(boxNameList.get(0));
        //主设备类型在被添加到房间后，主设备类型会被改变，但子类型不变仍然是DEVICE_SUB_TYPE_CONTROL_BOX
        controlBoxDev.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_DOOR + "");
        //如果是控制盒，则需要写子类型是控制盒，因为它可以模拟为多个设备
        controlBoxDev.setDeviceSubType(JdSmartDeviceType.DEVICE_SUB_TYPE_CONTROL_BOX + "");
        controlBoxDev.setOnline(1);
        controlBoxDev.setRoomId("阳台");
        controlBoxDev.setZoneId(floorList.get(0));
        JdSmartCtrlCmd controlBoxCmd = new JdSmartCtrlCmd();
        controlBoxCmd.setVendor(Vendor.VENDOR_CUSTOM);
        controlBoxCmd.setDeviceId(controlBoxDev.getDeviceId());
        controlBoxDev.setJdSmartCtrlCmd(controlBoxCmd);
        mJdsmartDevices.addDevice(controlBoxDev);
        mMapDevices.put(controlBoxDev.getDeviceId(), controlBoxDev);

        JdSmartDevice sosSensor = new JdSmartDevice();
        sosSensor.setVendor(Vendor.VENDOR_CUSTOM);
        sosSensor.setDeviceId("sosSensor_id");
        sosSensor.setDeviceName("sosSensor_name");
        sosSensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_SOS_SENSOR + "");
        sosSensor.setRoomId(roomList.get(2));
        sosSensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd sosSensorCmd = new JdSmartCtrlCmd();
        sosSensorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        sosSensorCmd.setDeviceId(sosSensor.getDeviceId());
        //sosSensorCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        sosSensorCmd.setValue1("0");  //status,
        sosSensorCmd.setValue4("100");
        sosSensor.setJdSmartCtrlCmd(sosSensorCmd);
        mJdsmartDevices.addDevice(sosSensor);
        mMapDevices.put(sosSensor.getDeviceId(), sosSensor);

        JdSmartDevice flammableSensor = new JdSmartDevice();
        flammableSensor.setVendor(Vendor.VENDOR_CUSTOM);
        flammableSensor.setDeviceId("flammableGASSensor_id");
        flammableSensor.setDeviceName("flammableGASSensor_name");
        flammableSensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_FLAMMABLE_GAS + "");
        flammableSensor.setRoomId(roomList.get(2));
        flammableSensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd flammableSensorCmd = new JdSmartCtrlCmd();
        flammableSensorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        flammableSensorCmd.setDeviceId(flammableSensor.getDeviceId());
        //flammableSensorCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        flammableSensorCmd.setValue1("0");
        flammableSensorCmd.setValue4("100");
        flammableSensor.setJdSmartCtrlCmd(flammableSensorCmd);
        mJdsmartDevices.addDevice(flammableSensor);
        mMapDevices.put(flammableSensor.getDeviceId(), flammableSensor);

        JdSmartDevice infraredSensor = new JdSmartDevice();
        infraredSensor.setVendor(Vendor.VENDOR_CUSTOM);
        infraredSensor.setDeviceId("infraredSensor_id");
        infraredSensor.setDeviceName("infraredSensor_name");
        infraredSensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_INFRARED_SENSOR + "");
        infraredSensor.setRoomId(roomList.get(2));
        infraredSensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd infraredSensorCmd = new JdSmartCtrlCmd();
        infraredSensorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        infraredSensorCmd.setDeviceId(infraredSensor.getDeviceId());
        //infraredSensorCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        infraredSensorCmd.setValue1("0");
        infraredSensorCmd.setValue4("100");
        infraredSensor.setJdSmartCtrlCmd(infraredSensorCmd);
        mJdsmartDevices.addDevice(infraredSensor);
        mMapDevices.put(infraredSensor.getDeviceId(), infraredSensor);

        JdSmartDevice magneticWindowSensor = new JdSmartDevice();
        magneticWindowSensor.setVendor(Vendor.VENDOR_CUSTOM);
        magneticWindowSensor.setDeviceId("magneticWindowSensor_id");
        magneticWindowSensor.setDeviceName("magneticWindowSensor_name");
        magneticWindowSensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_MAGNETIC_WINDOW + "");
        magneticWindowSensor.setRoomId(roomList.get(2));
        magneticWindowSensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd magneticWindowSensorCmd = new JdSmartCtrlCmd();
        magneticWindowSensorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        magneticWindowSensorCmd.setDeviceId(magneticWindowSensor.getDeviceId());
        //magneticWindowSensorCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        magneticWindowSensorCmd.setValue1("0");
        magneticWindowSensorCmd.setValue4("100");
        magneticWindowSensor.setJdSmartCtrlCmd(magneticWindowSensorCmd);
        mJdsmartDevices.addDevice(magneticWindowSensor);
        mMapDevices.put(magneticWindowSensor.getDeviceId(), magneticWindowSensor);


        //虚拟CO2传感器
        JdSmartDevice co2DevSensor = new JdSmartDevice();
        co2DevSensor.setVendor(Vendor.VENDOR_CUSTOM);
        co2DevSensor.setDeviceId("CO2_SENSOR_ID_1");
        co2DevSensor.setDeviceName("CO2_SENSOR_1");
        co2DevSensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_CO2_SENSOR + "");
        co2DevSensor.setOnline(1);
        co2DevSensor.setRoomId(roomList.get(2));
        co2DevSensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd co2DevSensorCmd = new JdSmartCtrlCmd();
        co2DevSensorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        co2DevSensorCmd.setDeviceId(co2DevSensor.getDeviceId());
        co2DevSensorCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        co2DevSensorCmd.setValue1("0002.1");
        co2DevSensor.setJdSmartCtrlCmd(co2DevSensorCmd);
        mJdsmartDevices.addDevice(co2DevSensor);
        mMapDevices.put(co2DevSensor.getDeviceId(), co2DevSensor);

        //虚拟温湿度传感器
        JdSmartDevice tmpeHumiditySensor = new JdSmartDevice();
        tmpeHumiditySensor.setVendor(Vendor.VENDOR_CUSTOM);
        tmpeHumiditySensor.setDeviceId("TEMP_HUMIDITY_ID_1");
        tmpeHumiditySensor.setDeviceName("TEMP_HUMIDITY_SENSOR_1");
        tmpeHumiditySensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_TEMP_HUMIDITY_SENSOR + "");
        tmpeHumiditySensor.setOnline(1);
        tmpeHumiditySensor.setRoomId(roomList.get(2));
        tmpeHumiditySensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd tmpeHumiditySensorCmd = new JdSmartCtrlCmd();
        tmpeHumiditySensorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        tmpeHumiditySensorCmd.setDeviceId(tmpeHumiditySensor.getDeviceId());
        tmpeHumiditySensorCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        tmpeHumiditySensorCmd.setValue1("26");
        tmpeHumiditySensorCmd.setValue2("30");
        tmpeHumiditySensor.setJdSmartCtrlCmd(tmpeHumiditySensorCmd);
        mJdsmartDevices.addDevice(tmpeHumiditySensor);
        mMapDevices.put(tmpeHumiditySensor.getDeviceId(), tmpeHumiditySensor);

        //温度传感器
        JdSmartDevice tmpeSensor = new JdSmartDevice();
        tmpeSensor.setVendor(Vendor.VENDOR_CUSTOM);
        tmpeSensor.setDeviceId("TEMPERATURE_ID_1");
        tmpeSensor.setDeviceName("TEMPERATURE_SENSOR_1");
        tmpeSensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_TEMPERATURE_SENSOR + "");
        tmpeSensor.setOnline(1);
        tmpeSensor.setRoomId(roomList.get(2));
        tmpeSensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd tmpeCmd = new JdSmartCtrlCmd();
        tmpeCmd.setVendor(Vendor.VENDOR_CUSTOM);
        tmpeCmd.setDeviceId(tmpeSensor.getDeviceId());
        tmpeCmd.setValue1("2603");
        tmpeCmd.setValue4("100");
        tmpeSensor.setJdSmartCtrlCmd(tmpeCmd);
        mJdsmartDevices.addDevice(tmpeSensor);
        mMapDevices.put(tmpeSensor.getDeviceId(), tmpeSensor);

        //湿度传感器
        JdSmartDevice humditySensor = new JdSmartDevice();
        humditySensor.setVendor(Vendor.VENDOR_CUSTOM);
        humditySensor.setDeviceId("HUMDITY_ID_1");
        humditySensor.setDeviceName("HUMDITY_SENSOR_1");
        humditySensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_HUMIDITY_SENSOR + "");
        humditySensor.setOnline(1);
        humditySensor.setRoomId(roomList.get(2));
        humditySensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd humdityCmd = new JdSmartCtrlCmd();
        humdityCmd.setVendor(Vendor.VENDOR_CUSTOM);
        humdityCmd.setDeviceId(humditySensor.getDeviceId());
        humdityCmd.setValue1("6068");
        humdityCmd.setValue4("100");
        humditySensor.setJdSmartCtrlCmd(humdityCmd);
        mJdsmartDevices.addDevice(humditySensor);
        mMapDevices.put(humditySensor.getDeviceId(), humditySensor);

        //虚拟烟感
        JdSmartDevice smokerSensor = new JdSmartDevice();
        smokerSensor.setVendor(Vendor.VENDOR_CUSTOM);
        smokerSensor.setDeviceId("SMOKER_SENSOR_ID_1");
        smokerSensor.setDeviceName("SMOKER_SENSOR_1");
        smokerSensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_SMOKER_SENSOR + "");
        smokerSensor.setOnline(1);
        smokerSensor.setRoomId(roomList.get(2));
        smokerSensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd smokerSensorCmd = new JdSmartCtrlCmd();
        smokerSensorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        smokerSensorCmd.setDeviceId(smokerSensor.getDeviceId());
        smokerSensorCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        smokerSensorCmd.setValue1("0");
        smokerSensor.setJdSmartCtrlCmd(smokerSensorCmd);
        mJdsmartDevices.addDevice(smokerSensor);
        mMapDevices.put(smokerSensor.getDeviceId(), smokerSensor);

        //pm2.5
        JdSmartDevice pm25Sensor = new JdSmartDevice();
        pm25Sensor.setVendor(Vendor.VENDOR_CUSTOM);
        pm25Sensor.setDeviceId("PM25_SENSOR_ID_1");
        pm25Sensor.setDeviceName("PM25_SENSOR_1");
        pm25Sensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_AIR_PM_25_SENSOR + "");
        pm25Sensor.setOnline(1);
        pm25Sensor.setRoomId(roomList.get(2));
        pm25Sensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd pm25SensorCmd = new JdSmartCtrlCmd();
        pm25SensorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        pm25SensorCmd.setDeviceId(pm25Sensor.getDeviceId());
        pm25SensorCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        pm25SensorCmd.setValue1("100");
        pm25Sensor.setJdSmartCtrlCmd(pm25SensorCmd);
        mJdsmartDevices.addDevice(pm25Sensor);
        mMapDevices.put(pm25Sensor.getDeviceId(), pm25Sensor);

        //水浸传感器
        JdSmartDevice waterSensor = new JdSmartDevice();
        waterSensor.setVendor(Vendor.VENDOR_CUSTOM);
        waterSensor.setDeviceId("WATER_SENSOR_ID_1");
        waterSensor.setDeviceName("WATER_SENSOR_1");
        waterSensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_WATER_SENSOR + "");
        waterSensor.setOnline(1);
        waterSensor.setRoomId(roomList.get(2));
        waterSensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd waterSensorCmd = new JdSmartCtrlCmd();
        waterSensorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        waterSensorCmd.setDeviceId(waterSensor.getDeviceId());
        waterSensorCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        waterSensorCmd.setValue1("0");
        waterSensor.setJdSmartCtrlCmd(waterSensorCmd);
        mJdsmartDevices.addDevice(waterSensor);
        mMapDevices.put(waterSensor.getDeviceId(), waterSensor);

        //CO
        JdSmartDevice coSensor = new JdSmartDevice();
        coSensor.setVendor(Vendor.VENDOR_CUSTOM);
        coSensor.setDeviceId("CO_SENSOR_ID_1");
        coSensor.setDeviceName("CO_SENSOR_1");
        coSensor.setDeviceType(JdSmartDeviceType.DEVICE_TYPE_CO_SENSOR + "");
        coSensor.setOnline(1);
        coSensor.setRoomId(roomList.get(2));
        coSensor.setZoneId(floorList.get(1));
        JdSmartCtrlCmd coSensorCmd = new JdSmartCtrlCmd();
        coSensorCmd.setVendor(Vendor.VENDOR_CUSTOM);
        coSensorCmd.setDeviceId(coSensor.getDeviceId());
        coSensorCmd.setOrder(JdSmartDeviceOrder.CLOSE);
        coSensorCmd.setValue1("1");
        coSensor.setJdSmartCtrlCmd(coSensorCmd);
        mJdsmartDevices.addDevice(coSensor);
        mMapDevices.put(coSensor.getDeviceId(), coSensor);
    }

    private JdSmartDevice findDeviceById(String id) {
        for (JdSmartDevice dev : mJdsmartDevices.getDevices()) {
            if (dev.getDeviceId().equals(id))
                return dev;
        }
        return null;
    }

    private void initScenes() {


        int i;
        //默认的场景图片有： 回家、离家、晨起、运动、睡眠
        String sceenNames[] = {"回家", "离家", "晨起", "运动", "睡眠", "其他"};

        for (i = 0; i < sceenNames.length; i++) {
            JdSmartScene js = new JdSmartScene();
            js.setSceneNo("scene_no_" + i);
            js.setSceneName(sceenNames[i]);
            js.setVendor(Vendor.VENDOR_CUSTOM);
            if (i == 0) {
                ArrayList<String> aliasArr = new ArrayList<String>();
                aliasArr.add("下班到家啦");
                aliasArr.add("我回家了");
                js.setNicknameList(aliasArr);
//                js.setLocalMusicPath("/storage/emulated/0/Music/DreamVillage_GuZheng-pre.mp3");
//                js.setVoiceTTS("欢迎主人回家，饭已做好，请入座");
            }
            mJdSmartScenes.addScene(js);
            mMapScenes.put(js.getSceneNo(), js);
        }

        //下面仅创建3个有效的场景绑定DEMO
        initSceneBind();
    }

    private void initSceneBind() {
        JdSmartDevice light1 = findDeviceById("light_1");
        JdSmartDevice light2 = findDeviceById("light_2");
        JdSmartDevice curtain = findDeviceById("curtain_1");

        JdSmartScene scene1 = mJdSmartScenes.getScenes().get(0);
        JdSmartScene scene2 = mJdSmartScenes.getScenes().get(1);
        JdSmartScene scene3 = mJdSmartScenes.getScenes().get(2);

        //SCENE1 bind light1 and light2
        JdSmartSceneBind scene1Bind = new JdSmartSceneBind();
        scene1Bind.setSceneNo(scene1.getSceneNo()); //scene1 bind

        JdSmartCtrlCmd scene1Light1Cmd = new JdSmartCtrlCmd(); //light 1
        scene1Light1Cmd.setDeviceId(light1.getDeviceId());
        scene1Light1Cmd.setVendor(Vendor.VENDOR_CUSTOM);
        scene1Light1Cmd.setOrder(JdSmartDeviceOrder.ON);
        scene1Light1Cmd.setDelayTime(100);
        scene1Light1Cmd.setSceneBindId("bindId_" + System.nanoTime());
        scene1Light1Cmd.setSceneNO(scene1.getSceneNo());

        JdSmartCtrlCmd scene1Light2Cmd = new JdSmartCtrlCmd();
        scene1Light2Cmd.setDeviceId(light2.getDeviceId());
        scene1Light2Cmd.setVendor(Vendor.VENDOR_CUSTOM);
        scene1Light2Cmd.setOrder(JdSmartDeviceOrder.ON);
        scene1Light2Cmd.setDelayTime(10);
        scene1Light2Cmd.setSceneBindId("bindId_" + System.nanoTime());
        scene1Light2Cmd.setSceneNO(scene1.getSceneNo());

        scene1Bind.updateCmd(scene1Light1Cmd);
        scene1Bind.updateCmd(scene1Light2Cmd);

        mListBind.add(scene1Bind);
        mMapBind.put(scene1Bind.getSceneNo(), scene1Bind);

        //SCENE2 bind light1 and curtain
        JdSmartSceneBind scene2Bind = new JdSmartSceneBind();
        scene2Bind.setSceneNo(scene2.getSceneNo()); //scene2 bind

        JdSmartCtrlCmd scene2Light1Cmd = new JdSmartCtrlCmd();
        scene2Light1Cmd.setDeviceId(light1.getDeviceId());
        scene2Light1Cmd.setVendor(Vendor.VENDOR_CUSTOM);
        scene2Light1Cmd.setOrder(JdSmartDeviceOrder.ON);
        scene2Light1Cmd.setDelayTime(20);
        scene2Light1Cmd.setSceneBindId("bindId_" + System.nanoTime());
        scene2Light1Cmd.setSceneNO(scene2.getSceneNo());

        JdSmartCtrlCmd scene2CurtainCmd = new JdSmartCtrlCmd();
        scene2CurtainCmd.setDeviceId(curtain.getDeviceId());
        scene2CurtainCmd.setVendor(Vendor.VENDOR_CUSTOM);
        scene2CurtainCmd.setOrder(JdSmartDeviceOrder.OPEN);
        scene2CurtainCmd.setValue1("50");
        scene2CurtainCmd.setDelayTime(10);
        scene2CurtainCmd.setSceneBindId("bindId_" + System.nanoTime());
        scene2CurtainCmd.setSceneNO(scene2.getSceneNo());

        scene2Bind.updateCmd(scene2Light1Cmd);
        scene2Bind.updateCmd(scene2CurtainCmd);

        mListBind.add(scene2Bind);
        mMapBind.put(scene2Bind.getSceneNo(), scene2Bind);

        //SCENE3 bind light2 and curtain
        JdSmartSceneBind scene3Bind = new JdSmartSceneBind();
        scene3Bind.setSceneNo(scene3.getSceneNo()); //scene2 bind

        JdSmartCtrlCmd scene3Light1Cmd = new JdSmartCtrlCmd();
        scene3Light1Cmd.setDeviceId(light2.getDeviceId());
        scene3Light1Cmd.setVendor(Vendor.VENDOR_CUSTOM);
        scene3Light1Cmd.setOrder(JdSmartDeviceOrder.ON);
        scene3Light1Cmd.setDelayTime(20);
        scene3Light1Cmd.setSceneBindId("bindId_" + System.nanoTime());
        scene3Light1Cmd.setSceneNO(scene3.getSceneNo());

        JdSmartCtrlCmd scene3CurtainCmd = new JdSmartCtrlCmd();
        scene3CurtainCmd.setDeviceId(curtain.getDeviceId());
        scene3CurtainCmd.setVendor(Vendor.VENDOR_CUSTOM);
        scene3CurtainCmd.setOrder(JdSmartDeviceOrder.OPEN);
        scene3CurtainCmd.setValue1("50");
        scene3CurtainCmd.setDelayTime(10);
        scene3CurtainCmd.setSceneBindId("bindId_" + System.nanoTime());
        scene3CurtainCmd.setSceneNO(scene3.getSceneNo());

        scene3Bind.updateCmd(scene3Light1Cmd);
        scene3Bind.updateCmd(scene3CurtainCmd);

        mListBind.add(scene3Bind);
        mMapBind.put(scene3Bind.getSceneNo(), scene3Bind);
    }

    private void initSOSSensorRecord() {
        JdSmartDevice sosDev = mJdsmartDevices.getDevices().get(3);

        for (int i = 0; i < 20; i++) {
            List<JdSmartSensorRecord> group1ListSOSRecord = new ArrayList<>();
            JdSmartSensorRecordGroup sosRecordGroup1 = new JdSmartSensorRecordGroup();
            sosRecordGroup1.setGroupName("sos_group" + i);
            sosRecordGroup1.setJdSmartSensorRecords(group1ListSOSRecord);

            JdSmartSensorRecord sosGroup1Record1 = new JdSmartSensorRecord();
            sosGroup1Record1.setDeviceId(sosDev.getDeviceId());
            sosGroup1Record1.setCreateTime(System.currentTimeMillis() + "");
            sosGroup1Record1.setVendor(Vendor.VENDOR_CUSTOM);
            sosGroup1Record1.setText("record1");
            group1ListSOSRecord.add(sosGroup1Record1);
            JdSmartSensorRecord sosGroup1Record2 = new JdSmartSensorRecord();
            sosGroup1Record2.setDeviceId(sosDev.getDeviceId());
            sosGroup1Record2.setCreateTime(System.currentTimeMillis() + "");
            sosGroup1Record2.setVendor(Vendor.VENDOR_CUSTOM);
            sosGroup1Record2.setText("record2");
            group1ListSOSRecord.add(sosGroup1Record2);

            mSOSGroup.add(sosRecordGroup1);
        }
    }

    @Override
    public JdSmartHostInfo getHostInfo() {
        return mJdSmartHostInfo;
    }


    @Override
    public void login(String name, String pwd, final JdbaseCallback callback) {
        Log.d(TAG, "debug login");
        if (mJdSmartHostInfo.isSupportLogin()) {
            mUserName = name;
        }
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString("UserName", name).commit();
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString("Password", pwd).commit();

        //设置当前是开始登录状态
        setLoginState(JdSmartLoginConstant.LOGIN_START);


        HttpRequestYniot.login(mContext, "15728367979", "123456",
                0, new HttpManager.OnHttpResponseListener() {
                    @Override
                    public void onHttpResponse(int requestCode, String resultJson, Exception e) {
                        if (StringUtil.isEmpty(resultJson)) {
                            return;
                        }
                        String token = "";
                        JSONObject jsonObject = JSONObject.parseObject(resultJson);
                        if (jsonObject == null) {
                            Toast.makeText(mContext, "登陆出错,请稍后重试", Toast.LENGTH_SHORT);
                        }
                        if (jsonObject.containsKey(Constant.KEY_ERROR_CODE)) {
                            Toast.makeText(mContext, (jsonObject.containsKey(Constant.KEY_ERROR_DESC) ? jsonObject.getString(Constant.KEY_ERROR_DESC) : Constant.VAL_UNKNOWN_ERROR), Toast.LENGTH_SHORT);
                        } else {
                            //登陆成功后保存用户信息
                            UserInfo userInfo = jsonObject.getObject(Constant.KEY_USER_INFO, UserInfo.class);
                            HttpRequestYniot.token = jsonObject.getString(Constant.KEY_TOKEN);
                            HttpRequestYniot.userInfo = userInfo;
                            HttpRequestYniot.userId = userInfo.getUserId();
                            HttpRequestYniot.hostAddress = userInfo.getCurrHostAddress();
                        }
                        Toast.makeText(mContext, "登陆成功" + token, Toast.LENGTH_LONG);

                        //login....
                        /*
                         在自有登录方法的结果回调中，根据实际情况赋值
                            LOGIN_OK = 3;	登录成功
                            LOGIN_FAIL_UNKNOWN = -1;     未知错误
                            LOGIN_FAIL_ACCOUNT_ERROR = -2;	用户名或密码错误
                            LOGIN_FAIL_NETWORK = -3;       网络问题导致登录失败
                        * */
                        //如果支持显示家庭列表，登录成功后回调家庭列表
                        if (false && mJdSmartHostInfo.isEnableSelectFamily()) {
                            if (mFamilyList.size() == 0) {
                                mFamilyList.add(new JdSmartFamily("1", "family1"));
                                mFamilyList.add(new JdSmartFamily("2", "family2"));
                                mFamilyList.add(new JdSmartFamily("3", "family3"));
                            }
                            callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(mFamilyList), "");
                            setLoginState(JdSmartLoginConstant.LOGIN_OK);
                        } else {
                            callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");
                        }
                    }
                });


    }

    //获取当前登录状态
    @Override
    public String getLoginState() {
        String ret = "";
        synchronized (this) {
            ret = JSON.toJSONString(mLoginResult);
        }
        Log.d(TAG, "getLoginState: " + ret);
        return ret;
    }

    //选择家庭，以显示该家庭下的所有设备
    @Override
    public void selectFamily(String familyID, JdbaseCallback callback) {
        Log.d(TAG, "debug selectFamily familyID=" + familyID);
        //设置当前选择的家庭名称，以在房间管理页左上角显示
        mJdSmartHostInfo.setCurFamilyName("test name");
        callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");
        //callback.onResult(JdbaseContant.RESULT_FAIL, "fail", "");
    }

    @Override
    public JdSmartAccount getAccount() {
        Log.d(TAG, "debug getAccount");
        JdSmartAccount account = new JdSmartAccount();
        if (mJdSmartHostInfo.isSupportLogin()) {
            account.setName(PreferenceManager.getDefaultSharedPreferences(mContext).getString("UserName", ""));
        }
        return account;
    }

    @Override
    public void logout(JdbaseCallback callback) {
        Log.d(TAG, "debug logout");
        if (mJdSmartHostInfo.isSupportLogin()) {
            mUserName = "";
            PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString("UserName", "").commit();
        }
        //此处调用自己退出登录的方法
        //logout....

        //回调退出登录的结果
        callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");
        //设置当前为未登录状态
        setLoginState(JdSmartLoginConstant.LOGIN_IDLE);
    }

    @Override
    public void searchAndBindHost(boolean isSearch, JdbaseCallback callback) {
        Log.d(TAG, "debug searchAndBindHost");
        callback.onResult(JdbaseContant.RESULT_FAIL, "fail", "");
    }

    @Override
    public void unbindHost(JdbaseCallback callback) {
        Log.d(TAG, "debug unbindHost");
        callback.onResult(JdbaseContant.RESULT_FAIL, "fail", "");
    }

    @Override
    public void controlScene(JdSmartScene jdsmartScene, JdbaseCallback callback) {
        Log.d(TAG, "debug controlScene jdsmartScene=" + jdsmartScene);

        callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");

        String sceneNO = jdsmartScene.getSceneNo();
        JdSmartSceneBind jdSmartSceneBind = mMapBind.get(sceneNO);
        if (jdSmartSceneBind != null) {
            for (JdSmartCtrlCmd cmd : jdSmartSceneBind.getCmdList()) {
                JdSmartDevice dev = mMapDevices.get(cmd.getDeviceId());
                if (dev != null) {
                    convertToCustomCmd(cmd, dev); //转化为客户主机命令，并执行命令
                    changeDeviceStatus(cmd, dev); //根据客户主机执行命令的结果，返回状态给上层UI
                }
            }
        }
    }

    @Override
    public void getScenes(JdbaseCallback callback) {
        Log.d(TAG, "debug getScenes >>>  " + JSON.toJSONString(mJdSmartScenes));
        callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(mJdSmartScenes), "");
    }

    @Override
    public void createScene(JdSmartScene jdsmartScene, JdbaseCallback callback) {
        Log.d(TAG, "debug createScene");

        JdSmartScene newScene = new JdSmartScene();
        newScene.setSceneNo("scene" + System.nanoTime() + "_no");
        newScene.setSceneName(jdsmartScene.getSceneName());
        newScene.setVendor(Vendor.VENDOR_CUSTOM);

        mJdSmartScenes.addScene(newScene);
        mMapScenes.put(newScene.getSceneNo(), newScene);

        callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(newScene), "");
    }

    @Override
    public void deleteScene(JdSmartScene jdsmartScene, JdbaseCallback callback) {
        Log.d(TAG, "debug deleteScene");
        JdSmartScene delScene = mMapScenes.get(jdsmartScene.getSceneNo());
        if (delScene != null) {
            mJdSmartScenes.getScenes().remove(delScene);
            mMapScenes.remove(jdsmartScene.getSceneNo());
            callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");
        } else {
            callback.onResult(JdbaseContant.RESULT_FAIL, "fail", "");
        }
    }

    @Override
    public void updateScene(JdSmartScene jdsmartScene, JdbaseCallback callback) {
        Log.d(TAG, "debug updateScene");
        JdSmartScene modify = mMapScenes.get(jdsmartScene.getSceneNo());
        if (modify != null) {
            modify.setSceneName(jdsmartScene.getSceneName());
            callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");
        } else {
            callback.onResult(JdbaseContant.RESULT_FAIL, "fail", "");
        }
    }

    @Override
    public void createSceneBind(List<JdSmartCtrlCmd> cmds, JdbaseCallback callback) {
        for (JdSmartCtrlCmd cmd : cmds) {
            Log.d(TAG, "debug createSceneBind cmd=" + cmd);
        }

        if (cmds.size() > 0) {
            String sceneNO = cmds.get(0).getSceneNO();
            JdSmartSceneBind jdSmartSceneBind = mMapBind.get(sceneNO);
            if (jdSmartSceneBind != null) {
                Log.d(TAG, "already have the sceneNO, add new cmds");
                for (JdSmartCtrlCmd cmd : cmds) {
                    jdSmartSceneBind.getCmdList().add(cmd);
                }
                callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");
            } else {
                jdSmartSceneBind = new JdSmartSceneBind();
                jdSmartSceneBind.setSceneNo(sceneNO);
                jdSmartSceneBind.setCmdList(cmds);

                mListBind.add(jdSmartSceneBind);
                mMapBind.put(sceneNO, jdSmartSceneBind);
                callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");
            }
        } else {
            Log.e(TAG, "error, cmds.size()<0!!!!!!!!!");
            callback.onResult(JdbaseContant.RESULT_FAIL, "failed", "");
        }
    }

    @Override
    public void deleteSceneBind(List<JdSmartCtrlCmd> cmds, JdbaseCallback callback) {
        for (JdSmartCtrlCmd cmd : cmds) {
            Log.d(TAG, "debug deleteSceneBind cmd=" + cmd);
        }
        if (cmds.size() > 0) {
            List<JdSmartCtrlCmd> del = new ArrayList<JdSmartCtrlCmd>();
            String sceneNO = cmds.get(0).getSceneNO();
            JdSmartSceneBind jdSmartSceneBind = mMapBind.get(sceneNO);
            if (jdSmartSceneBind != null) {
                for (JdSmartCtrlCmd cmd : jdSmartSceneBind.getCmdList()) {
                    for (JdSmartCtrlCmd newCmd : cmds) {
                        if (cmd.getDeviceId().equals(newCmd.getDeviceId())) {
                            del.add(cmd);
                        }
                    }
                }
                for (JdSmartCtrlCmd cmd : del) {
                    jdSmartSceneBind.getCmdList().remove(cmd);
                }
                callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");
            } else {
                Log.e(TAG, "error, no the scene bind!!!!!!!!!");
                callback.onResult(JdbaseContant.RESULT_FAIL, "failed", "");
            }
        } else {
            Log.e(TAG, "error, cmds.size()<0!!!!!!!!!");
            callback.onResult(JdbaseContant.RESULT_FAIL, "failed", "");
        }
    }

    @Override
    public void updateSceneBind(List<JdSmartCtrlCmd> cmds, JdbaseCallback callback) {
        for (JdSmartCtrlCmd cmd : cmds) {
            Log.d(TAG, "debug updateSceneBind cmd=" + cmd);
        }

        if (cmds.size() > 0) {
            String sceneNO = cmds.get(0).getSceneNO();
            JdSmartSceneBind bind = mMapBind.get(sceneNO);
            if (bind != null) {
                for (JdSmartCtrlCmd cmd : bind.getCmdList()) {
                    for (JdSmartCtrlCmd newCmd : cmds) {
                        if (cmd.getDeviceId().equals(newCmd.getDeviceId())) {
                            cmd.setCtrlCmd(newCmd);
                        }
                    }
                }
                callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");
            } else {
                Log.e(TAG, "error, no the scene bind!!!!!!!!!");
                callback.onResult(JdbaseContant.RESULT_FAIL, "failed", "");
            }
        } else {
            Log.e(TAG, "error, cmds.size()<0!!!!!!!!!");
            callback.onResult(JdbaseContant.RESULT_FAIL, "failed", "");
        }
    }

    @Override
    public void getSceneBind(JdSmartScene jdsmartScene, JdbaseCallback callback) {
        Log.d(TAG, "debug getSceneBind jdsmartScene=" + jdsmartScene);

        JdSmartDevices retJdsmartDevices = new JdSmartDevices();
        retJdsmartDevices.setVendor(Vendor.VENDOR_CUSTOM);
        JdSmartSceneBind bind = mMapBind.get(jdsmartScene.getSceneNo());
        if (bind != null) {
            for (JdSmartCtrlCmd cmd : bind.getCmdList()) {
                JdSmartDevice dev = mMapDevices.get(cmd.getDeviceId());
                JdSmartDevice jds = new JdSmartDevice();
                jds.setJdSmartDevice(dev);
                jds.setJdSmartCtrlCmd(cmd);
                retJdsmartDevices.addDevice(jds);
            }
            callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(retJdsmartDevices), "");
        } else {
            callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(retJdsmartDevices), "");
            //callback.onResult(JdbaseContant.RESULT_FAIL, "failed", "");
        }
    }

    @Override
    public void getAllDevices(JdbaseCallback callback) {
        Log.d(TAG, "debug getAllDevices");
        if (mJdSmartHostInfo.isSupportLogin() && (mUserName != null && !mUserName.equals(""))) {
            callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(mJdsmartDevices), "");
        } else {
            callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(mJdsmartDevices), "");
        }
    }

    @Override
    public void getAllDeviceType(JdbaseCallback callback) {
        Log.d(TAG, "debug getAllDeviceType");
        List result = new ArrayList();

        for (JdSmartDevice dev : mJdsmartDevices.getDevices()) {
            if (!result.contains(dev.getDeviceType()))
                result.add(dev.getDeviceType());
        }

        callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(result), "");
    }

    @Override
    public JdSmartDevices getDevicesByType(int deviceType) {
        Log.d(TAG, "debug getDevicesByType");
        JdSmartDevices ret = new JdSmartDevices();
        String strDevType = deviceType + "";
        for (JdSmartDevice dev : mJdsmartDevices.getDevices()) {
            if (strDevType.equals(dev.getDeviceType()))
                ret.addDevice(dev);
        }

        return ret;
    }

    @Override
    public void getDeviceDetail(final String deviceID, final JdbaseCallback callback) {
        Log.d(TAG, "debug getDeviceDetail");
        JdSmartDevice dev = mMapDevices.get(deviceID);
        if (dev != null) {
            callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(dev), "");
        } else {
            callback.onResult(JdbaseContant.RESULT_FAIL, "failed", "");
        }
    }

    @Override
    public void controlDevice(JdSmartCtrlCmd cmd, JdbaseCallback callback) {
        Log.d(TAG, "debug controlDevice cmd=" + cmd);
        if (callback != null) {
            Log.d(TAG, "debug 1");
            callback.onResult(JdbaseContant.RESULT_SUCCESS, "success", "");
        }
        Log.d(TAG, "debug controlDevice 2");
        JdSmartDevice dev = mMapDevices.get(cmd.getDeviceId());
        if (dev != null) {
            convertToCustomCmd(cmd, dev); //转化为客户主机命令，并执行命令
            changeDeviceStatus(cmd, dev); //根据客户主机执行命令的结果，返回状态给上层UI
        }

        Log.d(TAG, "debug controlDevice 3");
    }

    @Override
    public void getSensorRecord(String deviceid, int pageIndex, int pageSize, JdbaseCallback callback) {
        Log.d(TAG, "debug getSensorRecord deviceid=" + deviceid + ",pageIndex=" + pageIndex + ",pageSize=" + pageSize);
        if (callback != null) {
            JdSmartDevice sosDev = mJdsmartDevices.getDevices().get(3);
            JdSmartDevice gasDev = mJdsmartDevices.getDevices().get(4);
            JdSmartDevice infraredDev = mJdsmartDevices.getDevices().get(5);
            JdSmartDevice windowdDev = mJdsmartDevices.getDevices().get(6);
            if (deviceid.equals(sosDev.getDeviceId())) {
                callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(mSOSGroup), "");
            } else {
                callback.onResult(JdbaseContant.RESULT_SUCCESS, JSON.toJSONString(mSOSGroup), ""); //only for demo
            }
        }
    }

    @Override
    public void registerDeviceChange(JdbaseCallback callback) {
        Log.d(TAG, "debug registerDeviceChange");
        mUpdateDeviceCallback = callback;
    }

    @Override
    public void setVoiceText(String voiceText, final IAidlCallback callback) {
        Log.d(TAG, "ACTION_SET_VOICE_TEXT data=" + voiceText);

        if (voiceText.equals("播放刘德华的歌曲") || voiceText.equals("打开客厅灯")) {
            try {
                callback.onResult(JdbaseContant.RESULT_SUCCESS, "我能够处理" + voiceText, null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            try {
                callback.onResult(JdbaseContant.RESULT_FAIL, "我不能处理", null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    private String updateIRAirconditionState(JdSmartCtrlCmd jdCmd) {
        String value1 = jdCmd.getValue1() == null ? "" : jdCmd.getValue1();
        String value2 = jdCmd.getValue2() == null ? "" : jdCmd.getValue2();
        switch (jdCmd.getOrder()) {
            case JdSmartDeviceOrder.SET:
                if (jdCmd.getValue1() == null) {
                    return "";
                }
                switch (value1) {
                    case JdSmartDeviceOrder.AIRCONDITION_MODE_TYPE:  //heating, cool mode
                        irAirGroupData.put(JdSmartDeviceOrder.AIRCONDITION_MODE_TYPE, value2);
                        break;
                    case JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_TYPE://wind rate
                        irAirGroupData.put(JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_TYPE, value2);
                        break;
                    case JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_TYPE:
                        irAirGroupData.put(JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_TYPE, value2);
                    default:
                        Log.e(TAG, "unknown statusType:" + value1);
                        break;
                }
                break;
            case JdSmartDeviceOrder.MOVE_TO_LEVEL:
                int temp = Integer.parseInt(value1);
                if (temp < 17) {
                    temp = 17;
                } else if (temp > 30) {
                    temp = 30;
                }
                irAirGroupData.put(JdSmartDeviceOrder.TEMPERATURE, temp + ""); //空调温度
                break;
            default:
                Log.e(TAG, "unknown ir order:" + jdCmd);
                break;
        }
        return irAirGroupData.toJSONString();
    }

    private void changeDeviceStatus(JdSmartCtrlCmd cmd, JdSmartDevice dev) {
        if (dev != null) {
            int devSubType = dev.getDeviceSubType() != null ? Integer.parseInt(dev.getDeviceSubType()) : -1;

            if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_SWITCH + "")) {
                dev.getJdSmartCtrlCmd().setValue1(cmd.getOrder().contains(JdSmartDeviceOrder.ON) ? "0" : "-1");
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_FAN + "")) {
                if (JdSmartDeviceOrder.SET.equals(cmd.getOrder())) {
                    String val1 = cmd.getValue1();
                    if (JdSmartDeviceOrder.FAN_START_PIVOT.equals(val1)) {
                        dev.getJdSmartCtrlCmd().setValue1("0");
                        dev.getJdSmartCtrlCmd().setValue2(JdSmartDeviceOrder.FAN_START_PIVOT);
                    } else if (JdSmartDeviceOrder.FAN_STOP_PIVOT.equals(val1)) {
                        dev.getJdSmartCtrlCmd().setValue1("0");
                        dev.getJdSmartCtrlCmd().setValue2(JdSmartDeviceOrder.FAN_STOP_PIVOT);
                    }
                } else {
                    dev.getJdSmartCtrlCmd().setValue1(cmd.getOrder().contains(JdSmartDeviceOrder.OPEN) ? "0" : "-1");
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_MACHINE_ARM + "")) {
                dev.getJdSmartCtrlCmd().setValue1(cmd.getOrder().contains(JdSmartDeviceOrder.ON) ? "0" : "-1");
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_LAMP + "")) {
                if (devSubType == -1) {
                    Log.d(TAG, "changeDeviceStatus: 普通灯");
                    dev.getJdSmartCtrlCmd().setValue1(cmd.getOrder().contains(JdSmartDeviceOrder.ON) ? "0" : "-1");
                } else {
                    JSONObject jobj = null;
                    String gpData = dev.getJdSmartCtrlCmd().getGroupData();
                    if (!TextUtils.isEmpty(gpData)) {
                        jobj = JSON.parseObject(gpData);
                    } else {
                        jobj = new JSONObject();
                    }
                    Log.d(TAG, "lamp json=" + jobj.toString());
                    if (JdSmartDeviceOrder.ON.equals(cmd.getOrder())) {
                        dev.getJdSmartCtrlCmd().setValue1("0");
                    } else if (JdSmartDeviceOrder.OFF.equals(cmd.getOrder())) {
                        dev.getJdSmartCtrlCmd().setValue1("-1");
                    } else if (JdSmartDeviceOrder.SET.equals(cmd.getOrder())) {
                        String mode = cmd.getValue1();
                        String val = cmd.getValue2();
                        String bright = cmd.getValue3();
                        String saturation = cmd.getValue4();
                        Log.d(TAG, "mode=" + mode + ",val=" + val + ",bright=" + bright + ",saturation=" + saturation);
                        if (devSubType == JdSmartDeviceType.DEVICE_SUB_LAMP_TYPE_COLOR_RGB) {
                            if (JdSmartDeviceOrder.COLOR_LAMP_MODE_FLASH.equals(mode)) {
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_MODE, JdSmartDeviceOrder.COLOR_LAMP_MODE_FLASH);
                            } else {
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_MODE, JdSmartDeviceOrder.COLOR_LAMP_MODE_RGB);
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_VALUE, val);
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_BRIGHT, bright);
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_SATURATION, saturation);
                            }
                        } else if (devSubType == JdSmartDeviceType.DEVICE_SUB_LAMP_TYPE_COLOR_CW) {
                            jobj.put(JdSmartDeviceOrder.COLOR_LAMP_MODE, JdSmartDeviceOrder.COLOR_LAMP_MODE_CW);
                            jobj.put(JdSmartDeviceOrder.COLOR_LAMP_CW_VALUE, val);
                            jobj.put(JdSmartDeviceOrder.COLOR_LAMP_CW_BRIGHT, bright);
                        } else if (devSubType == JdSmartDeviceType.DEVICE_SUB_LAMP_TYPE_COLOR_RGBCW) {
                            if (JdSmartDeviceOrder.COLOR_LAMP_MODE_FLASH.equals(mode)) {
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_MODE, JdSmartDeviceOrder.COLOR_LAMP_MODE_FLASH);
                            } else if (JdSmartDeviceOrder.COLOR_LAMP_MODE_RGB.equals(mode)) {
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_MODE, JdSmartDeviceOrder.COLOR_LAMP_MODE_RGB);
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_VALUE, val);
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_BRIGHT, bright);
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_RGB_SATURATION, saturation);
                            } else {
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_MODE, JdSmartDeviceOrder.COLOR_LAMP_MODE_CW);
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_CW_VALUE, val);
                                jobj.put(JdSmartDeviceOrder.COLOR_LAMP_CW_BRIGHT, bright);
                            }
                        }
                        dev.getJdSmartCtrlCmd().setGroupData(jobj.toJSONString());
                    }
                    Log.d(TAG, "lamp dev.getJdSmartCtrlCmd()=" + dev.getJdSmartCtrlCmd().toString());
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_SOCKECT + "")) {
                dev.getJdSmartCtrlCmd().setValue1(cmd.getOrder().contains(JdSmartDeviceOrder.ON) ? "0" : "-1");
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_DIMMER + "") || dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_DIMMER_2 + "")) {
                if (cmd.getOrder().contains(JdSmartDeviceOrder.MOVE_TO_LEVEL)) {
                    int level = Integer.parseInt(cmd.getValue2());
                    dev.getJdSmartCtrlCmd().setValue1(level > 0 ? "0" : "-1");
                    dev.getJdSmartCtrlCmd().setValue2(level + "");
                } else {
                    dev.getJdSmartCtrlCmd().setValue1(cmd.getOrder().contains(JdSmartDeviceOrder.ON) ? "0" : "-1");
                    dev.getJdSmartCtrlCmd().setValue2(cmd.getOrder().contains(JdSmartDeviceOrder.ON) ? cmd.getValue2() : "0");
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_DIMMER_2 + "")) {
                if (cmd.getOrder().contains(JdSmartDeviceOrder.ACTION_INCREASE) || cmd.getOrder().contains(JdSmartDeviceOrder.ACTION_DECREASE)) {
                    //xxx
                } else {
                    dev.getJdSmartCtrlCmd().setValue1(cmd.getOrder().contains(JdSmartDeviceOrder.ON) ? "0" : "-1");
                    dev.getJdSmartCtrlCmd().setValue2(cmd.getOrder().contains(JdSmartDeviceOrder.ON) ? cmd.getValue2() : "0");
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_CURTAINS + "")) {
                dev.getJdSmartCtrlCmd().setValue1(cmd.getValue1());
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_AIRCONDITION + "")) {
                Log.d(TAG, "aircondition status");
                String groupData = "";
                if (JdSmartDeviceType.DEVICE_SUB_TYPE_IR == devSubType) {
                    Log.d(TAG, "changeDeviceStatus: 刷新红外空调状态");
                    groupData = updateIRAirconditionState(cmd);
                } else {
                    if (JdSmartDeviceOrder.SET.equals(cmd.getOrder())) {
                        if (cmd.getValue1() == null || cmd.getValue2() == null) {
                            return;
                        }
                        groupData = onSetAirConditionStatus(cmd.getValue1(), cmd.getValue2());
                    } else {
                        groupData = updateAirCondition();
                    }
                }
                dev.getJdSmartCtrlCmd().setValue1(mAirconditionOnoff + "");
                dev.getJdSmartCtrlCmd().setGroupData(groupData);
                Log.d(TAG, "changeDeviceStatus: 空调 groupdata = " + groupData);
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_WINDOW_CONTROLER + "")) {
                dev.getJdSmartCtrlCmd().setValue1(cmd.getValue1());
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_HEATER + "")) {
                Log.d(TAG, "debug controlDevice cmd.getOrder()=" + cmd.getOrder());
                if (cmd.getOrder().contains(JdSmartDeviceOrder.MOVE_TO_LEVEL)) {
                    int level = Integer.parseInt(cmd.getValue1());
                    dev.getJdSmartCtrlCmd().setValue1(level > 0 ? level + "" : "-1");
                } else {
                    dev.getJdSmartCtrlCmd().setValue1(cmd.getOrder().contains(JdSmartDeviceOrder.OPEN) ? "26" : "0");
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_CURTAINS_NO_POSITION + "")) {
                if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                    dev.getJdSmartCtrlCmd().setValue1(100 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                } else {
                    dev.getJdSmartCtrlCmd().setValue1(50 + "");
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_CLOTHES_HANGER + "")) {
                if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                    dev.getJdSmartCtrlCmd().setValue1(-1 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.SET)) {
                    String value1_mode = cmd.getValue1();
//                    JSONObject jobj = new JSONObject();
                    JSONObject jobj = JSONObject.parseObject(dev.getJdSmartCtrlCmd().getGroupData());
                    switch (value1_mode) {
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_MOVE_UP:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_MOVE, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_MOVE_UP);
                            break;
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_MOVE_DOWN:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_MOVE, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_MOVE_DOWN);
                            break;
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_MOVE_STOP:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_MOVE, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_MOVE_STOP);
                            break;
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_DESINFECTION_OFF:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_DESINFECTION, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_DESINFECTION_OFF);
                            break;
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_DESINFECTION_ON:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_DESINFECTION, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_DESINFECTION_ON);
                            break;
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_HEAT_OFF:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_HEAT, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_HEAT_OFF);
                            break;
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_HEAT_ON:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_HEAT, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_HEAT_ON);
                            break;
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_LIGHT_OFF:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_LIGHT, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_LIGHT_OFF);
                            break;
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_LIGHT_ON:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_LIGHT, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_LIGHT_ON);
                            break;
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_WIND_OFF:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_WIND, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_WIND_OFF);
                            break;
                        case JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_WIND_ON:
                            jobj.put(JdSmartDeviceOrder.CLOTHES_HANGER_STATUS_WIND, JdSmartDeviceOrder.CLOTHES_HANGER_ORDER_WIND_ON);
                            break;
                    }
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                    dev.getJdSmartCtrlCmd().setGroupData(jobj.toJSONString());
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_SWEEPER_MACHINE + "")) {
                if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                    dev.getJdSmartCtrlCmd().setValue1(-1 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.SET)) {
                    String value1_mode = cmd.getValue1();
                    JSONObject jobj = new JSONObject();
                    switch (value1_mode) {
                        case JdSmartDeviceOrder.SWEEPER_MACHINE_SWEEPER_MODE:
                            jobj.put(JdSmartDeviceOrder.SWEEPER_MACHINE_STATUS, JdSmartDeviceOrder.SWEEPER_MACHINE_SWEEPER_MODE);
                            break;
                        case JdSmartDeviceOrder.SWEEPER_MACHINE_AUTO_MODE:
                            jobj.put(JdSmartDeviceOrder.SWEEPER_MACHINE_STATUS, JdSmartDeviceOrder.SWEEPER_MACHINE_AUTO_MODE);
                            break;
                        case JdSmartDeviceOrder.SWEEPER_MACHINE_FORCE_MODE:
                            jobj.put(JdSmartDeviceOrder.SWEEPER_MACHINE_STATUS, JdSmartDeviceOrder.SWEEPER_MACHINE_FORCE_MODE);
                            break;
                        case JdSmartDeviceOrder.SWEEPER_MACHINE_MUTE_MODE:
                            jobj.put(JdSmartDeviceOrder.SWEEPER_MACHINE_STATUS, JdSmartDeviceOrder.SWEEPER_MACHINE_MUTE_MODE);
                            break;
                        case JdSmartDeviceOrder.SWEEPER_MACHINE_NORMAL_MODE:
                            jobj.put(JdSmartDeviceOrder.SWEEPER_MACHINE_STATUS, JdSmartDeviceOrder.SWEEPER_MACHINE_NORMAL_MODE);
                            break;
                        case JdSmartDeviceOrder.SWEEPER_MACHINE_CHARGE_MODE:
                            jobj.put(JdSmartDeviceOrder.SWEEPER_MACHINE_STATUS, JdSmartDeviceOrder.SWEEPER_MACHINE_CHARGE_MODE);
                            break;
                    }
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                    dev.getJdSmartCtrlCmd().setGroupData(jobj.toJSONString());
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_PROJECT + "")) {
                if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                    dev.getJdSmartCtrlCmd().setValue1(-1 + "");
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_DOOR + "")) {
                if (JdSmartDeviceType.DEVICE_SUB_TYPE_CONTROL_BOX == devSubType) {
                    if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                        dev.getJdSmartCtrlCmd().setValue1("100");
                    } else if (cmd.getOrder().equals(JdSmartDeviceOrder.STOP)) {
                        dev.getJdSmartCtrlCmd().setValue1("50");
                    } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                        dev.getJdSmartCtrlCmd().setValue1("0");
                    }
                } else {
                    if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                        dev.getJdSmartCtrlCmd().setValue1(0 + "");
                    } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                        dev.getJdSmartCtrlCmd().setValue1(-1 + "");
                    }
                }

            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_CAT_CAMERA + "")) {
                if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                    dev.getJdSmartCtrlCmd().setValue1(-1 + "");
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_CAMERA + "")) {
                if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                    dev.getJdSmartCtrlCmd().setValue1(-1 + "");
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_FRESH_AIR + "")) {
                Log.d(TAG, "changeDeviceStatus: DEVICE_TYPE_FRESH_AIR");
                JSONObject jsonObject = null;
                if (!TextUtils.isEmpty(cmd.getGroupData())) {
                    jsonObject = JSONObject.parseObject(cmd.getGroupData());
                } else {
                    jsonObject = JSON.parseObject(dev.getJdSmartCtrlCmd().getGroupData());
                }
                if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                    dev.getJdSmartCtrlCmd().setValue1(-1 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.SET)) {
                    String value1_mode = cmd.getValue1();
                    switch (value1_mode) {
                        case JdSmartDeviceOrder.FRESH_AIR_ORDER_WIND_HIGH:
                            jsonObject.put(JdSmartDeviceOrder.FRESH_AIR_STATUS_WIND, JdSmartDeviceOrder.FRESH_AIR_ORDER_WIND_HIGH);
                            break;
                        case JdSmartDeviceOrder.FRESH_AIR_ORDER_WIND_MID:
                            jsonObject.put(JdSmartDeviceOrder.FRESH_AIR_STATUS_WIND, JdSmartDeviceOrder.FRESH_AIR_ORDER_WIND_MID);
                            break;
                        case JdSmartDeviceOrder.FRESH_AIR_ORDER_WIND_LOW:
                            jsonObject.put(JdSmartDeviceOrder.FRESH_AIR_STATUS_WIND, JdSmartDeviceOrder.FRESH_AIR_ORDER_WIND_LOW);
                            break;
                    }
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                    dev.getJdSmartCtrlCmd().setGroupData(jsonObject.toJSONString());
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_FLOOR_HEATING + "")) {
                JSONObject jsonObject = null;
                if (!TextUtils.isEmpty(cmd.getGroupData())) {
                    jsonObject = JSONObject.parseObject(cmd.getGroupData());
                } else {
                    jsonObject = JSONObject.parseObject(dev.getJdSmartCtrlCmd().getGroupData());
                }

                if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                    dev.getJdSmartCtrlCmd().setValue1(-1 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.MOVE_TO_LEVEL)) {
                    String temeperature = cmd.getValue1();
                    int temp = Integer.parseInt(temeperature);
                    if (temp < 16) {
                        temp = 16;
                    } else if (temp > 32) {
                        temp = 31;
                    }
                    jsonObject.put(JdSmartDeviceOrder.FLOOR_HEATING_STATUS_TEMP, temp + "");
                    dev.getJdSmartCtrlCmd().setGroupData(jsonObject.toJSONString());
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.SET)) {
                    String value1_mode = cmd.getValue1();
                    switch (value1_mode) {
                        case JdSmartDeviceOrder.FLOOR_HEATING_ORDER_MODE_AUTO:
                            jsonObject.put(JdSmartDeviceOrder.FLOOR_HEATING_STATUS_MODE, JdSmartDeviceOrder.FLOOR_HEATING_ORDER_MODE_AUTO);
                            break;
                        case JdSmartDeviceOrder.FLOOR_HEATING_ORDER_MODE_MANUAL:
                            jsonObject.put(JdSmartDeviceOrder.FLOOR_HEATING_STATUS_MODE, JdSmartDeviceOrder.FLOOR_HEATING_ORDER_MODE_MANUAL);
                            break;
                        case JdSmartDeviceOrder.FLOOR_HEATING_ORDER_MODE_SLEEP:
                            jsonObject.put(JdSmartDeviceOrder.FLOOR_HEATING_STATUS_MODE, JdSmartDeviceOrder.FLOOR_HEATING_ORDER_MODE_SLEEP);
                            break;
                    }
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                    dev.getJdSmartCtrlCmd().setGroupData(jsonObject.toJSONString());
                }
            } else if (dev.getDeviceType().equals(JdSmartDeviceType.DEVICE_TYPE_AIRPURIFIER + "")) {
                JSONObject jsonObject = null;
                if (!TextUtils.isEmpty(cmd.getGroupData())) {
                    jsonObject = JSONObject.parseObject(cmd.getGroupData());
                } else {
                    jsonObject = JSON.parseObject(dev.getJdSmartCtrlCmd().getGroupData());
                }

                if (cmd.getOrder().equals(JdSmartDeviceOrder.OPEN)) {
                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.CLOSE)) {
                    dev.getJdSmartCtrlCmd().setValue1(-1 + "");
                } else if (cmd.getOrder().equals(JdSmartDeviceOrder.SET)) {
                    String value1_mode = cmd.getValue1();
                    switch (value1_mode) {
                        case JdSmartDeviceOrder.AIRPURIFIER_ORDER_MODE_AUTO:
                            jsonObject.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_MODE, JdSmartDeviceOrder.AIRPURIFIER_ORDER_MODE_AUTO);
                            break;
                        case JdSmartDeviceOrder.AIRPURIFIER_ORDER_MODE_MANUAL:
                            jsonObject.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_MODE, JdSmartDeviceOrder.AIRPURIFIER_ORDER_MODE_MANUAL);
                            break;
                        case JdSmartDeviceOrder.AIRPURIFIER_ORDER_MODE_SLEEP:
                            jsonObject.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_MODE, JdSmartDeviceOrder.AIRPURIFIER_ORDER_MODE_SLEEP);
                            break;
                        case JdSmartDeviceOrder.AIRPURIFIER_ORDER_WIND_HIGHT:
                            jsonObject.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_WIND, JdSmartDeviceOrder.AIRPURIFIER_ORDER_WIND_HIGHT);
                            break;
                        case JdSmartDeviceOrder.AIRPURIFIER_ORDER_WIND_MID:
                            jsonObject.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_WIND, JdSmartDeviceOrder.AIRPURIFIER_ORDER_WIND_MID);
                            break;
                        case JdSmartDeviceOrder.AIRPURIFIER_ORDER_WIND_LOW:
                            jsonObject.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_WIND, JdSmartDeviceOrder.AIRPURIFIER_ORDER_WIND_LOW);
                            break;
                    }

                    jsonObject.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_PM25, "50");
                    jsonObject.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_TEMP, "23");
                    jsonObject.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_HUMIDITY, "60");
                    jsonObject.put(JdSmartDeviceOrder.AIRPURIFIER_STATUS_AQI, "一级（优）");

                    dev.getJdSmartCtrlCmd().setValue1(0 + "");
                    dev.getJdSmartCtrlCmd().setGroupData(jsonObject.toJSONString());
                }
            } else {
                dev.getJdSmartCtrlCmd().setValue1(cmd.getValue1());
            }

            dev.getJdSmartCtrlCmd().setOrder(cmd.getOrder());

            sendUpdateDeviceMessage(dev.getDeviceId());
        }
    }

    private String mAirconditionTemperature = "26";
    private int mAirconditionOnoff = 0;  //0 for on, -1 for off

    public String convertToCustomCmd(JdSmartCtrlCmd cmd, JdSmartDevice dev) {
        int devType = Integer.parseInt(dev.getDeviceType());
        int devSubType = dev.getDeviceSubType() != null ? Integer.parseInt(dev.getDeviceSubType()) : -1;

        String strCmd = "";

        //判断是否是多功能控制盒类型
        if (devSubType == JdSmartDeviceType.DEVICE_SUB_TYPE_CONTROL_BOX) {
            if (JdSmartDeviceOrder.OPEN.equals(cmd.getOrder())) {
                strCmd = "open";
            } else if (JdSmartDeviceOrder.CLOSE.equals(cmd.getOrder())) {
                strCmd = "close";
            }
            return strCmd;
        }

        switch (devType) {
            case JdSmartDeviceType.DEVICE_TYPE_LAMP://普通灯
                if (JdSmartDeviceOrder.ON.equals(cmd.getOrder())) {
                    strCmd = "on";
                } else if (JdSmartDeviceOrder.OFF.equals(cmd.getOrder())) {
                    strCmd = "off";
                } else if (JdSmartDeviceOrder.SET.equals(cmd.getOrder())) {
                    String mode = cmd.getValue1();
                    String val = cmd.getValue2();
                    String bright = cmd.getValue3();
                    String saturation = cmd.getValue4();
                    switch (mode) {
                        case JdSmartDeviceOrder.COLOR_LAMP_MODE_RGB:
                            break;
                        case JdSmartDeviceOrder.COLOR_LAMP_MODE_CW:
                            break;
                        case JdSmartDeviceOrder.COLOR_LAMP_MODE_FLASH:
                            break;
                    }
                    strCmd = "xxxx";
                }
                break;
            case JdSmartDeviceType.DEVICE_TYPE_DIMMER: //调光灯
                if (JdSmartDeviceOrder.ON.equals(cmd.getOrder())) {
                    strCmd = "open";
                } else if (JdSmartDeviceOrder.OFF.equals(cmd.getOrder())) {
                    strCmd = "close";
                } else if (JdSmartDeviceOrder.MOVE_TO_LEVEL.equals(cmd.getOrder())) {
                    String progress = cmd.getValue2();
                    strCmd = "xxxx";
                }
                break;
            case JdSmartDeviceType.DEVICE_TYPE_CURTAINS:
            case JdSmartDeviceType.DEVICE_TYPE_WINDOW_CONTROLER:
                if (JdSmartDeviceOrder.OPEN.equals(cmd.getOrder())) {
                    strCmd = "open";
                } else if (JdSmartDeviceOrder.CLOSE.equals(cmd.getOrder())) {
                    strCmd = "close";
                } else if (JdSmartDeviceOrder.STOP.equals(cmd.getOrder())) {
                    strCmd = "stop";
                } else if (JdSmartDeviceOrder.MOVE_TO_LEVEL.equals(cmd.getOrder())) {
                    String progress = cmd.getValue1();
                    strCmd = "xxxx";
                }
                break;

            case JdSmartDeviceType.DEVICE_TYPE_AIRCONDITION:
                switch (cmd.getOrder()) {
                    case JdSmartDeviceOrder.OPEN:
                        strCmd = "xxxx";
                        mAirconditionOnoff = 0;
                        break;
                    case JdSmartDeviceOrder.CLOSE:
                        strCmd = "xxxx";
                        mAirconditionOnoff = -1;
                        break;

                    case JdSmartDeviceOrder.NEXT:  //devSubType != JdSmartDeviceType.DEVICE_SUB_TYPE_IR
                        if (JdSmartDeviceOrder.AIRCONDITION_MODE_TYPE.equals(cmd.getValue1())) {
                            onNextAirConditionStatus(JdSmartDeviceOrder.AIRCONDITION_MODE_TYPE);
                        } else if (JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_TYPE.equals(cmd.getValue1())) {
                            onNextAirConditionStatus(JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_TYPE);
                        } else if (JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_TYPE.equals(cmd.getValue1())) {
                            onNextAirConditionStatus(JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_TYPE);
                        }
                        break;

                    case JdSmartDeviceOrder.SET:    //devSubType == JdSmartDeviceType.DEVICE_SUB_TYPE_IR
                        String statusType = cmd.getValue1();
                        String value = cmd.getValue2();
                        Log.d(TAG, "AIRCONDITION set cmd value1=" + statusType + ",value2=" + value);
                        if (statusType == null || value == null) {
                            return "";
                        }
                        switch (statusType) {
                            case JdSmartDeviceOrder.AIRCONDITION_MODE_TYPE:  //heating, cool mode
                                if (value.equals(JdSmartDeviceOrder.AIRCONDITION_MODE_COOL)) {
                                    strCmd = "xxxx";
                                } else if (value.equals(JdSmartDeviceOrder.AIRCONDITION_MODE_HEAT)) {
                                    strCmd = "xxxx";
                                } else if (value.equals(JdSmartDeviceOrder.AIRCONDITION_MODE_DEHUMIDIFY)) {
                                    strCmd = "xxxx";
                                } else if (value.equals(JdSmartDeviceOrder.AIRCONDITION_MODE_WIND)) {
                                    strCmd = "xxxx";
                                } else if (value.equals(JdSmartDeviceOrder.AIRCONDITION_MODE_AUTO)) {
                                    strCmd = "xxxx";
                                }
                                break;
                            case JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_TYPE://wind rate
                                if (value.equals(JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_LOW)) {
                                    strCmd = "xxxx";
                                } else if (value.equals(JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_MIDDLE)) {
                                    strCmd = "xxxx";
                                } else if (value.equals(JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_HIGH)) {
                                    strCmd = "xxxx";
                                } else if (value.equals(JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_AUTO)) {
                                    strCmd = "xxxx";
                                }
                                break;
                            case JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_TYPE:
                                if (value.equals(JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_NO_DIRECTION)) {
                                    strCmd = "xxxx";
                                } else if (value.equals(JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_LEFT_RIGHT) ||
                                        value.equals(JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION__UP_DOWN)) {
                                    strCmd = "xxxx";
                                }
                                break;
                            default:
                                Log.e(TAG, "unknown statusType:" + statusType);
                                break;
                        }
                        break;
                    case JdSmartDeviceOrder.MOVE_TO_LEVEL:
                        String temeperature = cmd.getValue1();
                        int temp = Integer.parseInt(temeperature);
                        if (temp <= 16) {
                            temp = 16;
                        } else if (temp > 30) {
                            temp = 30;
                        }
                        mAirconditionTemperature = temp + "";

                        Log.d(TAG, "aircondition temeperature cmd:" + temp);
                        break;
                    default:
                        Log.e(TAG, "unknown order:" + cmd.getOrder());
                        break;
                }
                break;
            default:
                Log.e(TAG, "unknown dev type:" + devType);
        }
        Log.d(TAG, "customCmd:" + strCmd);
        return strCmd;
    }

    private String mAirconditionWorkMode[] = {JdSmartDeviceOrder.AIRCONDITION_MODE_COOL,
            JdSmartDeviceOrder.AIRCONDITION_MODE_HEAT,
            JdSmartDeviceOrder.AIRCONDITION_MODE_WIND,
            JdSmartDeviceOrder.AIRCONDITION_MODE_DEHUMIDIFY,
            JdSmartDeviceOrder.AIRCONDITION_MODE_AUTO};

    private String mAirconditionWindRate[] = {JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_AUTO,
            JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_HIGH,
            JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_MIDDLE,
            JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_LOW,
            JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_MUTE};

    private String mAirconditionWindDirection[] = {JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_LEFT_RIGHT,
            JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION__UP_DOWN,
            JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_NO_DIRECTION};
    private int mAirconditionWorkModeIndex = 0;
    private int mAirconditionWindRateIndex = 3;
    private int mAirconditionWindDirectionIndex = 0;

    private String onNextAirConditionStatus(String statusType) {
        switch (statusType) {
            case JdSmartDeviceOrder.AIRCONDITION_MODE_TYPE:
                mAirconditionWorkModeIndex++;
                break;
            case JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_TYPE:
                mAirconditionWindRateIndex++;
                break;
            case JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_TYPE:
                mAirconditionWindDirectionIndex++;
                break;
            default:
                break;
        }

        return updateAirCondition();
    }

    private String updateAirCondition() {
        mAirConditionJson.put(JdSmartDeviceOrder.TEMPERATURE, mAirconditionTemperature); //空调温度
        mAirConditionJson.put(JdSmartDeviceOrder.AIRCONDITION_MODE_TYPE, mAirconditionWorkMode[mAirconditionWorkModeIndex % mAirconditionWorkMode.length]);
        mAirConditionJson.put(JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_TYPE, mAirconditionWindRate[mAirconditionWindRateIndex % mAirconditionWindRate.length]);
        mAirConditionJson.put(JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_TYPE, mAirconditionWindDirection[mAirconditionWindDirectionIndex % mAirconditionWindDirection.length]);
        return mAirConditionJson.toJSONString();
    }

    private String onSetAirConditionStatus(String statusType, String value) {
        switch (statusType) {
            case JdSmartDeviceOrder.AIRCONDITION_MODE_TYPE:
                for (int i = 0; i < mAirconditionWorkMode.length; i++) {
                    if (value.equals(mAirconditionWorkMode[i])) {
                        mAirconditionWorkModeIndex = i;
                        break;
                    }
                }
                break;
            case JdSmartDeviceOrder.AIRCONDITION_WIND_RATE_TYPE:
                for (int i = 0; i < mAirconditionWindRate.length; i++) {
                    if (value.equals(mAirconditionWindRate[i])) {
                        mAirconditionWindRateIndex = i;
                        break;
                    }
                }
                break;
            case JdSmartDeviceOrder.AIRCONDITION_WIND_DIRECTION_TYPE:
                for (int i = 0; i < mAirconditionWindDirection.length; i++) {
                    if (value.equals(mAirconditionWindDirection[i])) {
                        mAirconditionWindDirectionIndex = i;
                        break;
                    }
                }
                break;
            default:
                break;
        }
        return updateAirCondition();
    }

    //如果有设备信息变化，要调用这个函数
    private void sendUpdateDeviceMessage(String devID) {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("deviceID", devID);
        msg.setData(bundle);
        msg.what = UpdateDeviceHandler.MSG_UPDATE_DEVICE_INFO;
        mUpdateDeviceHandler.sendMessage(msg);
    }

    private void updateDeviceInfo(JdSmartDevice dev) {
        if (dev != null) {
            mUpdateDeviceCallback.onResult(JdSmartConstant.ACTION_REPORT_DEVICE_STATUS, JSON.toJSONString(dev), "");
        }
    }

    private void notifySceneChange() { //如果场景准备好，或场景名有变化
        mUpdateDeviceCallback.onResult(JdSmartConstant.ACTION_SCENE_NAME_CHANGE, "", "");
    }

    /*
     *
     * 调用这个接口，去通知上层调用getAlldevice接口
     */
    private void notifyDevicesChange() {
        mUpdateDeviceCallback.onResult(JdSmartConstant.ACTION_ALL_DEVICE_INFO_CHANGE, "", "");
    }

    /*
     *   播放TTS文本
     */
    private void playTTS(String tts) {
        mUpdateDeviceCallback.onResult(JdSmartConstant.ACTION_CUSTOM_SMART_PLAY_TTS, tts, "");
    }

    /**
     * 重新导入时，会先调用这个接口，然后调用getAlldevice接口
     */
    public void refreshDevice() {
        Log.d(TAG, "refeshDevice");
    }

    private void requestDataFromSys() {
        Log.d(TAG, "requestDataFromSys ACTION_GET_DEFAULT_FLOOR_ROOM_FROM_SYS");
        mUpdateDeviceCallback.onResult(JdSmartHostActionConstant.ACTION_REQUEST_DATA_FROM_SYS, JdSmartHostActionConstant.ACTION_GET_DEFAULT_FLOOR_ROOM_FROM_SYS, "");
    }

    public void responseDataFromSys(String data1, String data2) {
        Log.d(TAG, "responseDataFromSys data1=" + data1 + ",data2=" + data2);
    }

    /**
     * 根据场景id，播放相应的场景音乐
     *
     * @param sceneNo 场景id
     */
    public void playSceneMusicFromSys(String sceneNo) {
        Log.d(TAG, "playSceneMusicFromSys sceneNo=" + sceneNo);
        mUpdateDeviceCallback.onResult(JdSmartHostActionConstant.ACTION_PLAY_SCENE_MUSIC_FROM_SYS, sceneNo, "");
    }
}
