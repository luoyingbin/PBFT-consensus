package com.common;

public class Constants {
    public static final String USER = "App";


    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";


    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 验证码 redis key
     */
    public static final String UNLOCK_IMAGE_KEY = "unlock_image_key:";

    /**
     * 验证码 redis key
     */
    public static final String UNLOCK_LOGIN_KEY = "unlock_login_key:";


    /**
     * 通用成功标识
     */
    public static final String SUCCESS = "0";

    /**
     * 通用失败标识
     */
    public static final String FAIL = "1";


    /**
     * 令牌
     */
    public static final String TOKEN = "token";
    /**
     * 令牌前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";
    /**
     * 令牌前缀
     */
    public static final String LOGIN_USER_KEY = "login_user_key";
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";
    /**
     * 短信验证码 redis key
     */
    public static final String SMS_CODE_KEY = "sms_code:";
    /**
     * 邮箱验证码 redis key
     */
    public static final String EMAIL_CODE_KEY = "email_code:";
    /**
     * 用户鉴权的 redis key
     */
    public static final String USER_KEY = "user_key:";
    /**
     * 终端管理的 redis key
     */
    public static final String TERMINAL_MANAGER_KEY = "terminal_manager_key:";

    /**
     * 指纹ID前缀
     */
    public static final String FINGERPRINT_PREFIX = "dc";

    public static final long FINGERPRINT_LENGTH = 34;

    public static final String FINGERPRINT_LOGIN_FREQUENCY_PREFIX = "LoginFrequency:";

    public static final String DEFAULT_FINGERPRINT = "zr98cb01b24e89c7f22934a56b052aa2dc";

    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "/profile";


    public static final long SECOND_MILLIS = 1000;

    public static final long MINUTE_MILLIS = 60 * SECOND_MILLIS;

    public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    public static final Long A_DAY = 86400000L;

    public static final int WEEK = 7;
    public static final int VARCHAR_LENGTH_16 = 16;
    public static final int VARCHAR_LENGTH_50 = 50;
    public static final int VARCHAR_LENGTH_128 = 128;
    public static final int VARCHAR_LENGTH_255 = 255;
    public static final int VARCHAR_LENGTH_512 = 512;
    public static final int VARCHAR_LENGTH_1024 = 1024;
    public static final int VARCHAR_LENGTH_2048 = 2048;

    public static final byte PERSONAL = 0;
    public static final byte ENTERPRISE = 1;
}
