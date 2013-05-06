package com.gmail.radikll.pro.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

public class Analyze {
	
	// map structure class (top) level defines
	public static final String METHODS 	= "methods";
	public static final String CONSTRU 	= "constructors";
	public static final String FIELDS 	= "fields";
	public static final String MODIFIER	= "modifier";
	public static final String PACKAGE	= "package";
	public static final String NAME		= "name";
	
	// map structure method level defines
	public static final String RETURN = "return";
	// map structure method and constructor level defines
	public static final String PARAMETS	= "parameters";
	public static final String EXCEPTIS	= "exceptions";
	//blic static final String MODIFIER	= "modifier";
	//blic static final String NAME		= "name";
	public static final String STRING 	= "string";
	public static final String GENERSTI	= "generic string";
	
	// map structure member variable (field) level defines
	public static final String TYPE 	= "type";
	public static final String VALUE	= "value";
	//blic static final String MODIFIER	= "modifier";
	//blic static final String STRING 	= "string";
	//blic static final String GENERSTI	= "generic string";
	
	private HashMap<String,Object> mMapClass;
	
	public Analyze(Class lookat) {
		
		//this.classAnalyze = lookat.getClass();
		this.mMapClass = classToMap(lookat);
	}
	
	/**
	 * Creates a map that contains all the information on a given class.
	 * @param lookatClass - a class from an instantiated object of the class you want to look at
	 * @return w/e i'll write this later. // (3 mouths later) damn it...
	 */
	public static HashMap<String,Object> classToMap(Class lookatClass){
		
		HashMap<String,Object> mapEverything = new HashMap<String, Object>();
		
		//Class lookatClass = lookat.getClass(); // w/e...
		
		mapEverything.put(MODIFIER, lookatClass.getModifiers());
		mapEverything.put(PACKAGE, lookatClass.getPackage());
		mapEverything.put(NAME, lookatClass.getName());
		
		Method[] theMethods = lookatClass.getDeclaredMethods();
		mapEverything.put(METHODS, methodList(theMethods));
		for(Method smethod: theMethods)
			mapEverything.put(smethod.toGenericString(), methodInfo(smethod));
		
		Constructor[] theConstructors = lookatClass.getConstructors();
		mapEverything.put(CONSTRU, constructorList(theConstructors));
		for(Constructor sconstru: theConstructors)
			mapEverything.put(sconstru.toGenericString(), constructorInfo(sconstru));
		
		// these are the member variables
		Field[] theFields = lookatClass.getDeclaredFields();
		mapEverything.put(FIELDS, fieldList(theFields));
		for(Field sfield: theFields)
			mapEverything.put(sfield.getName(), fieldInfo(sfield));
		
		return mapEverything;
	}
	
	public HashMap<String,Object> getMap(){
		return mMapClass;
	}
	
	/**
	 * Displays the class in a human-readable form.
	 */
	public String mapToString(){
		
		String theClass = "package " + mMapClass.get(PACKAGE) + "; \n";
		
		theClass += modifiers((Integer) mMapClass.get(MODIFIER)) + " class ";
		theClass += mMapClass.get(NAME) + " { \n";
		
		theClass += fieldsToString();
		
		for(String sconstru: (ArrayList<String>)mMapClass.get(CONSTRU)){
			
			HashMap<String, Object> mapConstru = (HashMap<String, Object>) mMapClass.get(sconstru);
			theClass += mapConstru.get(STRING) + "\n";
		}		
		
		for(String smethod: (ArrayList<String>)mMapClass.get(METHODS)){
			
			HashMap<String, Object> mapMethod = (HashMap<String, Object>) mMapClass.get(smethod);
			theClass += mapMethod.get(STRING) + "\n";
		}
		
		theClass += "}";
		
		return theClass;
	}
	
	public static ArrayList<String> methodList(Method[] mmmethods){
		
		ArrayList<String> methods = new ArrayList<String>();
		
		for(Method method: mmmethods)
			methods.add(method.toGenericString());
		
		return methods;
	}
	
