package com.atguigu.test.cglibDynamicProxy;

import org.springframework.aop.framework.AopProxy;
import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.io.File;
import java.lang.reflect.Method;

/**
 * <pre>
 * 测试&学习使用cglib实现动态代理
 * 1、使用cglib动态代理原理:
 *    使用ASM底层字节码操作类库,动态生成的代理对象为目标对象的子类.
 *    在子类中的所有方法中,都调用 {@link MethodInterceptor} 方法.
 *    所以 无法对final类和final方法进行增强. 备注:{@link Enhancer#generateClass}中判断了superClass如果是final修饰,就抛出异常.
 * 2、优点:
 *      无需要求被代理类实现接口.对代码侵入性较低.
 * 3、缺点:
 *      需要引入单独的cglib包和asm包等.作者只有一个人.后续版本升级维护等方面,可能没有jdk原生动态代理有优势.
 *      无法对final类和final方法进行增强.(因为是生成的子类,final类和final方法都不能被重写.)
 * 4、性能:
 *      据说和jdk8的动态代理对比,性能方面已经差不多了.
 * 5、视频地址:https://www.bilibili.com/video/BV1SJ411v7fq
 * 6、执行此代码之后,会在[项目根目录 + /com/atguigu/test/cglibDynamicProxy/] 目录下生成代理类class文件.
 * 7、从动态生成的代理类class文件可见,代理方法实现为:
 * {@code
 *     public final void login(String var1, String var2) {
 *         MethodInterceptor var10000 = this.CGLIB$CALLBACK_0;
 *         if (var10000 == null) {
 *             CGLIB$BIND_CALLBACKS(this);
 *             var10000 = this.CGLIB$CALLBACK_0;
 *         }
 *
 *         if (var10000 != null) {     // 如果能获取到 MethodInterceptor ,就调用其拦截方法.(拦截方法中定义了前后置等增强处理.)
 *             var10000.intercept(this, CGLIB$login$0$Method, new Object[]{var1, var2}, CGLIB$login$0$Proxy);
 *         } else {                    // 否则就直接调用目标方法(即父类中的方法).
 *             super.login(var1, var2);
 *         }
 *     }
 * }
 * </pre>
 *
 * @author LiuXianfa
 * @email xianfaliu@newbanker.cn
 * @date 2/10 14:58
 */
public class MyCglibDynamicProxyTest implements AopProxy, MethodInterceptor {
    public static void main(String[] args) {
        String projectRootPath = new File("").getAbsolutePath();
        System.out.println(String.format("获取当前项目根目录:[%s]", projectRootPath));

        // 开启代理类debug模式:把生成的代理类class文件输出到指定文件夹中.
        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, projectRootPath);


        MyCglibDynamicProxyTest myCglibDynamicProxyTest = new MyCglibDynamicProxyTest(new LoginService());

        Object proxy = myCglibDynamicProxyTest.getProxy();
        ((LoginService) proxy).login("lxf", "asdf");
    }

    private Object targetObject;

    public MyCglibDynamicProxyTest(Object targetObject) {
        this.targetObject = targetObject;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        Object result;
        try {
            System.out.println("@Before");
            result = methodProxy.invokeSuper(o, args);// 调用父类方法.因为cglib实现原理就是动态生成一个目标对象的子类.
            System.out.println("@AfterReturning");
        } catch (Exception e) {
            System.out.println("@AfterThrwoing");
            throw e;
        } finally {
            System.out.println("@After");
        }
        return result;
    }

    @Override
    public Object getProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetObject.getClass());
        enhancer.setCallback(this);
        return enhancer.create();
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetObject.getClass());
        enhancer.setCallback(this);
        enhancer.setClassLoader(classLoader);
        return enhancer.create();
    }

    // ====== getter/setter ======
    public Object getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(Object targetObject) {
        this.targetObject = targetObject;
    }
}


class LoginService {
    public void login(String userName, String pwd) {
        System.out.println("当前登录用户名:" + userName);
    }
}