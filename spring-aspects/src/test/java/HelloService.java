/**
 * @author LiuXianfa
 * @email xianfaliu@newbanker.cn
 * @date 3/15 22:27
 */
public class HelloService {

    public void hello (){
        System.out.println("hello aspect!");
    }

    public static void main(String[] args) {
        new HelloService().hello();
    }
}