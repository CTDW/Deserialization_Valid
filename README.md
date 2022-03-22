# Deserialization_Valid
## 一.abstract
  ### 支持反序列化过程中的参数校验
## 二.how to Used
  ### 1.add dependency         
        <dependency>
            <groupId>com.google.auto.service</groupId>
            <artifactId>auto-service</artifactId>
            <version>1.0-rc2</version>
        </dependency>
  ### 2.for example
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
        
