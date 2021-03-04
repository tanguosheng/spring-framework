package io.spring2go.corespring.stack;

// 栈接口
public interface IStack<T> {
	public boolean isEmpty();
	
	public T pop() throws StackException;
	
	public T peek() throws StackException;
	
	public void push (T e) throws StackException;
	
	public void clear();
}
