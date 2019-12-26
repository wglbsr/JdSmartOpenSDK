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

package com.judian.jdsmart.open;


/**
 * 常量工具类
 *
 * @author Lemon
 * @warn TODO 修改里面所有常量
 */
public class Constant {

    public static final String APP_OFFICIAL_BLOG = "http://my.oschina.net/u/2437072";
    public static final String APP_OFFICIAL_EMAIL = "1184482681@qq.com";
    public static final String APP_DOWNLOAD_WEBSITE = "http://files.cnblogs.com/files/tommylemon/ZBLibraryDemoApp.apk";
    public static final String APP_DEVELOPER_WEBSITE = "https://github.com/TommyLemon";
    public static final String UPDATE_LOG_WEBSITE = "github.com/TommyLemon/Android-ZBLibrary/commits/master";


    public static final String KEY_ERROR_CODE = "errCode";
    public static final String KEY_ERROR_DESC = "errDesc";
    public static final String KEY_USER_INFO = "userInfo";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_HOST_ADDRESS = "hostAddress";
    public static final String KEY_DATA = "data";
    public static final String KEY_UUID = "uuid";
    public static final String KEY_USERNAME = "loginName";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_MESSAGE_ID = "messageId";
    public static final String KEY_MOBILE = "mobile";
    public static final String KEY_RE_PASSWORD = "rePassword";
    public static final String KEY_SMS_CODE = "smsCode";
    public static final String KEY_RULE_ID = "ruleId";
    public static final String KEY_TARGET_USER_ID = "targetUserId";
    public static final String KEY_GROUP_USER = "user";
    public static final String KEY_MESSAGE_TYPE = "messageType";
    public static final String KEY_PAGE_NUM = "paged";
    public static final String KEY_TOTAL_PAGE = "totalPage";
    public static final String KEY_LIST = "list";
    public static final String KEY_CACHE_DATA_KEY = "KEY_CACHE_DATA_KEY";

    public static final String KEY_LOGIN_USERNAME = "savedLoginUsername";
    public static final String KEY_LOGIN_PASSWORD = "savedLoginPassword";

    //---------------------------Ken---------------------------
    public static final String KEY_ROOM_TYPE = "roomType";
    public static final String KEY_SCENE_DEVICE_DATA = "sceneDeviceData";
    public static final String KEY_SCENE_DATA = "sceneData";
    public static final String KEY_MAIN_FREQUENTLY_DEVICE_DATA = "mainFrequentlyDeviceData";
    public static final String KEY_MAIN_FREQUENTLY_ROOM_DATA = "mainFrequentlyROOMData";
    public static final String KEY_MAIN_ROOM_NUMBER = "mainRoomNumber";
    public static final String KEY_MAIN_DEVICE_NUMBER = "mainDeviceNumber";
    public static final String KEY_MAIN_ROOM_ALL_LIST = "mainRoomAllList";
    public static final String KEY_MAIN_DEVICE_ALL_LIST = "mainDeviceAllList";
    public static final String KEY_MAIN_ROOM_DETAIL_LIST = "mainRoomDetailList";
    public static final String KEY_EZ_ACCESS_TOKEN = "ezAccessToken";
    public static final String KEY_MAINFRAGMENT_BROADCAST = "yoni.smarthome.fragment.MainFragment.CHANGE_CONTENT";
    public static final String KEY_SCENE_REMOTER_CODE = "sceneRemoterCode";
    public static final String KEY_SCENE_REMOTER_LIST = "sceneRemoterList";
    public static final String KEY_MAIN_HOST_LIST = "mainHostList";
    public static final String KEY_MAIN_HOST_NAME = "mainHostName";
    public static final String KEY_MAIN_REMOTER_CATEGORY = "mainRemoterCategory";
    public static final String KEY_MAIN_REMOTER_BRAND = "mainRemoterBrand";
    public static final String KEY_MAIN_REMOTER_INDEX = "mainRemoterIndex";
    public static final String KEY_SCENE_BROADCAST = "yoni.smarthome.fragment.sceneListFragment.CHANGE_CONTENT";

