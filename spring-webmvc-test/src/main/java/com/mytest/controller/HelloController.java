package com.mytest.controller;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author LiuXianfa
 * @email xianfaliu@newbanker.cn
 * @date 2/16 23:34
 */
@RestController
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
    public String hello(HttpServletRequest request, HttpServletResponse response, @RequestHeader("Host") String Host) {
        System.out.println(Host);
        return "hello,success!!!";
    }
}