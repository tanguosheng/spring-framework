package com.atguigu.test.cglib;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @author LiuXianfa
 * @email xianfaliu@newbanker.cn
 * @date 2/8 16:13
 */
public class CglibTest {

    public void run() {
        System.out.println("这是run方法,执行业务逻辑.");
    }


    public static void main(String[] args) {

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(CglibTest.class);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {

                long l = System.currentTimeMillis();
                System.out.println("before method run...");
                Object o1 = methodProxy.invokeSuper(obj, args);
                System.out.println("after method run...");

                System.out.println("方法执行返回值为:" + o1);
                System.out.println(String.format("执行方法所用时间为:%s毫秒", (System.currentTimeMillis() - l)));
                return o1;
            }
        });

        CglibTest cglibTest = (CglibTest) enhancer.create();
        cglibTest.run();
    }
}
