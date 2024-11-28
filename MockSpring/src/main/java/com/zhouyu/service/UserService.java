package com.zhouyu.service;

import com.spring.*;

@Component("userService")
@Scope("prototype")
public class UserService implements InitializingBean, UserInterface, BeanNameAware {
    @Autowired
    private OrderService orderService;
    @ZhouyuValue("xxx")
    private String test;
    private String beanName;
    public void test() {
        System.out.println(orderService);
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("initialize");
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }
}
