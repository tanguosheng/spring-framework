/**
 * 1、执行需要先配置aspectj
 * 2、然后使用ajc.exe编译:
 * >ajc HelloService.java TxAspect.aj
 *
 * 3、然后使用java命令执行:
 * >java HelloService
 * before
 * hello aspect!
 * after
 *
 *
 * @author LiuXianfa
 * @email xianfaliu@newbanker.cn
 * @date 3/15 22:29
 */
public aspect TxAspect {
    void around():call(void hello()){
        System.out.println("before");
        proceed();
        System.out.println("after");
    }
}