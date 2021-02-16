package my.test.org.springframework.aop.support;

import org.springframework.aop.support.AopUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * @author LiuXianfa
 * @email xianfaliu@newbanker.cn
 * @date 2/11 0:04
 */
public class AopUtilsTest {

    public static void main(String[] args) throws NoSuchMethodException {
        Method superMethod = Super.class.getMethod("doSomething");
        System.out.println("接口的方法:" + superMethod);
        System.out.println("是否存在注解:" + superMethod.getAnnotation(Anno.class));


        Method mostSpecificMethod = AopUtils.getMostSpecificMethod(superMethod, SubClass.class);

        System.out.println("子类的方法:" + mostSpecificMethod);
        System.out.println("是否存在注解:" + mostSpecificMethod.getAnnotation(Anno.class));
        System.out.println("[结论]可以发现:在父类/接口上的注解是不会被子类继承的!");
    }
}

@Target(value = {ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface Anno {
}

interface Super {
    @Anno
    void doSomething();
}

class SubClass implements Super {

    @Override
    public void doSomething() {
        System.out.println("wo are doing something....");
    }
}