package com.mytest.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * @author LiuXianfa
 * @email xianfaliu@newbanker.cn
 * @date 2/16 23:36
 */
public class MyAnnotationConfigDispatcherServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return null;
        // return new Class[]{RootConfig.class};  // fixme:使用这配置,会报错 Caused by: java.lang.ClassNotFoundException: org.springframework.cglib.core.NamingPolicy
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{RootConfig.class}; // 这地方泛型需要写上,虽然运行时不会有问题,但是在gradle打包的时候会报错.奇葩.
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

}

/*
这里有个坑:
    这里如果让 getRootConfigClasses()方法和getServletConfigClasses()方法的返回值互换,然后用tomcat启动之后,访问 http://localhost:8080/hello 就会404   !!!!

为什么我会跳进这个坑里:
    因为我看到 AbstractAnnotationConfigDispatcherServletInitializer#getServletConfigClasses() 方法注释:
    @return the configuration for the Servlet application context, or {@code null} if all configuration is specified through root config classes.
    上面意思是:
    返回值：Servlet应用程序上下文的配置；如果所有配置都是通过root config类指定的，则为null。

debug时分析过程:
    404时,日志显示:
    此时在 DispatcherServlet#getHandler(request) 方法返回值(方法处理器)就会为空.所以才会打印上面日志.
    stepInto点进去发现: AbstractHandlerMethodMapping.getHandlerInternal 返回null.猜测这里返回null,是因为在容器启动的时候,就没有向map中注册对应的controller.那找一下是在哪里注册的呢?
    是在:AbstractHandlerMethodMapping#afterPropertiesSet() 方法中调用 initHandlerMethods()方法 注册的.
    然后发现在注册的时候,beanNames变量中并没有我们需要的helloController这个beanName.但是 HelloController 构造方法也打印了.

    仔细分析才发现,在 RequestMappingHandlerMapping#obtainApplicationContext()方法返回的ioc容器为
    createServletApplicationContext() 方法创建的ioc容器.而且其getParent()并不是 createRootApplicationContext() 方法创建的ioc容器.
    所以beanNames变量中并没有我们需要的helloController这个beanName.
    所以在请求  http://localhost:8080/hello 就会404

@see org.springframework.context.ApplicationContext#getParent
 */