    public static final String ROOM_TYPE_FREQUENTLY = "Frequently";
    public static final String SCENE_ADD_DEVICE_ITEM = "sceneAddDeviceItem";
    public static final String TRIGGER_ADD_DEVICE_ITEM = "triggerAddDeviceItem";
    public static final String MAIN_ADD_FREQUENTLY_ROOM_ITEM = "mainAddFrequentlyRoomItem";
    public static final String MAIN_ADD_FREQUENTLY_DEVICE_ITEM = "mainAddFrequentlyDeviceItem";
    public static final String MAIN_ROOM_LIST_ITEM = "mainRoomListItem";
    public static final String MAIN_ROOM_ADD_SELECTED_DEVICE_MAP = "mainRoomAddSelectedDeviceMap";
    public static final String MAIN_DEVICE_LIST_ITEM = "mainDeviceListItem";
    public static final String MAIN_DEVICE_CURTAINS_ROTATION = "mainDeviceCurtainsRotation";
    public static final String MAIN_ROOM_IMAGE_ROOT_PATH = "/static/img/room/";

    public static final String VAL_INTENT_DELETE_DEVICE = "deleteDevice";
    public static final String VAL_INTENT_RENAME_DEVICE = "renameDevice";
    public static final String VAL_INTENT_ADD_DEVICE = "addDevice";

    public static final String VAL_INTENT_DELETE_ROOM = "deleteRoom";
    public static final String VAL_INTENT_RENAME_ROOM = "renameRoom";
    public static final String VAL_INTENT_ADD_ROOM = "addRoom";

    public static final String EZ_APP_KEY = "9058b1dfd1f84d8b99d4948da2dff1c0";
    public static final String EZ_OPEN_API_SERVER = "https://open.ys7.com";
    public static final String EZ_OPEN_AUTH_API_SERVER = "https://openauth.ys7.com";

    public static final int TRANSPONDER_REMOTER_CATEGORY_BOX = 1;
    public static final int TRANSPONDER_REMOTER_CATEGORY_TV = 2;
    public static final int TRANSPONDER_REMOTER_CATEGORY_NETWORK_BOX = 3;
    public static final int TRANSPONDER_REMOTER_CATEGORY_AIR_CONDITION = 5;
    public static final int TRANSPONDER_REMOTER_CATEGORY_WIND = 8;
    public static final int TRANSPONDER_REMOTER_CATEGORY_DIY = 100;

    public static final int REQUEST_NO_RESULT_CODE = -1;
    public static final int REQUEST_RESULT_CODE_ERROR = 0x10;
    public static final int REQUEST_RESULT_CODE_ONE = 0x11;
    public static final int REQUEST_RESULT_CODE_TWO = 0x12;
    public static final int REQUEST_RESULT_CODE_THREE = 0x13;
    public static final int REQUEST_RESULT_CODE_FOUR = 0x14;
    public static final int REQUEST_RESULT_CODE_FIVE = 0x15;
    public static final int REQUEST_RESULT_CODE_SIX = 0x16;
    public static final int REQUEST_RESULT_CODE_SEVEN = 0x17;
    public static final int REQUEST_RESULT_CODE_EIGHT = 0x18;
    public static final int REQUEST_RESULT_CODE_NINE = 0x19;
    public static final int REQUEST_RESULT_CODE_TEN = 0x20;
    public static final int REQUEST_RESULT_CODE_ELEVEN = 0x21;
    public static final int REQUEST_RESULT_CODE_TWELVE = 0x22;
    public static final int REQUEST_RESULT_CODE_THIRTEEN = 0x23;
    public static final int REQUEST_RESULT_CODE_TWENTYFOUR = 0x24;

    public static final int MENU_ITEM_ID_ONE = 0x21;
    public static final int MENU_ITEM_ID_TWO = 0x22;
    public static final int MENU_ITEM_ID_THREE = 0x23;
    public static final int MENU_ITEM_ID_FOUR = 0x24;
    public static final int MENU_ITEM_ID_FIVE = 0x25;
//    public static final int MENU_ITEM_ID_SIX = 0x26;
//    public static final int MENU_ITEM_ID_SEVEN = 0x27;

