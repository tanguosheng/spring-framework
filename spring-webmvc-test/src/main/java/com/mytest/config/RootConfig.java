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
@EnableWebMvc
public class RootConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MyInterceptor()).addPathPatterns("/*");
    }
}