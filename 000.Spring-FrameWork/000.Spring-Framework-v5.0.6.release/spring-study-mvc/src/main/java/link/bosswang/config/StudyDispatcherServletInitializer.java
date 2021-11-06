package link.bosswang.config;

import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * MVC启动初始化类
 */
public class StudyDispatcherServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
	/**
	 * 提供Root ApplicationContext初始化的配置类
	 *
	 * @return
	 */
	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class<?>[]{ServiceConfig.class};
	}

	/**
	 * 提供Servlet applicationContext初始化的配置类
	 *
	 * @return
	 */
	@Override
	protected Class<?>[] getServletConfigClasses() {
		// 获取程序运行的过程中产生的代理类(分别代表CGLIB代理和JDK动态代理)
		System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY,
		 "/home/wei/workspace/SOURCE_CODE/Spring-Framework-v5.0.6.release/spring-proxy");
		 System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles",
		 "true");
		return new Class<?>[]{MvcConfig.class};
	}

	/**
	 * 配置servlet的处理路径
	 *
	 * @return
	 */
	@Override
	protected String[] getServletMappings() {
		return new String[]{"/"};
	}
}