    public static final String TEMP_DATA = "tempData";
    public static final String TEMP_ROOM_DETAIL_FIRST_CAMERA = "tempRoomDetailFirstCamera";
    //------------------------------------------------------


    public static final String UUID_DEFAULT = "99ce29670acc6781b02c4708ed75094c";
    public static final String UUID_MESSAGE = "1f61wge3Jfwe650gwe5Pkw68";
    public static final String URL_BASE = "https://api.smarthome.yn-iot.cn";// SettingUtil.getCurrentServerAddress();
    public static final String VAL_UNKNOWN_ERROR = "未知错误!";
    public static final String VAL_NETWORK_ERROR = "网络错误,请稍后重试!";


    /*键*/
    public static final String KEY_ABOUT_US = "aboutUs";

    public static final String KEY_PLATFORM = "platform";

    public static final String KEY_PLATFORM_ID = "platformId";

    public static final String KEY_APP_VERSION = "appVersion";

    public static final String KEY_IMEI = "imei";

    public static final String KEY_CHANNEL_ID = "channelId";

    public static final String KEY_PHONE_MODEL = "phoneModel";

    public static final String KEY_SYSTEM_VERSION = "systemVersion";

    public static final String KEY_ENCRYPT_CODE = "keyCode";

    public static final String KEY_INSIDE_VERSION = "insideVersion";

    public static final String KEY_USER_AGENT = "userAgent";

    public static final String KEY_LONGITUDE = "latitude";

    public static final String KEY_LATITUDE = "longitude";

    public static final String KEY_LONGITUDE_SHORT = "lat";

    public static final String KEY_LATITUDE_SHORT = "lng";

    public static final String KEY_NETWORD_TYPE = "networkType";

    public static final String KEY_PAGED = "paged";

    public static final String KEY_GROUP_MESSAGE = "messageGroup";

    public static final String KEY_MESSAGE_LIST = "messageList";

    public static final String KEY_DEVICE_LIST = "deviceList";

    public static final String KEY_USER_LIST = "list";

    public static final String KEY_SMS_TYPE = "smsType";

    public static final String KEY_TIPS = "tips";

