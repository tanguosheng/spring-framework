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

package org.springframework.aop.framework.adapter;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.BeforeAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.util.Assert;

/**
 * Interceptor to wrap am {@link org.springframework.aop.MethodBeforeAdvice}.
 * Used internally by the AOP framework; application developers should not need
 * to use this class directly.
 *
 * @author Rod Johnson
 * @see AfterReturningAdviceInterceptor
 * @see ThrowsAdviceInterceptor
 */
@SuppressWarnings("serial")
public class MethodBeforeAdviceInterceptor implements MethodInterceptor, BeforeAdvice, Serializable {

    /**
     * 前置通知的方法增强器
     */
	private final MethodBeforeAdvice advice;

	/**
	 * Create a new MethodBeforeAdviceInterceptor for the given advice.
	 * @param advice the MethodBeforeAdvice to wrap
	 */
	public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}


	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
        // 当前 MethodBeforeAdviceInterceptor 这个 方法拦截器(MethodInterceptor),用来处理 @Before 注解.
        // 实现:先去调用前置通知,然后再调用目标方法;
        //      在advice.before() 方法中会去调用aop切面中声明的 @Before 方法.

        // 这里先调用 @Before("pointCut()") 指定的方法，
		this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
        // @Before("pointCut()") 指定的方法执行完之后，再执行目标方法。
		return mi.proceed();
	}

}
