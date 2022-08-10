# ApplicationContext
## Spring 容器功能
### 组件扫描
+ 自动发现应用容器中需要创建的Bean
### 自动装配
+ 自动满足Bean之间的依赖

## 常用的容器
1. FileSystemXmlApplicationContext:从文件系统加载配置
2. ClassPathXmlApplicationContext: 从classpath加载配置
3. XmlWebApplicationContext: 用于web应用程序的容器
4. AnnotationConfigServletWebServerApplicationContext: 在Spring Boot的boot模块下(基于注解)
5. AnnotationConfigReactiveWebServerApplicationContext: 在Spring Boot的boot模块下(基于注解)
6. AnnotationConfigApplicationContext:适用于普通的非Web应用(基于注解)
7. 以上容器的共性：
    1. 都会调用refresh方法，该方法大致功能如下:
         - 容器初始化、配置解析
         - BeanFactoryPostProcessor和BeanPostProcessor的注册和激活
         - 国际化配置
         - 其他

 ## 重要的类
 + org.springframework.context.support.AbstractApplicationContext 该抽象类是Spring中最高级的部分，定义了Spring中简单不易动的部分，使用了模版方法模式.