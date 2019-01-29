package br.com.anteros.nosql.spring.web.config;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import br.com.anteros.nosql.spring.web.support.OpenNoSQLSessionInViewFilter;


public abstract class AbstractSpringWebAppInitializer implements WebApplicationInitializer{
	
	private static final String ANTEROS_CORS_FILTER = "anterosCorsFilter";
	private static final String SPRING_SECURITY_FILTER_CHAIN = "springSecurityFilterChain";
	private static final String DISPATCHER = "dispatcher";
	private static final String OPEN_NOSQL_SESSION_IN_VIEW_FILTER = "OpenNoSQLSessionInViewFilter";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();
		appContext.setServletContext(servletContext);
		
		if (registerFirstConfigurationClasses() != null) {
			for (Class<?> clz : registerFirstConfigurationClasses()) {
				appContext.register(clz);
			}
		}
		
		if (persistenceConfigurationClass() != null) {
			appContext.register(persistenceConfigurationClass());
		}
		
		if (mvcConfigurationClass() != null) {
			appContext.register(mvcConfigurationClass());
		}
		
		if (securityConfigurationClass() != null) {
			appContext.register(securityConfigurationClass());
		}
		
		if (globalMethodConfigurationClass() != null) {
			appContext.register(globalMethodConfigurationClass());
		}
		
		if (resourceServerConfigurationClass() != null) {
			appContext.register(resourceServerConfigurationClass());
		}
		
		if (oauth2ServerConfigurationClass() != null) {
			appContext.register(oauth2ServerConfigurationClass());
		}
		
		if (registerLastConfigurationClasses() != null) {
			for (Class<?> clz : registerLastConfigurationClasses()) {
				appContext.register(clz);
			}
		}
		
		appContext.setServletContext(servletContext);
		servletContext.addListener(new ContextLoaderListener(appContext));

		addListener(servletContext);
		
		
		
		addServlet(servletContext, appContext);
		

		
		Dynamic servlet = servletContext.addServlet(DISPATCHER, new DispatcherServlet(appContext));
		servlet.addMapping("/");
		servlet.setLoadOnStartup(1);
		

		FilterRegistration.Dynamic filter = servletContext.addFilter(ANTEROS_CORS_FILTER, AnterosCorsFilter.class);
		filter.addMappingForUrlPatterns(null, false, "/*");

		FilterRegistration.Dynamic springSecurityFilterChain = servletContext.addFilter(SPRING_SECURITY_FILTER_CHAIN,
				DelegatingFilterProxy.class);
		springSecurityFilterChain.addMappingForUrlPatterns(null, false, "/*");
		
		FilterRegistration.Dynamic openSQLSessionInViewFilterChain = servletContext.addFilter(OPEN_NOSQL_SESSION_IN_VIEW_FILTER,
				OpenNoSQLSessionInViewFilter.class);
		openSQLSessionInViewFilterChain.addMappingForUrlPatterns(null, false, "/*");

		
	}
	
	public abstract Class<?> oauth2ServerConfigurationClass();

	public abstract Class<?> resourceServerConfigurationClass();

	public abstract Class<?> globalMethodConfigurationClass();
	
	public abstract Class<?> securityConfigurationClass();

	public abstract Class<?> mvcConfigurationClass();
	
	public abstract Class<?>[] persistenceConfigurationClass();
	
	public abstract Class<?>[] registerFirstConfigurationClasses();

	public abstract Class<?>[] registerLastConfigurationClasses();
	
	public abstract void addListener(ServletContext servletContext);

	public abstract void addServlet(ServletContext servletContext, AnnotationConfigWebApplicationContext appContext);

}