    public static final String KEY_LOADING_COVER = "loadingCover";
    public static final String KEY_HOST_ID = "securityId";
    public static final String KEY_HOST_NAME = "hostName";
    public static final String KEY_HOST_TYPE = "deviceType";
    public static final String KEY_RETURN_DATA = "returnData";
    public static final String REG_EXP_HOST_ADDRESS = "[0-9a-zA-Z]+";
    public static final String KEY_HOST_STATUS = "hostStatus";
    public static final String KEY_PUSH_HOST_RING = "pushRing";
    public static final String KEY_DEVICE_TYPE = "deviceType";
    public static final String KEY_RESOURCE_TYPE = "resourceType";
    public static final String KEY_RESOURCE_ID = "resourceId";
    public static final String KEY_RESOURCE_URL = "resourceUrl";
    public static final String KEY_FILE_NAME = "fileName";
    public static final String KEY_DEL_USER_ID = "delUserId";
    public static final String KEY_DEVICE_RESP = "deviceResp";
    public static final String KEY_WS_MSG_LOWER = "msg";
    public static final String KEY_WS_MANUFACTURER = "manufacturer";
    public static final String KEY_WS_CMD = "cmd";
    public static final String KEY_WS_DATA_TYPE_UPPER = "DataType";
    public static final String KEY_WS_DEVICE_TYP_UPPER = "DeviceType";
    public static final String KEY_WS_DEVICE_ID_UPPER = "DeviceId";
    public static final String KEY_WS_MSG_UPPER = "Msg";
    public static final String KEY_WS_DATA_UPPER = "Data";
    public static final String KEY_WS_DATA_LOWER = "data";
    public static final String KEY_WS_ENDPOINT_ID_UPPER = "EndPointID";
    public static final String KEY_WS_ADDRESS_LOWER = "address";
    public static final String KEY_WS_ADDRESS_UPPER = "Address";
    public static final String KEY_WS_CLIENT_ID = "client_Id";
    public static final String KEY_WS_CODE_LOWER = "code";
    public static final String KEY_NAME = "name";
    public static final String KEY_DEVICE = "device";
    public static final String KEY_TIME_ROW_ID = "timerRowid";
    public static final String KEY_SECURITY_ID = "securityId";
    public static final String KEY_TIMER = "timer";
    public static final String KEY_EXECUTE_STATUS = "executeStatus";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_DEVICE_SERIAL = "deviceSerial";
    public static final String KEY_UMENG_PUSH_DEVICE_TOKEN = "umengPushDeviceToken";
    public static final String VAL_UMENG_USER_ALIAS_TYPE_DEFAULT = "友盟推送用户唯一标记";
    public static final String PREFIX_UMENG_USER_ALIAS = "umeng_alias_user_id_";
    public static final String PREFIX_ALI_USER_ACCOUNT = "ali_account_user_id_";
    public static final String KEY_PUSH_CMD_TYPE = "cmdType";
    public static final String VAL_PUSH_CMD_TYPE_REQUEST = "request";
    public static final String VAL_PUSH_CMD_TYPE_CANCEL = "cancel";
    public static final String VAL_PUSH_CMD_TYPE_HANGUP = "hangUp";
    public static final String PREFIX_SECURITY_MODES = "security_modes_";
    public static final String KEY_YEAR = "year";
    public static final String KEY_INFO = "info";
    public static final String KEY_TIMER_ROW_ID = "timerRowid";
    public static final String KEY_CURRENT_HOST_ADDRESS = "currentHostAddress";
    public static final String KEY_DEVICE_SITUATION = "situation";
    public static final String KEY_DEVICE_CATEGORY = "category";
    public static final String VAL_DEVICE_CATEGORY_SECURITY = "security";
    public static final String KEY_SECURITY_INFO = "securityInfo";
    public static final String KEY_IS_NOTIFY_SECURITY_WARNING = "KEY_IS_NOTIFY_SECURITY_WARNING";
    public static final String KEY_MAIN_ACTIVITY_TAB_INDEX = "KEY_MAIN_ACTIVITY_TAB_INDEX";
    public static final String KEY_WARNING_NOTIFICATION_CLICK = "KEY_WARNING_NOTIFICATION_CLICK";
    public static final int VAL_NOTIFICATION_CLICK_DEFAULT = 1;
    public static final int VAL_NOTIFICATION_CANCELED_DEFAULT = 2;