	public static HashMap<String, Object> methodInfo(Method mmmethod){
		
		HashMap<String, Object> mapMethod = new HashMap<String, Object>();
		
		mapMethod.put(RETURN, mmmethod.getReturnType());
		mapMethod.put(MODIFIER, mmmethod.getModifiers());
		mapMethod.put(NAME, mmmethod.getName());
		mapMethod.put(STRING, mmmethod.toString());
		mapMethod.put(GENERSTI, mmmethod.toGenericString());
		
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		for(Type type: mmmethod.getGenericParameterTypes())
			paramTypes.add(type);
		mapMethod.put(PARAMETS, paramTypes);
		
		ArrayList<Class> exceptions = new ArrayList<Class>();
		for(Class exception: mmmethod.getExceptionTypes())
			exceptions.add(exception);
		mapMethod.put(EXCEPTIS, exceptions);
		
		return mapMethod;
	}
		
	public static ArrayList<String> constructorList(Constructor[] mmconstructors){
		
		ArrayList<String> constructors = new ArrayList<String>();
		
		for(Constructor constructor: mmconstructors)
			constructors.add(constructor.toGenericString());
		
		return constructors;
	}
	
	public static HashMap<String, Object> constructorInfo(Constructor mmconstructor){
		
		HashMap<String, Object> mapConstru = new HashMap<String, Object>();
		
		mapConstru.put(MODIFIER, mmconstructor.getModifiers());
		mapConstru.put(NAME, mmconstructor.getName());
		mapConstru.put(STRING, mmconstructor.toString());
		mapConstru.put(GENERSTI, mmconstructor.toGenericString());
		
		ArrayList<Type> paramTypes = new ArrayList<Type>();
		for(Type type: mmconstructor.getGenericParameterTypes())
			paramTypes.add(type);
		mapConstru.put(PARAMETS, paramTypes);
		
		ArrayList<Class> exceptions = new ArrayList<Class>();
		for(Class exception: mmconstructor.getExceptionTypes())
			exceptions.add(exception);
		mapConstru.put(EXCEPTIS, exceptions);
		
		return mapConstru;
	}
	
	public static ArrayList<String> fieldList(Field[] mmfields){
		
		ArrayList<String> fields = new ArrayList<String>();
		
		for(Field field: mmfields)
			fields.add(field.getName());
		
		return fields;
	}
	
	public static HashMap<String, Object> fieldInfo(Field mmfield){
		
		HashMap<String, Object> mapfield = new HashMap<String, Object>();
		
		try {
			mapfield.put(VALUE, mmfield.get(mmfield.getName()));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		mapfield.put(TYPE, mmfield.getType());
		mapfield.put(MODIFIER, mmfield.getModifiers());
		mapfield.put(STRING, mmfield.toString());
		mapfield.put(GENERSTI, mmfield.toGenericString());
		
		return mapfield;
	}
	
	private String fieldsToString(){
		
		String fieldsString = new String();
		
		for(String sfeild: (ArrayList<String>)mMapClass.get(FIELDS)){
			// searches map for one of the fields from the field list
			HashMap<String, Object> mapfield = (HashMap<String, Object>) mMapClass.get(sfeild);
			
			fieldsString += modifiers((Integer) mapfield.get(MODIFIER));
			fieldsString += typename((Class) mapfield.get(TYPE));
			fieldsString += sfeild + " = " + mapfield.get(VALUE) + ";\n";
		}
		
		return fieldsString;		
	}
	
	private static String typename(Class t) {
	    
	    if(t == null)
	    	return "";
	    
	    String brackets = "";
	    while (t.isArray()) {
	      brackets += "[]";
	      t = t.getComponentType();
	    }
	    String name = t.getName();
	    int pos = name.lastIndexOf('.');
	    if (pos != -1)
	      name = name.substring(pos + 1);
	    return name + brackets + " ";
	  }
	
	private static String modifiers(int m) {
	    if (m == 0)
	      return "";
	    else
	      return Modifier.toString(m) + " ";
	  }
}
