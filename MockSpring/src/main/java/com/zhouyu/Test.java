package com.zhouyu;

import com.spring.ZhouyuApplicationContext;
import com.zhouyu.service.UserInterface;
import com.zhouyu.service.UserService;

import java.lang.reflect.InvocationTargetException;

public class Test {
    public static void main(String[] args) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        //scan -> create singleton bean
        ZhouyuApplicationContext applicationContext = new ZhouyuApplicationContext(AppConfig.class);
        //because it returns a proxy which proxed UserInterface
        UserInterface userService = (UserInterface) applicationContext.getBean("userService");
        userService.test();
        System.out.println(applicationContext.getBean("userService"));
        System.out.println(applicationContext.getBean("userService"));
        System.out.println(applicationContext.getBean("orderService"));
    }
}