    public static final String KEY_TASK_ID = "KEY_TASK_ID";
    public static final String KEY_CACHE_MAIN_TAB_INDEX = "KEY_CACHE_MAIN_TAB_INDEX";
    public static final String KEY_MAIN_TAB_INDEX = "KEY_MAIN_TAB_INDEX";
    public static final CharSequence VAL_TAB_INDEX_CHANNEL_NAME = "VAL_TAB_INDEX_CHANNEL_NAME";
    public static final String KEY_MAIN_TAB_TASK_ID = "KEY_MAIN_TAB_TASK_ID";
    public static final String KEY_HOST_PUSH_RING = "pushRing";
    public static final String KEY_WARNING_NOTIFICATION_MESSAGE = "KEY_WARNING_NOTIFICATION_MESSAGE";
    public static final String KEY_ABOUT_US_CONTENT = "content";
    public static final String KEY_ABOUT_US_TITLE = "title";
    public static final String KEY_APP_NEW_VERSION = "KEY_APP_NEW_VERSION";
    public static final String KEY_DEVICE_SHOW_TYPE = "KEY_DEVICE_SHOW_TYPE";
    public static final String KEY_ROOM_SHOW_TYPE = "KEY_ROOM_SHOW_TYPE";
    public static final String KEY_PUSH_NOTIFICATION = "KEY_PUSH_NOTIFICATION";
    public static final String KEY_SUGGEST_PHONE = "phone";
    public static final String KEY_SUGGEST_TYPE = "type";
    public static final String KEY_SUGGEST_CONTENT = "suggest";
    public static final String KEY_SUGGEST_NAME = "name";
    public static final String KEY_OLD_PASSWORD = "oldPassword";
    public static final String KEY_APP_CONFIG = "KEY_APP_CONFIG";
    public static final String KEY_TITLE = "title";
    public static final String KEY_URL = "url";
    public static final String KEY_COMMON_LOG_TITLE = "KEY_COMMON_LOG_TITLE";
    public static final String KEY_COMMON_LOG_SECOND_TITLE = "KEY_COMMON_LOG_SECOND_TITLE";
    public static final String KEY_COMMON_LOG_TIME = "KEY_COMMON_LOG_TIME";
    public static final String KEY_COMMON_LOG_TITLE_SEPARATOR = "KEY_COMMON_LOG_TITLE_SEPARATOR";
    public static final String KEY_INTENT_ITEMS_TO_DO = "INTENT_ITEMS_TO_DO";
    public static final String VAL_INTENT_HOST_CHANGED = "hostChange";
    public static final int VAL_ERROR_CODE_NEED_LOGIN = 100032;
    public static final String KEY_WARNING_BROADCAST = "KEY_WARNING_BROADCAST";
    public static final String KEY_TRIGGER_STATUS = "status";
    public static final String KEY_TRIGGER_ID = "triggerId";
    public static final String KEY_SDK_TUYA_HOME_ID = "KEY_SDK_TUYA_HOME_ID";
    public static final String KEY_ICON = "icon";
    public static final String KEY_TRIGGER_CONDITION = "condition";
    public static final String KEY_TRIGGER_CONDITION_OR = "conditionOr";
    public static final String KEY_TRIGGER_NAME = "name";
    public static final String KEY_TRIGGER_EXECUTE = "execute";
    public static final String KEY_QRCODE_SCAN_CONTENT = "KEY_QRCODE_SCAN_CONTENT";
    public static final String KEY_DEVICE_NAME = "name";
    public static final String KEY_VALIDATE_CODE = "validateCode";
    public static final String KEY_MANUFACTURER_ID = "manufacturerId";
    public static final String KEY_REFRESH_FLAG = "KEY_REFRESH_FLAG";
    public static final String KEY_SELECTED_DEVICE_LIST = "KEY_SELECTED_DEVICE_LIST";
    public static final String KEY_SENSOR_ID_OR_POSITION = "KEY_SENSOR_ID_OR_POSITION";
    public static final String KEY_SENSOR_DATA = "KEY_SENSOR_DATA";
    public static final String KEY_SELECTED_ITEM_INDEX = "KEY_SELECTED_ITEM_INDEX";
    public static final String KEY_BOTTOM_RADIO_PARAM = "KEY_BOTTOM_RADIO_PARAM";
    public static final String KEY_SENSOR_PARENT_ID_OR_POSITION = "KEY_SENSOR_PARENT_ID_OR_POSITION";
    public static final String KEY_NETWORK_CONFIG_TYPE = "KEY_NETWORK_CONFIG_TYPE";



    /*海康威视SDK配置*/
    /**
     * 平台申请的id
     */
    public static String CLIENT_ID = "589e024be0c94f8b9c22114770ce1614";
    /**
     * 平台申请的secret
     */
    public static String CLIENT_SECRET = "21724b9d91f242da93ad9f4e227a0e18";
    /**
     * 设备序列号
     */
    public static String DEVICE_SERIAL = "234718297";
    /**
     * 设备通道号
     */
    public static int DEVICE_CHANNEL_NO = 1;
    /**
     * 验证码
     */
    public static String VERIFY_CODE = "**填写验证码**";
//loadingCover


    /*值*/
    public static String VAL_UUID_DEFAULT = "99ce29670acc6781b02c4708ed75094c";

    public static final String VAL_UUID_MESSAGE = "1f61wge3Jfwe650gwe5Pkw68";

    public static String URL_BASE_WSS = "wss://api.smarthome.yn-iot.cn/wss";

    public static final Integer VAL_PAGE_SIZE = 20;

    public static final String VAL_APP_INSIDE_VERSION = "20";

    public static final String VAL_SEND_SMS_CODE_TEXT = "发送";

    public static String VAL_ENCRYPT_CODE = null;


}
