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

package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareParents;
import org.aspectj.lang.annotation.Pointcut;

import org.springframework.aop.Advisor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJAfterAdvice;
import org.springframework.aop.aspectj.AspectJAfterReturningAdvice;
import org.springframework.aop.aspectj.AspectJAfterThrowingAdvice;
import org.springframework.aop.aspectj.AspectJAroundAdvice;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJMethodBeforeAdvice;
import org.springframework.aop.aspectj.DeclareParentsAdvisor;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConvertingComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.comparator.InstanceComparator;

/**
 * Factory that can create Spring AOP Advisors given AspectJ classes from
 * classes honoring the AspectJ 5 annotation syntax, using reflection to
 * invoke the corresponding advice methods.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Phillip Webb
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory implements Serializable {

    /**
     * 切面类里的增强方法的排序规则：
     * 1、先根据增强方法上的注解排序(Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class顺序)
     * 2、再根据 方法名称排序
     */
	private static final Comparator<Method> METHOD_COMPARATOR;

	static {
		Comparator<Method> adviceKindComparator = new ConvertingComparator<>(
				new InstanceComparator<>(
						Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
				(Converter<Method, Annotation>) method -> {
					AspectJAnnotation<?> annotation =
						AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
					return (annotation != null ? annotation.getAnnotation() : null);
				});
		Comparator<Method> methodNameComparator = new ConvertingComparator<>(Method::getName);
		METHOD_COMPARATOR = adviceKindComparator.thenComparing(methodNameComparator);
	}


	@Nullable
	private final BeanFactory beanFactory;


	/**
	 * Create a new {@code ReflectiveAspectJAdvisorFactory}.
	 */
	public ReflectiveAspectJAdvisorFactory() {
		this(null);
	}

	/**
	 * Create a new {@code ReflectiveAspectJAdvisorFactory}, propagating the given
	 * {@link BeanFactory} to the created {@link AspectJExpressionPointcut} instances,
	 * for bean pointcut handling as well as consistent {@link ClassLoader} resolution.
	 * @param beanFactory the BeanFactory to propagate (may be {@code null}}
	 * @since 4.3.6
	 * @see AspectJExpressionPointcut#setBeanFactory
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBeanClassLoader()
	 */
	public ReflectiveAspectJAdvisorFactory(@Nullable BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
		// 获取标记 @Aspect 注解的切面类Class
		Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
		// 获取标记 @Aspect的 beanName
		String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
		// 校验
		validate(aspectClass);

		// We need to wrap the MetadataAwareAspectInstanceFactory with a decorator
		// so that it will only instantiate once.
		MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory = new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);

        // 此aop切面类中定义的所有用于增强的方法。比如标注了 @Around, @Before, @After, @AfterReturning, @AfterThrowing 注解的方法。
		List<Advisor> advisors = new ArrayList<>();

		// 获取切面类里的增强方法。其实是排除Pointcut注解的其他方法，所以下面还需具体要判断是否为切面方法。
        List<Method> advisorMethods = getAdvisorMethods(aspectClass);
        // 遍历所有的 候选增强方法，并找到真实的增强方法，保存到 advisors list 中。
        for (Method candidateAdviceMethod : advisorMethods) {
			Advisor advisor = getAdvisor(candidateAdviceMethod, lazySingletonAspectInstanceFactory, advisors.size(), aspectName);
			if (advisor != null) {
				advisors.add(advisor);
			}
		}

		// If it's a per target aspect, emit the dummy instantiating aspect.
        // 如果是一个懒加载的切面，就虚拟一个切面。
		if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
			Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
			advisors.add(0, instantiationAdvisor);
		}

		// Find introduction fields.
		for (Field field : aspectClass.getDeclaredFields()) {
			Advisor advisor = getDeclareParentsAdvisor(field);
			if (advisor != null) {
				advisors.add(advisor);
			}
		}

		return advisors;
	}

    /**
     * 获取切面类里的增强方法。
     *
     * 遍历切面类里的所有方法，找到不带有 Pointcut 注解的所有方法。
     * @param aspectClass 切面类Class
     * @return
     */
	private List<Method> getAdvisorMethods(Class<?> aspectClass) {
		final List<Method> methods = new ArrayList<>();
		ReflectionUtils.doWithMethods(aspectClass, method -> {
			// 排除 pointcuts 方法。AnnotationUtils.getAnnotation(method, Pointcut.class) 为null说明，方法上没有 @Pointcut 注解
			if (AnnotationUtils.getAnnotation(method, Pointcut.class) == null) {
				methods.add(method);
			}
		});
        // 为增强方法排序
        methods.sort(METHOD_COMPARATOR);
		return methods;
	}

	/**
	 * Build a {@link org.springframework.aop.aspectj.DeclareParentsAdvisor}
	 * for the given introduction field.
	 * <p>Resulting Advisors will need to be evaluated for targets.
	 * @param introductionField the field to introspect
	 * @return the Advisor instance, or {@code null} if not an Advisor
	 */
	@Nullable
	private Advisor getDeclareParentsAdvisor(Field introductionField) {
		DeclareParents declareParents = introductionField.getAnnotation(DeclareParents.class);
		if (declareParents == null) {
			// Not an introduction field
			return null;
		}

		if (DeclareParents.class == declareParents.defaultImpl()) {
			throw new IllegalStateException("'defaultImpl' attribute must be set on DeclareParents");
		}

		return new DeclareParentsAdvisor(
				introductionField.getType(), declareParents.value(), declareParents.defaultImpl());
	}


    // 参数 candidateAdviceMethod 表示 ：候选的增强方法。所以这里还需要对此方法进一步校验。
	@Override
	@Nullable
	public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
			int declarationOrderInAspect, String aspectName) {

		validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());

		AspectJExpressionPointcut expressionPointcut = getPointcut(candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
		if (expressionPointcut == null) {
			return null;
		}

		return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
				this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
	}

	@Nullable
	private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
        // 从 候选增强方法 上找aop的注解，如果没有aop的指定注解，就说明此 候选方法并不是aop的增强方法。
		AspectJAnnotation<?> aspectJAnnotation = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
		if (aspectJAnnotation == null) {
			return null;
		}

        // @Before("pointCut()") @After("pointCut()") 等这些注解里的 "pointCut()" 表达式。
        String pointcutExpression = aspectJAnnotation.getPointcutExpression();

        AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
        ajexp.setExpression(pointcutExpression);
		if (this.beanFactory != null) {
			ajexp.setBeanFactory(this.beanFactory);
		}
		return ajexp;
	}


	@Override
	@Nullable
	public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {

		Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
		validate(candidateAspectClass);

		AspectJAnnotation<?> aspectJAnnotation = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
		if (aspectJAnnotation == null) {
			return null;
		}

		// If we get here, we know we have an AspectJ method.
		// Check that it's an AspectJ-annotated class
		if (!isAspect(candidateAspectClass)) {
			throw new AopConfigException("Advice must be declared inside an aspect type: " +
					"Offending method '" + candidateAdviceMethod + "' in class [" +
					candidateAspectClass.getName() + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found AspectJ method: " + candidateAdviceMethod);
		}

		AbstractAspectJAdvice springAdvice;

		switch (aspectJAnnotation.getAnnotationType()) {
			case AtPointcut:
				if (logger.isDebugEnabled()) {
					logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
				}
				return null;
			case AtAround:
				springAdvice = new AspectJAroundAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtBefore:
				springAdvice = new AspectJMethodBeforeAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtAfter:
				springAdvice = new AspectJAfterAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtAfterReturning:
				springAdvice = new AspectJAfterReturningAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
				if (StringUtils.hasText(afterReturningAnnotation.returning())) {
					springAdvice.setReturningName(afterReturningAnnotation.returning());
				}
				break;
			case AtAfterThrowing:
				springAdvice = new AspectJAfterThrowingAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
				if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
					springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
				}
				break;
			default:
				throw new UnsupportedOperationException(
						"Unsupported advice type on method: " + candidateAdviceMethod);
		}

		// Now to configure the advice...
		springAdvice.setAspectName(aspectName);
		springAdvice.setDeclarationOrder(declarationOrder);
		String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
		if (argNames != null) {
			springAdvice.setArgumentNamesFromStringArray(argNames);
		}
		springAdvice.calculateArgumentBindings();

		return springAdvice;
	}


	/**
	 * Synthetic advisor that instantiates the aspect.
	 * Triggered by per-clause pointcut on non-singleton aspect.
	 * The advice has no effect.
	 */
	@SuppressWarnings("serial")
	protected static class SyntheticInstantiationAdvisor extends DefaultPointcutAdvisor {

		public SyntheticInstantiationAdvisor(final MetadataAwareAspectInstanceFactory aif) {
			super(aif.getAspectMetadata().getPerClausePointcut(), (MethodBeforeAdvice)
					(method, args, target) -> aif.getAspectInstance());
		}
	}

}
