package com.mytest.controller;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author LiuXianfa
 * @email xianfaliu@newbanker.cn
 * @date 2/16 23:34
 */
@Controller
@Order(value = 1)
//@RestController // 这里有个坑:springmvc框架的DispatcherServlet默认使用的是 RequestMappingHandlerMapping ,这个HandlerMapping只会处理带有@Controller注解和@RequestMapping注解的bean.详见:其isHandler()方法
public class HelloController {

    public HelloController() {
        System.out.println("init");
    }

    /**
     * 如果404,请阅读 当前项目下的readme.md
     *
     * @return
     */
    @RequestMapping("/hello")
    @ResponseBody
    public String hello(HttpServletRequest request, HttpServletResponse response, @RequestHeader("Host") String Host) {
        System.out.println(Host);
        return "hello,success!!!";
    }
}