package edu.tum.uc.jvm.pxp;

import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.ADDRESS;
import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.ARRAYS;
import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.ARRAY_ELEMENTS;
import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.CLASSNAME;
import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.FIELD_NAME;
import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.INDEX;
import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.INSTANCE_FIELDS;
import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.OBJECTS;
import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.OBJECT_ADDRESS;
import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.STATIC_FIELDS;
import static de.tum.in.i22.uc.cm.datatypes.java.NameKeys.TYPE;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

import edu.tum.uc.jvm.utility.UnsafeUtil;

public class Enforcer {

    private static boolean DEFAULT_BOOLEAN;
    private static byte DEFAULT_BYTE;
    private static short DEFAULT_SHORT;
    private static int DEFAULT_INT;
    private static long DEFAULT_LONG;
    private static float DEFAULT_FLOAT;
    private static double DEFAULT_DOUBLE;
    private static char DEFAULT_CHAR;

    private static String ERROR_MSG_START = "Enforcement failed on ";

    public static void deleteData(Map<String, Set<Map<String, String>>> locations) {
	for (Map<String, String> objectProps : locations.get(OBJECTS)) {
	    replaceObjectWithNew(objectProps);
	}
	for (Map<String, String> arrayProps : locations.get(ARRAYS)) {
	    //replaceArrayWithNew(arrayProps);
	}
	for (Map<String, String> arrayElemProps : locations.get(ARRAY_ELEMENTS)) {
	    replaceArrayElementWithNew(arrayElemProps);
	}
	for (Map<String, String> fieldProps : locations.get(INSTANCE_FIELDS)) {
	    replaceInstanceFieldValueWithNew(fieldProps);
	}
	for (Map<String, String> fieldProps : locations.get(STATIC_FIELDS)) {
	    replaceStaticFieldValueWithNew(fieldProps);
	}
    }

    private static void replaceObjectWithNew(Map<String, String> objectProps) {
	try {
	    long address = Long.parseLong(objectProps.get(ADDRESS));
	    String className = objectProps.get(CLASSNAME);

	    Object oldObject = UnsafeUtil.objectFromAddress(address);
	    // check that the object has still correct type and not null,
	    // otherwise it could have already been cleaned up by GC
	    //System.out.println(objectProps);
	    //System.out.println(oldObject);
	    if (oldObject != null && oldObject.getClass().getName().equals(className)) {
		if (oldObject instanceof FilterInputStream) {
		    Field in = FilterInputStream.class.getDeclaredField("in");
		    in.setAccessible(true);
		    InputStream oldIS = (InputStream) in.get(oldObject);
		    //if (oldIS != null) oldIS.close();
		    in.set(oldObject, new ByteArrayInputStream("I am Batman".getBytes()));
		} else if (oldObject instanceof FilterOutputStream) {
		    //System.out.println("will replace out");
		    Field out = FilterOutputStream.class.getDeclaredField("out");
		    out.setAccessible(true);
		    OutputStream oldOs = (OutputStream) out.get(oldObject);
		    //if (oldOs != null) oldOs.close();
		    out.set(oldObject, new DummyOutputStream());
		    //System.out.println("replaced out");
		    
		} else {
		    Object newObject = createNewObject(oldObject.getClass());
		    UnsafeUtil.replaceObject(oldObject, newObject);	
		}	
	    } else {
		throw new EnforcementException("Object not found");
	    }
	} catch (Exception e) {
	    System.err.println(ERROR_MSG_START + "object:");
	    System.err.println(objectProps);
	    e.printStackTrace();
	}

    }

    private static void replaceArrayWithNew(Map<String, String> arrayProps) {
	try {
	    String arrayType = arrayProps.get(TYPE);
	    long address = Long.parseLong(arrayProps.get(ADDRESS));

	    Object oldArray = UnsafeUtil.objectFromAddress(address);
	    // check that the array has still correct type and not null,
	    // otherwise
	    // it could have already been cleaned up by GC
	    if (oldArray != null && oldArray.getClass().getName().equals(arrayType)) {
		int length = Array.getLength(oldArray);
		Class<?> elementType = oldArray.getClass().getComponentType();
		Object newArray = Array.newInstance(elementType, length);
		if (!elementType.isPrimitive()) {
		    // if this is an object array, place new objects where
		    // non-null elements were before
		    for (int i = 0; i < length; i++) {
			if (Array.get(oldArray, i) != null) {
			    Array.set(newArray, i, createNewObject(elementType));
			}
		    }
		}
		UnsafeUtil.replaceObject(oldArray, newArray);
	    } else {
		throw new EnforcementException("Array not found");
	    }
	} catch (Exception e) {
	    System.err.println(ERROR_MSG_START + "array:");
	    System.err.println(arrayProps);
	    e.printStackTrace();
	}

    }

