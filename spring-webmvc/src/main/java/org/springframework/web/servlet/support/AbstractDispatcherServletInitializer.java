/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.support;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.AbstractContextLoaderInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FrameworkServlet;

/**
 * Base class for {@link org.springframework.web.WebApplicationInitializer}
 * implementations that register a {@link DispatcherServlet} in the servlet context.
 *
 * <p>Most applications should consider extending the Spring Java config subclass
 * {@link AbstractAnnotationConfigDispatcherServletInitializer}.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.2
 */
public abstract class AbstractDispatcherServletInitializer extends AbstractContextLoaderInitializer {

	/**
	 * The default servlet name. Can be customized by overriding {@link #getServletName}.
	 */
	public static final String DEFAULT_SERVLET_NAME = "dispatcher";


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
        // 1、首先调用super方法:[重要]super方法中会注册一个[上线文加载监听器].
		super.onStartup(servletContext);

        // 2、注册用于派发http请求的servlet
		registerDispatcherServlet(servletContext);

        // @see org.springframework.web.servlet.DispatcherServlet
        //       DispatcherServlet作用:servlet容器接收到的所有http请求,都是此servlet把请求根据@RequestMapping派发到Controller方法的
    }

	/**
	 * Register a {@link DispatcherServlet} against the given servlet context.
	 * <p>This method will create a {@code DispatcherServlet} with the name returned by
	 * {@link #getServletName()}, initializing it with the application context returned
	 * from {@link #createServletApplicationContext()}, and mapping it to the patterns
	 * returned from {@link #getServletMappings()}.
	 * <p>Further customization can be achieved by overriding {@link
	 * #customizeRegistration(ServletRegistration.Dynamic)} or
	 * {@link #createDispatcherServlet(WebApplicationContext)}.
	 * @param servletContext the context to register the servlet against
	 */
	protected void registerDispatcherServlet(ServletContext servletContext) {
		String servletName = getServletName();
		Assert.hasLength(servletName, "getServletName() must not return null or empty");

        // 1、模板方法:创建ioc容器
		WebApplicationContext servletAppContext = createServletApplicationContext();
		Assert.notNull(servletAppContext, "createServletApplicationContext() must not return null");

        // 2、创建 org.springframework.web.servlet.DispatcherServlet
		FrameworkServlet dispatcherServlet = createDispatcherServlet(servletAppContext);
		Assert.notNull(dispatcherServlet, "createDispatcherServlet(WebApplicationContext) must not return null");
        // 3、设置ApplicationContextInitializer todo:???在哪里被调用的?
		dispatcherServlet.setContextInitializers(getServletApplicationContextInitializers());

        // 4、[servlet] 把DispatcherServlet注册到servlet容器中.
		ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, dispatcherServlet);
		if (registration == null) {
			throw new IllegalStateException("Failed to register servlet with name '" + servletName + "'. " +
					"Check if there is another servlet registered under the same name.");
		}

        // 5、对此servlet进行配置:
        registration.addMapping(getServletMappings());// 指定servlet处理的http请求uri.
        registration.setLoadOnStartup(1); // 加载顺序
		registration.setAsyncSupported(isAsyncSupported()); // 是否异步支持

        // 6、获取servlet过滤器并注册到servlet容器中.
		Filter[] filters = getServletFilters();
		if (!ObjectUtils.isEmpty(filters)) {
			for (Filter filter : filters) {
				registerServletFilter(servletContext, filter);
			}
		}

        // 7、子类实现:自定义注册
		customizeRegistration(registration);
	}

	/**
	 * Return the name under which the {@link DispatcherServlet} will be registered.
	 * Defaults to {@link #DEFAULT_SERVLET_NAME}.
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected String getServletName() {
		return DEFAULT_SERVLET_NAME;
	}

	/**
     * <pre>
     * 创建要提供给DispatcherServlet的Servlet应用程序上下文。
     * 返回的上下文委托给Spring的{@link DispatcherServlet#DispatcherServlet(WebApplicationContext)} 。
     * 因此，它通常包含控制器，视图解析器，语言环境解析器和其他与`Web相关的bean`。(而需要在多个servlet共享的bean都放到根ioc容器中,比如与数据源相关的bean、业务bean等)
     *
     *
     * 文档地址: <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-servlet-context-hierarchy">文档地址</a>
     * 注释:对于许多应用程序而言，拥有一个WebApplicationContext简单且足够。
     *      但是spring也支持具有父子结构的ioc容器.
     *      其中 根WebApplicationContext在多个DispatcherServlet（或其他Servlet）实例之间共享;而每个servlet实例都有其自己的子WebApplicationContext.
     *
     *      根WebApplicationContext通常包含基础结构bean. (例如需要在多个Servlet实例之间共享的数据存储库和业务服务。)
     *      这些需要被共享的Bean被有效地继承(因为子容器中getBean获取不到,就会委派给父容器).
     *      并且可以在Servlet特定的子级中重写（即重新声明），该子级WebApplicationContext通常包含本地的Bean Servlet。(比如controller、view、HandlerMapping等)
     *
     * 图示:<a href="https://docs.spring.io/spring-framework/docs/current/reference/html/images/mvc-context-hierarchy.png">图示</a>
     * </pre>
     *
	 * Create a servlet application context to be provided to the {@code DispatcherServlet}.
	 * <p>The returned context is delegated to Spring's
	 * {@link DispatcherServlet#DispatcherServlet(WebApplicationContext)}. As such,
	 * it typically contains controllers, view resolvers, locale resolvers, and other
	 * web-related beans.
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected abstract WebApplicationContext createServletApplicationContext();

	/**
	 * Create a {@link DispatcherServlet} (or other kind of {@link FrameworkServlet}-derived
	 * dispatcher) with the specified {@link WebApplicationContext}.
	 * <p>Note: This allows for any {@link FrameworkServlet} subclass as of 4.2.3.
	 * Previously, it insisted on returning a {@link DispatcherServlet} or subclass thereof.
	 */
	protected FrameworkServlet createDispatcherServlet(WebApplicationContext servletAppContext) {
		return new DispatcherServlet(servletAppContext);
	}

	/**
	 * Specify application context initializers to be applied to the servlet-specific
	 * application context that the {@code DispatcherServlet} is being created with.
	 * @since 4.2
	 * @see #createServletApplicationContext()
	 * @see DispatcherServlet#setContextInitializers
	 * @see #getRootApplicationContextInitializers()
	 */
	@Nullable
	protected ApplicationContextInitializer<?>[] getServletApplicationContextInitializers() {
		return null;
	}

	/**
	 * Specify the servlet mapping(s) for the {@code DispatcherServlet} &mdash;
	 * for example {@code "/"}, {@code "/app"}, etc.
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected abstract String[] getServletMappings();

	/**
	 * Specify filters to add and map to the {@code DispatcherServlet}.
	 * @return an array of filters or {@code null}
	 * @see #registerServletFilter(ServletContext, Filter)
	 */
	@Nullable
	protected Filter[] getServletFilters() {
		return null;
	}

	/**
	 * Add the given filter to the ServletContext and map it to the
	 * {@code DispatcherServlet} as follows:
	 * <ul>
	 * <li>a default filter name is chosen based on its concrete type
	 * <li>the {@code asyncSupported} flag is set depending on the
	 * return value of {@link #isAsyncSupported() asyncSupported}
	 * <li>a filter mapping is created with dispatcher types {@code REQUEST},
	 * {@code FORWARD}, {@code INCLUDE}, and conditionally {@code ASYNC} depending
	 * on the return value of {@link #isAsyncSupported() asyncSupported}
	 * </ul>
	 * <p>If the above defaults are not suitable or insufficient, override this
	 * method and register filters directly with the {@code ServletContext}.
	 * @param servletContext the servlet context to register filters with
	 * @param filter the filter to be registered
	 * @return the filter registration
	 */
	protected FilterRegistration.Dynamic registerServletFilter(ServletContext servletContext, Filter filter) {
		String filterName = Conventions.getVariableName(filter);
		Dynamic registration = servletContext.addFilter(filterName, filter);

		if (registration == null) {
			int counter = 0;
			while (registration == null) {
				if (counter == 100) {
					throw new IllegalStateException("Failed to register filter with name '" + filterName + "'. " +
							"Check if there is another filter registered under the same name.");
				}
				registration = servletContext.addFilter(filterName + "#" + counter, filter);
				counter++;
			}
		}

		registration.setAsyncSupported(isAsyncSupported());
		registration.addMappingForServletNames(getDispatcherTypes(), false, getServletName());
		return registration;
	}

	private EnumSet<DispatcherType> getDispatcherTypes() {
		return (isAsyncSupported() ?
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC) :
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE));
	}

	/**
	 * A single place to control the {@code asyncSupported} flag for the
	 * {@code DispatcherServlet} and all filters added via {@link #getServletFilters()}.
	 * <p>The default value is "true".
	 */
	protected boolean isAsyncSupported() {
		return true;
	}

	/**
	 * Optionally perform further registration customization once
	 * {@link #registerDispatcherServlet(ServletContext)} has completed.
	 * @param registration the {@code DispatcherServlet} registration to be customized
	 * @see #registerDispatcherServlet(ServletContext)
	 */
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
	}

}
