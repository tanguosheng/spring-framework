package com.atguigu.aop;

public class MathCalculator /* implements IMathCalculator */ {// 如果实现了接口,就会使用jdk动态代理;否则使用cglib动态代理.也可以通过配置强制使用cglib动态代理.
	
	public int div(int i,int j){
		System.out.println("MathCalculator...div...");
		return i/j;	
	}

}
