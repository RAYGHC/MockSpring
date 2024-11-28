package com.spring;

import com.zhouyu.AppConfig;

import java.beans.Introspector;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class ZhouyuApplicationContext {

    private Class configClass;
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private Map<String, Object> singletonObjects = new HashMap<>();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public ZhouyuApplicationContext(Class configClass) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        this.configClass = configClass;

        scan(configClass);
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if (beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        }
    }
    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getType();
        //if you don't pass any param then it will use the default constructor
        Object instance = null;
        try {
            instance = clazz.getConstructor().newInstance();
            //Dependency Injection
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    field.set(instance, getBean(field.getName()));
                }
            }
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware)instance).setBeanName(beanName);
            }
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                //it can help us to create AOP proxy
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }
            if (instance instanceof InitializingBean) {
                //need two parentheses here, first is for the type transform, second is to make sure the type transform happens before the method call
                ((InitializingBean) instance).afterPropertiesSet();
            }
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                //it can help us to create AOP proxy
                 instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }
            //BeanPostProcessor
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return instance;
    }



    public Object getBean(String beanName) {
        if (!beanDefinitionMap.containsKey(beanName)) {
            throw new NullPointerException();
        }
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition.getScope().equals("singleton")) {
            Object singletonBean = singletonObjects.get(beanName);
            //for dependency injection
            if (Objects.isNull(singletonBean)) {
                singletonBean = singletonObjects.put(beanName, createBean(beanName, beanDefinition));
            }
            return singletonBean;
        } else {
            //prototype
            Object prototypeBean = createBean(beanName, beanDefinition);
            return prototypeBean;
        }
    }

    private void scan(Class configClass) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        //scan
        //need to check if there is an annotation of ComponentScan
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            //need to get the path to scan
            ComponentScan componentScan = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = componentScan.value();
            path = path.replace('.', '/'); //    com/zhouyu/service this is a relative path
            System.out.println(path);
            //after getting the path, we need to find .class file but .java file, .class file is located at target package with already got compiled
            //this is because spring will scan during the program running time,it's better for decoupling from source code, and improve the performance
            //here we need to understand how classloader works, we have three classloaders
            //bootStrap --> jre/lib
            //Ext --> jre/ext/lib
            //App --> all the rest
            ClassLoader classLoader = ZhouyuApplicationContext.class.getClassLoader();
            URL resource = classLoader.getResource(path);//this relative path is relative to the app classloader
            //we can wrap this url to file,here the file can represent a directory or a file
            File file = new File(resource.getFile());

            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    String absolutePath = f.getAbsolutePath();
                    absolutePath = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
                    absolutePath = absolutePath.replace('/', '.');

                    //even though we can directly see if there is annotation in the file, but its better to load the class and see if there is annotation
                    //so we still need to use classloader
                    Class<?> clazz = classLoader.loadClass(absolutePath);
                    if (clazz.isAnnotationPresent(Component.class)) {
                        //make sure that class implements BeanPostProcessor interface, so we don't need to care what exact clazz is
                        //we cant use instanceof here because clazz is a class object not an object
                        if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                            BeanPostProcessor instance = (BeanPostProcessor) clazz.getConstructor().newInstance();
                            beanPostProcessorList.add(instance);
                        }
                        Component componentAnnotation = clazz.getAnnotation(Component.class);
                        String beanName = componentAnnotation.value();
                        if ("".equals(beanName)) {
                            beanName = Introspector.decapitalize(clazz.getSimpleName());
                        }
                        //to avoid duplicate work, we can record the bean attributes
                        BeanDefinition beanDefinition = new BeanDefinition();
                        beanDefinition.setType(clazz);
                        //if there exists @Scope
                        if (clazz.isAnnotationPresent(Scope.class)) {
                            Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
                            String value = scopeAnnotation.value();
                            //value perhaps singleton or prototype
                            beanDefinition.setScope(value);
                        } else {
                            beanDefinition.setScope("singleton");
                        }
                        beanDefinitionMap.put(beanName, beanDefinition);
                    }
                }
            }
        }
    }
}
