package com.zhouyu.service;

import com.spring.BeanPostProcessor;
import com.spring.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

//need to let spring know this class, by scan
@Component
public class ZhouyuBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (beanName.equals("userService")) {
            //because the proxy is based on the interface
            Object proxyInstance = Proxy.newProxyInstance(ZhouyuBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                //when call the test method of userservice, it will call the invoke method
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    //Aspect logic
                    System.out.println("Aspect Logic");
                    return method.invoke(bean, args);
                }
            });
            return proxyInstance;
        }
        return bean;
    }
}
