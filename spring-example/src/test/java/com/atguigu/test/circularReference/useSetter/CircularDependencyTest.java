package com.atguigu.test.circularReference.useSetter;


import com.atguigu.circularReference.useSetter.CircularDependencyA;
import com.atguigu.circularReference.useSetter.CircularDependencyB;
import com.atguigu.circularReference.useSetter.CircularReferenceConfig;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author LiuXianfa
 * @email xianfaliu@newbanker.cn
 * @date 8/16 19:33
 */
public class CircularDependencyTest {


    @Test
    public void test() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(CircularReferenceConfig.class);


        CircularDependencyA a = applicationContext.getBean(CircularDependencyA.class);
        CircularDependencyB b = applicationContext.getBean(CircularDependencyB.class);


        System.out.println(a);
        System.out.println(b);


        System.out.println(a.circB);
        System.out.println(b.circA);


        // fixme:这里为什么没有赋值成功呢?
        Assert.assertNotNull(a.circB);
        Assert.assertNotNull(b.circA);




        applicationContext.close();
    }

}