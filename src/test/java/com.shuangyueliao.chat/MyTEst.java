package com.shuangyueliao.chat;

import java.util.HashMap;

/**
 * @author shuangyueliao
 * @create 2019/8/14 23:43
 * @Version 0.1
 */
public class MyTEst {
    public static void main(String[] args) {
        HashMap hashMap = new HashMap();
        hashMap.put("1", 2);
        Object o = hashMap.get("1");
        Object remove = hashMap.remove("1");
        System.out.println(o);
        System.out.println(remove);
    }
}
