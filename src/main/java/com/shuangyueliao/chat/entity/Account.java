package com.shuangyueliao.chat.entity;

import lombok.Data;

/**
 * @author shuangyueliao
 * @create 2019/8/14 0:35
 * @Version 0.1
 */
@Data
public class Account {
    protected Integer id;
    protected String username;
    protected String password;
    /**
     * 群号
     */
    protected String groupNumber;
}
