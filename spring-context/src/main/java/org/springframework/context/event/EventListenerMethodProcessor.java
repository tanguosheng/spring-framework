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

package org.springframework.context.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Registers {@link EventListener} methods as individual {@link ApplicationListener} instances.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.2
 */
public class EventListenerMethodProcessor implements SmartInitializingSingleton, ApplicationContextAware {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ConfigurableApplicationContext applicationContext;

	private final EventExpressionEvaluator evaluator = new EventExpressionEvaluator();

	private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext,
				"ApplicationContext does not implement ConfigurableApplicationContext");
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	private ConfigurableApplicationContext getApplicationContext() {
		Assert.state(this.applicationContext != null, "No ApplicationContext set");
		return this.applicationContext;
	}


	// 此方法说明:(英文说明见接口)
    // 使用 事件监听器创建工厂,把标注 @EventListener注解的方法,创建成 ApplicationListener.
    // 然后使用 context.addApplicationListener(applicationListener); 把 创建的 ApplicationListener 注册到spring应用程序上下文中.
	@Override
	public void afterSingletonsInstantiated() {
	    // 事件监听器创建工厂:可以把标注 @EventListener注解的方法,创建成 ApplicationListener.
		List<EventListenerFactory> factories = getEventListenerFactories();
		ConfigurableApplicationContext context = getApplicationContext();
		String[] beanNames = context.getBeanNamesForType(Object.class);
		for (String beanName : beanNames) {
			if (!ScopedProxyUtils.isScopedTarget(beanName)) {
				Class<?> type = null;
				try {
					type = AutoProxyUtils.determineTargetClass(context.getBeanFactory(), beanName);
				}
				catch (Throwable ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				if (type != null) {
					if (ScopedObject.class.isAssignableFrom(type)) {
						try {
							Class<?> targetClass = AutoProxyUtils.determineTargetClass(
									context.getBeanFactory(), ScopedProxyUtils.getTargetBeanName(beanName));
							if (targetClass != null) {
								type = targetClass;
							}
						}
						catch (Throwable ex) {
							// An invalid scoped proxy arrangement - let's ignore it.
							if (logger.isDebugEnabled()) {
								logger.debug("Could not resolve target bean for scoped proxy '" + beanName + "'", ex);
							}
						}
					}
					try {
					    // 处理bean
						processBean(factories, beanName, type);
					}
					catch (Throwable ex) {
						throw new BeanInitializationException("Failed to process @EventListener " +
								"annotation on bean with name '" + beanName + "'", ex);
					}
				}
			}
		}
	}


	/**
	 * Return the {@link EventListenerFactory} instances to use to handle
	 * {@link EventListener} annotated methods.
	 */
	protected List<EventListenerFactory> getEventListenerFactories() {
		Map<String, EventListenerFactory> beans = getApplicationContext().getBeansOfType(EventListenerFactory.class);
		List<EventListenerFactory> factories = new ArrayList<>(beans.values());
		AnnotationAwareOrderComparator.sort(factories);
		return factories;
	}

	protected void processBean(final List<EventListenerFactory> factories, final String beanName, final Class<?> targetType) {
        // 缓存:没有 @EventListener 注解的类.
        // 在第一次处理的时候,如果类中没有 @EventListener 注解的方法,就把当前这个类缓存起来.再次调用之后,就直接返回了.
		if (this.nonAnnotatedClasses.contains(targetType)) {
            return;
		}


        Map<Method, EventListener> annotatedMethods = null;
        try {
            // 找到带有 @EventListener 注解的方法.
            annotatedMethods = MethodIntrospector.selectMethods(targetType,
                                                                (MethodIntrospector.MetadataLookup<EventListener>) method ->
                                                                        AnnotatedElementUtils.findMergedAnnotation(method, EventListener.class));
        }catch (Throwable ex) {
            // An unresolvable type in a method signature, probably from a lazy bean - let's ignore it.
            if (logger.isDebugEnabled()) {
                logger.debug("Could not resolve methods for bean with name '" + beanName + "'", ex);
            }
        }

        // 如果当前类没有 @EventListener 注解的方法,就缓存到 nonAnnotatedClasses ,下一次再处理此类的时候,就直接返回了.
        if (CollectionUtils.isEmpty(annotatedMethods)) {
            this.nonAnnotatedClasses.add(targetType);
            if (logger.isTraceEnabled()) {
                logger.trace("No @EventListener annotations found on bean class: " + targetType.getName());
            }
        } else {
            // 当前类中存在 @EventListener 注解的方法.就去处理所有标有 @EventListener 注解方法.
            ConfigurableApplicationContext context = getApplicationContext();
            for (Method method : annotatedMethods.keySet()) {
                for (EventListenerFactory factory : factories) {
                    if (factory.supportsMethod(method)) {
                        Method methodToUse = AopUtils.selectInvocableMethod(method, context.getType(beanName));

                        // 把带有 @EventListener 注解方法,使用工厂factory创建成 应用程序监听器(ApplicationListener)
                        ApplicationListener<?> applicationListener = factory.createApplicationListener(beanName, targetType, methodToUse);

                        if (applicationListener instanceof ApplicationListenerMethodAdapter) {
                            ((ApplicationListenerMethodAdapter) applicationListener).init(context, this.evaluator);
                        }

                        // 把 ApplicationListener 注册到spring应用程序上线文中.
                        context.addApplicationListener(applicationListener);

                        // 如果任何一个 事件监听创建工厂(EventListenerFactory) 创建把目标方法创建成ApplicationListener 就break:以便处理下一个 @EventListener方法.
                        break;
                    }
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug(annotatedMethods.size() + " @EventListener methods processed on bean '" + beanName + "': " + annotatedMethods);
            }
        }

	}

}
