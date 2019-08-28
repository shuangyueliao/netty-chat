package com.shuangyueliao.chat.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author shuangyueliao
 * @create 2019/8/25 0:05
 * @Version 0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RabbitmqChatDefinitionRegistrar.class)
public @interface EnableRabbitmqChat {
}
