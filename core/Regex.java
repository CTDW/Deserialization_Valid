package com.seeease.common.plugin.serialValid;

/**
 * For example:
 * public class Test {
 *
 *     @Regex(value = "^1[3-9]\\d{9}$",message = "sdf")
 *     public String ser;
 *
 *     public static void main(String[] args) {
 *         Test test = new Test();
 *         String s = "{ser:\"123\"}";
 *         Test test1 = JSONObject.parseObject(s, Test.class);
 *
 *     }
 *
 * }
 */
public @interface Regex {
    String message() default "";
    String value();
}