    // puts a default value into an array element of primitive type only
    private static void replaceArrayElementWithNew(Map<String, String> arrayElemProps) {
	try {
	    String arrayType = arrayElemProps.get(TYPE);
	    long address = Long.parseLong(arrayElemProps.get(ADDRESS));
	    int index = Integer.parseInt(arrayElemProps.get(INDEX));

	    Object array = UnsafeUtil.objectFromAddress(address);
	    if (array != null && array.getClass().getName().equals(arrayType) && Array.getLength(array) > index) {
		Class<?> elementClass = array.getClass().getComponentType();
		Object defaultValue = getDefaultValue(elementClass);
		Array.set(array, index, defaultValue);
	    } else {
		throw new EnforcementException("Array not found");
	    }
	} catch (Exception e) {
	    System.err.println(ERROR_MSG_START + "array element:");
	    System.err.println(arrayElemProps);
	    e.printStackTrace();
	}
    }

    // puts a default value into an instance field of primitive type only
    private static void replaceInstanceFieldValueWithNew(Map<String, String> fieldProps) {
	try {
	    String fieldOwnerClass = fieldProps.get(CLASSNAME);
	    long fieldOwnerAddress = Long.parseLong(fieldProps.get(OBJECT_ADDRESS));
	    String fieldName = fieldProps.get(FIELD_NAME);

	    Object fieldOwner = UnsafeUtil.objectFromAddress(fieldOwnerAddress);
	    if (fieldOwner != null && fieldOwner.getClass().getName().equals(fieldOwnerClass)) {
		Field field = fieldOwner.getClass().getField(fieldName);
		field.setAccessible(true);
		field.set(fieldOwner, getDefaultValue(field.getType()));
	    } else {
		throw new EnforcementException("Field owner not found");
	    }
	} catch (Exception e) {
	    System.err.println(ERROR_MSG_START + "instance field:");
	    System.err.println(fieldProps);
	    e.printStackTrace();
	}
    }

    // puts a default value into a static field of primitive type only
    private static void replaceStaticFieldValueWithNew(Map<String, String> fieldProps) {
	try {
	    String fieldOwnerClass = fieldProps.get(CLASSNAME);
	    String fieldName = fieldProps.get(FIELD_NAME);

	    Field field = Class.forName(fieldOwnerClass).getField(fieldName);
	    field.setAccessible(true);
	    field.set(null, getDefaultValue(field.getType()));
	} catch (Exception e) {
	    System.err.println(ERROR_MSG_START + "static field:");
	    System.err.println(fieldProps);
	    e.printStackTrace();
	}

    }

    private static Object createNewObject(Class<?> clazz) throws NoSuchMethodException, SecurityException,
	    InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	// check if inputstream or outputstream
	Class<?> clazz2 = clazz;
	while ((clazz2 = clazz2.getSuperclass()) != null) {
	    if (clazz2.getName().equals(FilterOutputStream.class.getName())) {
		InputStream dummy = new ByteArrayInputStream("Lorem impsum".getBytes());
		// try generic constructor with one inputstream
		try {
		    Constructor<?> constructor = clazz.getDeclaredConstructor(InputStream.class);
		    constructor.setAccessible(true);
		    return constructor.newInstance(dummy);
		} catch (Exception e) {
		    return null;
		}
	    }
	    
	    if (clazz2.getName().equals(OutputStream.class.getName())) {
		OutputStream dummy = new DummyOutputStream();
		// try generic constructor with one os
		try {
		    Constructor<?> constructor = clazz.getDeclaredConstructor(OutputStream.class);
		    constructor.setAccessible(true);
		    return constructor.newInstance(dummy);
		} catch (Exception e) {
		    return null;
		}
	    }
	}
	
	// try zero param constructor
	try {
	    Constructor<?> constructor = clazz.getDeclaredConstructor(new Class[0]);
	    constructor.setAccessible(true);
	    return constructor.newInstance(new Object[0]);
	} catch (Exception e) {
	    // if that fails, try others
	    try {
		for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
			ctor.setAccessible(true);
			return ctor.newInstance(new Object[ctor.getParameterCount()]);
		    }
	    } catch (Exception e2) {
		
	    }
	    
	}
	return null;
    }

    private static Object getDefaultValue(Class<?> clazz) {
	if (clazz.equals(boolean.class)) {
	    return DEFAULT_BOOLEAN;
	} else if (clazz.equals(byte.class)) {
	    return DEFAULT_BYTE;
	} else if (clazz.equals(short.class)) {
	    return DEFAULT_SHORT;
	} else if (clazz.equals(int.class)) {
	    return DEFAULT_INT;
	} else if (clazz.equals(long.class)) {
	    return DEFAULT_LONG;
	} else if (clazz.equals(float.class)) {
	    return DEFAULT_FLOAT;
	} else if (clazz.equals(double.class)) {
	    return DEFAULT_DOUBLE;
	} else if (clazz.equals(char.class)) {
	    return DEFAULT_CHAR;
	} else {
	    throw new IllegalArgumentException("Class " + clazz.getName() + " does not represent a value type.");
	}
    }

    static class EnforcementException extends Exception {
	private static final long serialVersionUID = -1378291520760364502L;

	public EnforcementException(String message) {
	    super(message);
	}
    }

}
