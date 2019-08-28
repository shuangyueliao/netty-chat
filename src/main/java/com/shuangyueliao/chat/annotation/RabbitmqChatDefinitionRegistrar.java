package com.shuangyueliao.chat.annotation;

import com.shuangyueliao.chat.queue.rabbitmq.RabbitmqOfflineInfoHelper;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * @author shuangyueliao
 * @create 2019/8/25 0:16
 * @Version 0.1
 */
public class RabbitmqChatDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry){
        Class beanClass = RabbitmqOfflineInfoHelper.class;
        RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
        String beanName = StringUtils.uncapitalize(beanClass.getSimpleName());
        //在这里可以拿到所有注解的信息，可以根据不同注解来返回不同的class,从而达到开启不同功能的目的
        //通过这种方式可以自定义beanName
        registry.registerBeanDefinition(beanName, beanDefinition);
    }
}
