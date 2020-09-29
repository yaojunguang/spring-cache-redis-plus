package com.smarthito.cache.redis.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * @author yaojunguang
 * 一些方法的对象
 * Created by yaojunguang on 15/5/28.
 */
public class StringUtil {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

    /**
     * 分隔符
     */
    public final static String SEP = ";";


    /**
     * 生成易识别的字符串码
     *
     * @param length 字符串
     * @return 结果集
     */
    public static String generateCode(int length) {
        String str = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    /**
     * 转化为金额的格式
     *
     * @param money 钱数
     * @return 结果
     */
    public static String toMoney(Double money) {
        if (money % 1 == 0) {
            return String.format("%.0f", money);
        } else if (money % 0.1 == 0) {
            return String.format("%.1f", money);
        } else {
            return String.format("%.2f", money);
        }
    }

    /**
     * 转化为金额的格式
     * <p>
     * =======
     * * @param num 钱数
     * * @return 结果
     */

    public static String intToStr(Integer num) {
        return String.format("%d", num);
    }

    /**
     * @param url 钱数
     * @return 结果
     */
    public static String urlEncode(String url) {
        try {
            return URLEncoder.encode(url, "utf-8");
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        return url;
    }


    /**
     * 生成随机字符串
     *
     * @param length 字符串长度
     * @return 字符串
     */
    public static String randomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    /**
     * 生成随机数字字符串
     *
     * @return 字符串
     */
    public static String randomNumberCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(10);
            sb.append(number);
        }
        return sb.toString();
    }

    /**
     * UUID 去-
     *
     * @return 结果
     */
    public static String uuid() {
        String s = UUID.randomUUID().toString();
        return s.replace("-", "");
    }

    /**
     * 描述: 去掉“-”的uuid
     *
     * @param:
     * @return:
     * @auther: yangmingtian
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * MD5加码。32位
     *
     * @param inStr 输入的字符串
     * @return 加密结果
     */
    public static String md5(String inStr) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }
        char[] charArray = inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }

        byte[] md5Bytes = md5.digest(byteArray);

        StringBuilder hexValue = new StringBuilder();

        for (byte md5Byte : md5Bytes) {
            int val = ((int) md5Byte) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }

        return hexValue.toString();
    }

    /**
     * 随机一组数据
     *
     * @param from 从
     * @param less 到
     * @return 结果
     */
    public static Integer random(Integer from, Integer less) {
        return (int) (Math.random() * (less - from) + from);
    }

    /**
     * 计算出新的CRC32的值
     *
     * @param code 需要处理的字符串
     * @return 结果信息
     */
    public static Long CRC32(String code) {
        CRC32 crc = new CRC32();
        crc.update(code.getBytes());
        return crc.getValue();
    }

    public static boolean notBlank(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * 是否为空或者null
     *
     * @param value 查看值
     * @return 结果
     */
    public static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    /**
     * 手机号验证
     * 中国大陆：开头1号段，后边跟10位数字
     * 台湾：09开头后面跟8位数字
     * 香港：9或6开头后面跟7位数字
     * 澳门：66或68开头后面跟5位数字
     *
     * @return boolean
     * @author yangmingtian
     * @params [countyCode, telephone]
     */
    public static boolean validateTelephone(String telephone) {
        String regex = "^[1]\\d{10}$|^([6|9])\\d{7}$|^[0][9]\\d{8}$|^[6]([8|6])\\d{5}$";
        Pattern compile = Pattern.compile(regex);
        return compile.matcher(telephone).matches();
    }
}
