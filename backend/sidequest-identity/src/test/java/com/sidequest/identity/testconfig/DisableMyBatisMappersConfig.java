package com.sidequest.identity.testconfig;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class DisableMyBatisMappersConfig {

    @Bean
    public static BeanFactoryPostProcessor removeMyBatisMapperBeans() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                for (String name : beanFactory.getBeanDefinitionNames()) {
                    BeanDefinition def = beanFactory.getBeanDefinition(name);
                    String className = def.getBeanClassName();
                    if (className != null && className.contains("org.mybatis.spring.mapper.MapperFactoryBean")) {
                        if (beanFactory instanceof DefaultListableBeanFactory dlbf) {
                            dlbf.removeBeanDefinition(name);
                        }
                    }
                }
            }
        };
    }
}
