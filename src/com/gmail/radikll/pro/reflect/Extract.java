package com.gmail.radikll.pro.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Extract {
	
	/**
	 * Will search a class for a method then returns that method.
	 * @param lookat - a class from an instantiated object of the class you want to extract from
	 * @param name - the name of the method you want
	 * @return A method if successful else null :/
	 */
	public static Method getMethod(Class lookat, String name){
		
		for(Method method : lookat.getDeclaredMethods())
			if(method.getName().equals(name))
				return method;
		
		return null;
	}
	
	/**
	 * Will set a variable from an instantiated object.
	 * @param lookat - a class from an instantiated object of the class you want to extract from
	 * @param name - the name of the variable you want
	 * @param value - the value you want to set
	 * @return True if successful and false if not
	 */
	public static Boolean setVariable(Class lookat, String name, Object value){
		
		for(Field field : lookat.getDeclaredFields())
			if(field.getName().equals(name))
				try {
					field.set(field, value);
					return true;
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
		
		return false;
	}
	
	/**
	 * Will get a variable from an instantiated object.
	 * @param lookat - a class from an instantiated object of the class you want to extract from
	 * @param name - the name of the variable you want
	 * @return The value of the variable
	 */
	public static Object getVariable(Class lookat, String name){
		
		for(Field field : lookat.getDeclaredFields())
			if(field.getName().equals(name))
				try {
					return field.get(name); // i'm not sure if this is how you use this method
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
		
		return null;
	}
	
}
