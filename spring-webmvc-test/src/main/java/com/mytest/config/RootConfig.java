package com.mytest.config;

import com.mytest.interceptor.MyInterceptor;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author LiuXianfa
 * @email xianfaliu@newbanker.cn
 * @date 2/16 23:38
 */
@ComponentScan(basePackages = "com.mytest")
@Configuration
@EnableWebMvc // fixme:疑问:为什么加上这个注解之后,就不会执行 org.springframework.context.support.AbstractApplicationContext.refresh 方法了?去掉之后,就会执行.
public class RootConfig {
}

/*

implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // fixme:这里不执行.导致:无法注册拦截器????
        //  发现是 @EnableWebMvc 中 @Import(DelegatingWebMvcConfiguration.class) 的 DelegatingWebMvcConfiguration 使用	@Autowired(required = false) 不生效导致的.
        //  org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.populateBean 方法中:
        //  if (hasInstAwareBpps || needsDepCheck) 判断为false,所以无法使用后置处理器.
        //  发现beanFactory是 DefaultListableBeanFactory 而不是 AbstractApplicationContext???为啥呢?
        registry.addInterceptor(new MyInterceptor()).addPathPatterns("/*");
    }
}


 */
