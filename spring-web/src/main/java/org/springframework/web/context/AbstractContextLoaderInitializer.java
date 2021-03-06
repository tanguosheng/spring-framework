/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.lang.Nullable;
import org.springframework.web.WebApplicationInitializer;

/**
 * Convenient base class for {@link WebApplicationInitializer} implementations
 * that register a {@link ContextLoaderListener} in the servlet context.
 *
 * <p>The only method required to be implemented by subclasses is
 * {@link #createRootApplicationContext()}, which gets invoked from
 * {@link #registerContextLoaderListener(ServletContext)}.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.2
 */
public abstract class AbstractContextLoaderInitializer implements WebApplicationInitializer {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		registerContextLoaderListener(servletContext);
	}

	/**
     * 向servlet上线文中注册一个servlet监听器:{@link ContextLoaderListener}.
     * 此监听器中,存在一个spring web应用上线文: {@link WebApplicationContext}
     *
     *
	 * Register a {@link ContextLoaderListener} against the given servlet context.
     * The {@code ContextLoaderListener} is initialized with the application context returned
	 * from the {@link #createRootApplicationContext()} template method.
     *
	 * @param servletContext the servlet context to register the listener against
	 */
	protected void registerContextLoaderListener(ServletContext servletContext) {
        // 1、模板方法:创建spring-web根应用程序上线文:WebApplicationContext.
		WebApplicationContext rootAppContext = createRootApplicationContext();
		if (rootAppContext != null) {
            // 2、[重要]注册用于初始化root ioc容器的servlet应用监听器.(在servlet容器初始化完毕之后,在所有filter、servlet初始化之前,会调用监听器的contextInitialized方法)
            //          此类的contextInitialized方法就是去初始化 root ioc容器.[这对应之前在web.xml中配置的那个监听器]
            ContextLoaderListener listener = new ContextLoaderListener(rootAppContext);
			listener.setContextInitializers(getRootApplicationContextInitializers());
			// 3、把监听器注册到servlet上下文中.
			servletContext.addListener(listener);
		}
		else {
			logger.debug("No ContextLoaderListener registered, as " +
					"createRootApplicationContext() did not return an application context");
		}
	}

	/**
     * 创建要提供给ContextLoaderListener的“根”应用程序上下文。
     * 返回的上下文委派给{@link ContextLoaderListener#ContextLoaderListener(WebApplicationContext)} ，
     * 并将其建立为任何DispatcherServlet应用程序上下文的"父"上下文。 因此，它通常包含中间层服务，数据源等。
     *
     * 图示:<a href="https://docs.spring.io/spring-framework/docs/current/reference/html/images/mvc-context-hierarchy.png">图示</a>
     *
	 * Create the "<strong>root</strong>" application context to be provided to the
	 * {@code ContextLoaderListener}.
	 * <p>The returned context is delegated to
	 * {@link ContextLoaderListener#ContextLoaderListener(WebApplicationContext)} and will
	 * be established as the parent context for any {@code DispatcherServlet} application
	 * contexts. As such, it typically contains middle-tier services, data sources, etc.
	 * @return the root application context, or {@code null} if a root context is not
	 * desired
	 * @see org.springframework.web.servlet.support.AbstractDispatcherServletInitializer
	 */
	@Nullable
	protected abstract WebApplicationContext createRootApplicationContext();

	/**
	 * Specify application context initializers to be applied to the root application
	 * context that the {@code ContextLoaderListener} is being created with.
	 * @since 4.2
	 * @see #createRootApplicationContext()
	 * @see ContextLoaderListener#setContextInitializers
	 */
	@Nullable
	protected ApplicationContextInitializer<?>[] getRootApplicationContextInitializers() {
		return null;
	}

}
