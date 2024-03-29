/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.etk.entity.base.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.etk.common.logging.Logger;
import org.etk.entity.base.lang.IsEmpty;
import org.etk.entity.base.lang.SourceMonitored;

import com.ibm.icu.util.TimeZone;

import javolution.util.FastMap;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 6, 2011  
 */
public class ObjectType {

  private static Logger logger = Logger.getLogger(ObjectType.class);

  public static final Object NULL = new NullObject();

  protected static FastMap<String, Class<?>> classCache = FastMap.newInstance();

  public static final String LANG_PACKAGE = "java.lang."; // We will test both the raw value and this + raw value
  public static final String SQL_PACKAGE = "java.sql.";   // We will test both the raw value and this + raw value

  /**
   * Loads a class with the current thread's context classloader.
   * @param className The name of the class to load
   * @return The requested class
   * @throws ClassNotFoundException
   */
  public static Class<?> loadClass(String className) throws ClassNotFoundException {
      int genericsStart = className.indexOf("<");
      if (genericsStart != -1) className = className.substring(0, genericsStart);

      // small block to speed things up by putting using preloaded classes for common objects, this turns out to help quite a bit...
      Class<?> theClass = CachedClassLoader.globalClassNameClassMap.get(className);

      if (theClass != null) return theClass;

      return loadClass(className, null);
  }

  /**
   * Loads a class with the current thread's context classloader.
   * @param className The name of the class to load
   * @param loader The ClassLoader to use
   * @return The requested class
   * @throws ClassNotFoundException
   */
  public static Class<?> loadClass(String className, ClassLoader loader) throws ClassNotFoundException {
      int genericsStart = className.indexOf("<");
      if (genericsStart != -1) className = className.substring(0, genericsStart);

      // Handle array classes. Details in http://java.sun.com/j2se/1.5.0/docs/guide/jni/spec/types.html#wp16437
      if (className.endsWith("[]")) {
          if (Character.isLowerCase(className.charAt(0)) && className.indexOf(".") < 0) {
             String prefix = className.substring(0, 1).toUpperCase();
             // long and boolean have other prefix than first letter
             if (className.startsWith("long")) {
                 prefix = "J";
             } else if (className.startsWith("boolean")) {
                 prefix = "Z";
             }
             className = "[" + prefix;
          } else {
              Class<?> arrayClass = loadClass(className.replace("[]", ""), loader);
              className = "[L" + arrayClass.getName().replace("[]", "") + ";";
          }
      }

      // small block to speed things up by putting using preloaded classes for common objects, this turns out to help quite a bit...
      Class<?> theClass = CachedClassLoader.globalClassNameClassMap.get(className);

      if (theClass != null) return theClass;

      if (loader == null) loader = Thread.currentThread().getContextClassLoader();

      try {
          theClass = loader.loadClass(className);
      } catch (Exception e) {
          theClass = classCache.get(className);
          if (theClass == null) {
              theClass = Class.forName(className);
              if (theClass != null) {
                  if (classCache.putIfAbsent(className, theClass) == null) {
                      if (Debug.verboseOn()) Debug.logVerbose("Loaded Class: " + theClass.getName(), module);
                  }
              }
          }
      }

      return theClass;
  }

  /**
   * Returns an instance of the specified class.  This uses the default
   * no-arg constructor to create the instance.
   * @param className Name of the class to instantiate
   * @return An instance of the named class
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public static Object getInstance(String className) throws ClassNotFoundException,
          InstantiationException, IllegalAccessException {
      Class<?> c = loadClass(className);
      Object o = c.newInstance();

      if (Debug.verboseOn()) Debug.logVerbose("Instantiated object: " + o.toString(), module);
      return o;
  }

  /**
   * Tests if a class properly implements the specified interface.
   * @param objectClass Class to test
   * @param interfaceName Name of the interface to test against
   * @return true if interfaceName is an interface of objectClass
   * @throws ClassNotFoundException
   */
  public static boolean interfaceOf(Class<?> objectClass, String interfaceName) throws ClassNotFoundException {
      Class<?> interfaceClass = loadClass(interfaceName);

      return interfaceOf(objectClass, interfaceClass);
  }

  /**
   * Tests if a class properly implements the specified interface.
   * @param objectClass Class to test
   * @param interfaceObject to test against
   * @return true if interfaceObject is an interface of the objectClass
   */
  public static boolean interfaceOf(Class<?> objectClass, Object interfaceObject) {
      Class<?> interfaceClass = interfaceObject.getClass();

      return interfaceOf(objectClass, interfaceClass);
  }

  /**
   * Returns an instance of the specified class using the constructor matching the specified parameters.
   * @param className Name of the class to instantiate
   * @param parameters Parameters passed to the constructor
   * @return An instance of the className
   * @throws ClassNotFoundException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public static Object getInstance(String className, Object[] parameters) throws ClassNotFoundException,
          InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
      Class<?>[] sig = new Class<?>[parameters.length];
      for (int i = 0; i < sig.length; i++) {
          sig[i] = parameters[i].getClass();
      }
      Class<?> c = loadClass(className);
      Constructor<?> con = c.getConstructor(sig);
      Object o = con.newInstance(parameters);

      if (Debug.verboseOn()) Debug.logVerbose("Instantiated object: " + o.toString(), module);
      return o;
  }

  /**
   * Tests if an object properly implements the specified interface.
   * @param obj Object to test
   * @param interfaceName Name of the interface to test against
   * @return true if interfaceName is an interface of obj
   * @throws ClassNotFoundException
   */
  public static boolean interfaceOf(Object obj, String interfaceName) throws ClassNotFoundException {
      Class<?> interfaceClass = loadClass(interfaceName);

      return interfaceOf(obj, interfaceClass);
  }

  /**
   * Tests if an object properly implements the specified interface.
   * @param obj Object to test
   * @param interfaceObject to test against
   * @return true if interfaceObject is an interface of obj
   */
  public static boolean interfaceOf(Object obj, Object interfaceObject) {
      Class<?> interfaceClass = interfaceObject.getClass();

      return interfaceOf(obj, interfaceClass);
  }

  /**
   * Tests if an object properly implements the specified interface.
   * @param obj Object to test
   * @param interfaceClass Class to test against
   * @return true if interfaceClass is an interface of obj
   */
  public static boolean interfaceOf(Object obj, Class<?> interfaceClass) {
      Class<?> objectClass = obj.getClass();

      return interfaceOf(objectClass, interfaceClass);
  }

  /**
   * Tests if a class properly implements the specified interface.
   * @param objectClass Class to test
   * @param interfaceClass Class to test against
   * @return true if interfaceClass is an interface of objectClass
   */
  public static boolean interfaceOf(Class<?> objectClass, Class<?> interfaceClass) {
      while (objectClass != null) {
          Class<?>[] ifaces = objectClass.getInterfaces();

          for (Class<?> iface: ifaces) {
              if (iface == interfaceClass) return true;
          }
          objectClass = objectClass.getSuperclass();
      }
      return false;
  }

  /**
   * Tests if a class is a class of or a sub-class of the parent.
   * @param objectClass Class to test
   * @param parentName Name of the parent class to test against
   * @return true if objectClass is a class of or a sub-class of the parent
   * @throws ClassNotFoundException
   */
  public static boolean isOrSubOf(Class<?> objectClass, String parentName) throws ClassNotFoundException {
      Class<?> parentClass = loadClass(parentName);

      return isOrSubOf(objectClass, parentClass);
  }

  /**
   * Tests if a class is a class of or a sub-class of the parent.
   * @param objectClass Class to test
   * @param parentObject Object to test against
   * @return true if objectClass is a class of or a sub-class of the parent
   */
  public static boolean isOrSubOf(Class<?> objectClass, Object parentObject) {
      Class<?> parentClass = parentObject.getClass();

      return isOrSubOf(objectClass, parentClass);
  }

  /**
   * Tests if an object is an instance of or a sub-class of the parent.
   * @param obj Object to test
   * @param parentName Name of the parent class to test against
   * @return true if obj is an instance of or a sub-class of the parent
   * @throws ClassNotFoundException
   */
  public static boolean isOrSubOf(Object obj, String parentName) throws ClassNotFoundException {
      Class<?> parentClass = loadClass(parentName);

      return isOrSubOf(obj, parentClass);
  }

  /**
   * Tests if an object is an instance of or a sub-class of the parent.
   * @param obj Object to test
   * @param parentObject Object to test against
   * @return true if obj is an instance of or a sub-class of the parent
   */
  public static boolean isOrSubOf(Object obj, Object parentObject) {
      Class<?> parentClass = parentObject.getClass();

      return isOrSubOf(obj, parentClass);
  }

  /**
   * Tests if an object is an instance of or a sub-class of the parent.
   * @param obj Object to test
   * @param parentClass Class to test against
   * @return true if obj is an instance of or a sub-class of the parent
   */
  public static boolean isOrSubOf(Object obj, Class<?> parentClass) {
      Class<?> objectClass = obj.getClass();

      return isOrSubOf(objectClass, parentClass);
  }

  /**
   * Tests if a class is a class of or a sub-class of the parent.
   * @param objectClass Class to test
   * @param parentClass Class to test against
   * @return true if objectClass is a class of or a sub-class of the parent
   */
  public static boolean isOrSubOf(Class<?> objectClass, Class<?> parentClass) {
      //Debug.logInfo("Checking isOrSubOf for [" + objectClass.getName() + "] and [" + objectClass.getName() + "]", module);
      while (objectClass != null) {
          if (objectClass == parentClass) return true;
          objectClass = objectClass.getSuperclass();
      }
      return false;
  }

  /**
   * Tests if a class is a class of a sub-class of or properly implements an interface.
   * @param objectClass Class to test
   * @param typeObject Object to test against
   * @return true if objectClass is a class of a sub-class of, or properly implements an interface
   */
  public static boolean instanceOf(Class<?> objectClass, Object typeObject) {
      Class<?> typeClass = typeObject.getClass();

      return instanceOf(objectClass, typeClass);
  }

  /**
   * Tests if a class is a class of a sub-class of or properly implements an interface.
   * @param objectClass Class to test
   * @param typeName name to test against
   * @return true if objectClass is a class or a sub-class of, or properly implements an interface
   */
  public static boolean instanceOf(Class<?> objectClass, String typeName) {
      return instanceOf(objectClass, typeName, null);
  }

  /**
   * Tests if an object is an instance of a sub-class of or properly implements an interface.
   * @param obj Object to test
   * @param typeObject Object to test against
   * @return true if obj is an instance of a sub-class of, or properly implements an interface
   */
  public static boolean instanceOf(Object obj, Object typeObject) {
      Class<?> typeClass = typeObject.getClass();

      return instanceOf(obj, typeClass);
  }

  /**
   * Tests if an object is an instance of a sub-class of or properly implements an interface.
   * @param obj Object to test
   * @param typeName name to test against
   * @return true if obj is an instance of a sub-class of, or properly implements an interface
   */
  public static boolean instanceOf(Object obj, String typeName) {
      return instanceOf(obj, typeName, null);
  }

  /**
   * Tests if a class is a class of a sub-class of or properly implements an interface.
   * @param objectClass Class to test
   * @param typeName Object to test against
   * @param loader
   * @return true if objectClass is a class of a sub-class of, or properly implements an interface
   */
  public static boolean instanceOf(Class<?> objectClass, String typeName, ClassLoader loader) {
      Class<?> infoClass = loadInfoClass(typeName, loader);

      if (infoClass == null)
          throw new IllegalArgumentException("Illegal type found in info map (could not load class for specified type)");

      return instanceOf(objectClass, infoClass);
  }

  /**
   * Tests if an object is an instance of a sub-class of or properly implements an interface.
   * @param obj Object to test
   * @param typeName Object to test against
   * @param loader
   * @return true if obj is an instance of a sub-class of, or properly implements an interface
   */
  public static boolean instanceOf(Object obj, String typeName, ClassLoader loader) {
      Class<?> infoClass = loadInfoClass(typeName, loader);

      if (infoClass == null) {
          throw new IllegalArgumentException("Illegal type found in info map (could not load class for specified type)");
      }

      return instanceOf(obj, infoClass);
  }

  public static Class<?> loadInfoClass(String typeName, ClassLoader loader) {
      //Class infoClass = null;
      try {
          return ObjectType.loadClass(typeName, loader);
      } catch (SecurityException se1) {
          throw new IllegalArgumentException("Problems with classloader: security exception (" +
                  se1.getMessage() + ")");
      } catch (ClassNotFoundException e1) {
          try {
              return ObjectType.loadClass(LANG_PACKAGE + typeName, loader);
          } catch (SecurityException se2) {
              throw new IllegalArgumentException("Problems with classloader: security exception (" +
                      se2.getMessage() + ")");
          } catch (ClassNotFoundException e2) {
              try {
                  return ObjectType.loadClass(SQL_PACKAGE + typeName, loader);
              } catch (SecurityException se3) {
                  throw new IllegalArgumentException("Problems with classloader: security exception (" +
                          se3.getMessage() + ")");
              } catch (ClassNotFoundException e3) {
                  throw new IllegalArgumentException("Cannot find and load the class of type: " + typeName +
                          " or of type: " + LANG_PACKAGE + typeName + " or of type: " + SQL_PACKAGE + typeName +
                          ":  (" + e3.getMessage() + ")");
              }
          }
      }
  }

  /**
   * Tests if an object is an instance of a sub-class of or properly implements an interface.
   * @param obj Object to test
   * @param typeClass Class to test against
   * @return true if obj is an instance of a sub-class of typeClass
   */
  public static boolean instanceOf(Object obj, Class<?> typeClass) {
      if (obj == null) return true;
      Class<?> objectClass = obj.getClass();
      return instanceOf(objectClass, typeClass);
  }

  /**
   * Tests if a class is a class of a sub-class of or properly implements an interface.
   * @param objectClass Class to test
   * @param typeClass Class to test against
   * @return true if objectClass is a class or sub-class of, or implements typeClass
   */
  public static boolean instanceOf(Class<?> objectClass, Class<?> typeClass) {
      if (typeClass.isInterface() && !objectClass.isInterface()) {
          return interfaceOf(objectClass, typeClass);
      } else {
          return isOrSubOf(objectClass, typeClass);
      }
  }

  public static Object simpleTypeConvert(Object obj, String type, String format, Locale locale, boolean noTypeFail) throws GeneralException {
      return simpleTypeConvert(obj, type, format, null, locale, noTypeFail);
  }

  /**
   * Converts the passed object to the named simple type.  Supported types
   * include: String, Boolean, Double, Float, Long, Integer, Date (java.sql.Date),
   * Time, Timestamp, TimeZone;
   * @param obj Object to convert
   * @param type Name of type to convert to
   * @param format Optional (can be null) format string for Date, Time, Timestamp
   * @param timeZone Optional (can be null) TimeZone for converting dates and times
   * @param locale Optional (can be null) Locale for formatting and parsing Double, Float, Long, Integer
   * @param noTypeFail Fail (Exception) when no type conversion is available, false will return the primary object
   * @return the converted value
   * @throws GeneralException
   */
  @SourceMonitored
  @SuppressWarnings("unchecked")
  public static Object simpleTypeConvert(Object obj, String type, String format, TimeZone timeZone, Locale locale, boolean noTypeFail) throws GeneralException {
      if (obj == null) {
          return null;
      }

      int genericsStart = type.indexOf("<");
      if (genericsStart != -1) {
          type = type.substring(0, genericsStart);
      }

      if ("PlainString".equals(type)) {
          return obj.toString();
      }
      Class<?> sourceClass = obj.getClass();
      if (sourceClass.getName().equals(type)) {
          return obj;
      }
      if ("Object".equals(type) || "java.lang.Object".equals(type)) {
          return obj;
      }
      if (obj instanceof String && UtilValidate.isEmpty(obj)) {
          return null;
      }
      if (obj instanceof Node) {
          Node node = (Node) obj;
          String nodeValue =  node.getTextContent();
          if ("String".equals(type) || "java.lang.String".equals(type)) {
              return nodeValue;
          } else {
              return simpleTypeConvert(nodeValue, type, format, timeZone, locale, noTypeFail);
          }
      }
      Class<?> targetClass = null;
      try {
          targetClass = loadClass(type);
      } catch (ClassNotFoundException e) {
          throw new GeneralException("Conversion from " + sourceClass.getName() + " to " + type + " not currently supported", e);
      }
      Converter<Object,Object> converter = null;
      try {
          converter = (Converter<Object, Object>) Converters.getConverter(sourceClass, targetClass);
      } catch (ClassNotFoundException e) {}
      if (converter != null) {
          LocalizedConverter<Object, Object> localizedConverter = null;
          try {
              localizedConverter = (LocalizedConverter) converter;
          } catch (ClassCastException e) {}
          if (localizedConverter != null) {
              if (timeZone == null) {
                  timeZone = TimeZone.getDefault();
              }
              if (locale == null) {
                  locale = Locale.getDefault();
              }
              try {
                  return localizedConverter.convert(obj, locale, timeZone, format);
              } catch (ConversionException e) {
                  throw new GeneralException(e.getMessage(), e);
              }
          }
          try {
              return converter.convert(obj);
          } catch (ConversionException e) {
              throw new GeneralException(e.getMessage(), e);
          }
      }
      // we can pretty much always do a conversion to a String, so do that here
      if (targetClass.equals(String.class)) {
          Debug.logWarning("No special conversion available for " + obj.getClass().getName() + " to String, returning object.toString().", module);
          return obj.toString();
      }
      if (noTypeFail) {
          throw new GeneralException("Conversion from " + obj.getClass().getName() + " to " + type + " not currently supported");
      } else {
          if (Debug.infoOn()) Debug.logInfo("No type conversion available for " + obj.getClass().getName() + " to " + targetClass.getName() + ", returning original object.", module);
          return obj;
      }
  }

  public static Object simpleTypeConvert(Object obj, String type, String format, Locale locale) throws GeneralException {
      return simpleTypeConvert(obj, type, format, locale, true);
  }

  public static Boolean doRealCompare(Object value1, Object value2, String operator, String type, String format,
      List<Object> messages, Locale locale, ClassLoader loader, boolean value2InlineConstant) {
      if (logger.isDebugEnabled())
      logger.debug("Comparing value1: \"" + value1 + "\" " + operator + " value2:\"" + value2 + "\"");

      try {
          if (!"PlainString".equals(type)) {
              Class<?> clz = ObjectType.loadClass(type, loader);
              type = clz.getName();
          }
      } catch (ClassNotFoundException e) {
        logger.warn("The specified type [" + type + "] is not a valid class or a known special type, may see more errors later because of this: " + e.getMessage());
      }

      // some default behavior for null values, results in a bit cleaner operation
      if ("is-null".equals(operator) && value1 == null) {
          return Boolean.TRUE;
      } else if ("is-not-null".equals(operator) && value1 == null) {
          return Boolean.FALSE;
      } else if ("is-empty".equals(operator) && value1 == null) {
          return Boolean.TRUE;
      } else if ("is-not-empty".equals(operator) && value1 == null) {
          return Boolean.FALSE;
      } else if ("contains".equals(operator) && value1 == null) {
          return Boolean.FALSE;
      }

      int result = 0;

      Object convertedValue2 = null;
      if (value2 != null) {
          Locale value2Locale = locale;
          if (value2InlineConstant) {
              value2Locale = UtilMisc.parseLocale("en");
          }
          try {
              convertedValue2 = ObjectType.simpleTypeConvert(value2, type, format, value2Locale);
          } catch (GeneralException e) {
              logger.error(e.getMessage(), e);
              messages.add("Could not convert value2 for comparison: " + e.getMessage());
              return null;
          }
      }

      // have converted value 2, now before converting value 1 see if it is a Collection and we are doing a contains comparison
      if ("contains".equals(operator) && value1 instanceof Collection<?>) {
          Collection<?> col1 = (Collection<?>) value1;
          return col1.contains(convertedValue2) ? Boolean.TRUE : Boolean.FALSE;
      }

      Object convertedValue1 = null;
      try {
          convertedValue1 = ObjectType.simpleTypeConvert(value1, type, format, locale);
      } catch (GeneralException e) {
          logger.error(e.getMessage(), e);
          messages.add("Could not convert value1 for comparison: " + e.getMessage());
          return null;
      }

      // handle null values...
      if (convertedValue1 == null || convertedValue2 == null) {
          if ("equals".equals(operator)) {
              return convertedValue1 == null && convertedValue2 == null ? Boolean.TRUE : Boolean.FALSE;
          } else if ("not-equals".equals(operator)) {
              return convertedValue1 == null && convertedValue2 == null ? Boolean.FALSE : Boolean.TRUE;
          } else if ("is-not-empty".equals(operator) || "is-empty".equals(operator)) {
              // do nothing, handled later...
          } else {
              if (convertedValue1 == null) {
                  messages.add("Left value is null, cannot complete compare for the operator " + operator);
                  return null;
              }
              if (convertedValue2 == null) {
                  messages.add("Right value is null, cannot complete compare for the operator " + operator);
                  return null;
              }
          }
      }

      if ("contains".equals(operator)) {
          if ("java.lang.String".equals(type) || "PlainString".equals(type)) {
              String str1 = (String) convertedValue1;
              String str2 = (String) convertedValue2;

              return str1.indexOf(str2) < 0 ? Boolean.FALSE : Boolean.TRUE;
          } else {
              messages.add("Error in XML file: cannot do a contains compare between a String and a non-String type");
              return null;
          }
      } else if ("is-empty".equals(operator)) {
          if (convertedValue1 == null)
              return Boolean.TRUE;
          if (convertedValue1 instanceof String && ((String) convertedValue1).length() == 0)
              return Boolean.TRUE;
          if (convertedValue1 instanceof List<?> && ((List<?>) convertedValue1).size() == 0)
              return Boolean.TRUE;
          if (convertedValue1 instanceof Map<?, ?> && ((Map<?, ?>) convertedValue1).size() == 0)
              return Boolean.TRUE;
          return Boolean.FALSE;
      } else if ("is-not-empty".equals(operator)) {
          if (convertedValue1 == null)
              return Boolean.FALSE;
          if (convertedValue1 instanceof String && ((String) convertedValue1).length() == 0)
              return Boolean.FALSE;
          if (convertedValue1 instanceof List<?> && ((List<?>) convertedValue1).size() == 0)
              return Boolean.FALSE;
          if (convertedValue1 instanceof Map<?, ?> && ((Map<?, ?>) convertedValue1).size() == 0)
              return Boolean.FALSE;
          return Boolean.TRUE;
      }

      if ("java.lang.String".equals(type) || "PlainString".equals(type)) {
          String str1 = (String) convertedValue1;
          String str2 = (String) convertedValue2;

          if (str1.length() == 0 || str2.length() == 0) {
              if ("equals".equals(operator)) {
                  return str1.length() == 0 && str2.length() == 0 ? Boolean.TRUE : Boolean.FALSE;
              } else if ("not-equals".equals(operator)) {
                  return str1.length() == 0 && str2.length() == 0 ? Boolean.FALSE : Boolean.TRUE;
              } else {
                  messages.add("ERROR: Could not do a compare between strings with one empty string for the operator " + operator);
                  return null;
              }
          }
          result = str1.compareTo(str2);
      } else if ("java.lang.Double".equals(type) || "java.lang.Float".equals(type) || "java.lang.Long".equals(type) || "java.lang.Integer".equals(type) || "java.math.BigDecimal".equals(type)) {
          Number tempNum = (Number) convertedValue1;
          double value1Double = tempNum.doubleValue();

          tempNum = (Number) convertedValue2;
          double value2Double = tempNum.doubleValue();

          if (value1Double < value2Double)
              result = -1;
          else if (value1Double > value2Double)
              result = 1;
          else
              result = 0;
      } else if ("java.sql.Date".equals(type)) {
          java.sql.Date value1Date = (java.sql.Date) convertedValue1;
          java.sql.Date value2Date = (java.sql.Date) convertedValue2;
          result = value1Date.compareTo(value2Date);
      } else if ("java.sql.Time".equals(type)) {
          java.sql.Time value1Time = (java.sql.Time) convertedValue1;
          java.sql.Time value2Time = (java.sql.Time) convertedValue2;
          result = value1Time.compareTo(value2Time);
      } else if ("java.sql.Timestamp".equals(type)) {
          java.sql.Timestamp value1Timestamp = (java.sql.Timestamp) convertedValue1;
          java.sql.Timestamp value2Timestamp = (java.sql.Timestamp) convertedValue2;
          result = value1Timestamp.compareTo(value2Timestamp);
      } else if ("java.lang.Boolean".equals(type)) {
          Boolean value1Boolean = (Boolean) convertedValue1;
          Boolean value2Boolean = (Boolean) convertedValue2;
          if ("equals".equals(operator)) {
              if ((value1Boolean.booleanValue() && value2Boolean.booleanValue()) || (!value1Boolean.booleanValue() && !value2Boolean.booleanValue()))
                  result = 0;
              else
                  result = 1;
          } else if ("not-equals".equals(operator)) {
              if ((!value1Boolean.booleanValue() && value2Boolean.booleanValue()) || (value1Boolean.booleanValue() && !value2Boolean.booleanValue()))
                  result = 0;
              else
                  result = 1;
          } else {
              messages.add("Can only compare Booleans using the operators 'equals' or 'not-equals'");
              return null;
          }
      } else if ("java.lang.Object".equals(type)) {
          if (convertedValue1.equals(convertedValue2)) {
              result = 0;
          } else {
              result = 1;
          }
      } else {
          messages.add("Type \"" + type + "\" specified for compare not supported.");
          return null;
      }

      if (logger.isDebugEnabled()) logger.debug("Got Compare result: " + result + ", operator: " + operator);
      if ("less".equals(operator)) {
          if (result >= 0)
              return Boolean.FALSE;
      } else if ("greater".equals(operator)) {
          if (result <= 0)
              return Boolean.FALSE;
      } else if ("less-equals".equals(operator)) {
          if (result > 0)
              return Boolean.FALSE;
      } else if ("greater-equals".equals(operator)) {
          if (result < 0)
              return Boolean.FALSE;
      } else if ("equals".equals(operator)) {
          if (result != 0)
              return Boolean.FALSE;
      } else if ("not-equals".equals(operator)) {
          if (result == 0)
              return Boolean.FALSE;
      } else {
          messages.add("Specified compare operator \"" + operator + "\" not known.");
          return null;
      }

      if (logger.isDebugEnabled()) logger.debug("Returning true");
      return Boolean.TRUE;
  }

  @SuppressWarnings("unchecked")
  public static boolean isEmpty(Object value) {
      if (value == null) return true;

      if (value instanceof String) return UtilValidate.isEmpty((String) value);
      if (value instanceof Collection) return UtilValidate.isEmpty((Collection<? extends Object>) value);
      if (value instanceof Map) return UtilValidate.isEmpty((Map<? extends Object, ? extends Object>) value);
      if (value instanceof CharSequence) return UtilValidate.isEmpty((CharSequence) value);
      if (value instanceof IsEmpty) return UtilValidate.isEmpty((IsEmpty) value);

      // These types would flood the log
      // Number covers: BigDecimal, BigInteger, Byte, Double, Float, Integer, Long, Short
      if (value instanceof Boolean) return false;
      if (value instanceof Number) return false;
      if (value instanceof Character) return false;
      if (value instanceof java.sql.Timestamp) return false;

      if (logger.isDebugEnabled()) {
          logger.debug("In ObjectType.isEmpty(Object value) returning false for " + value.getClass() + " Object.");
      }
      return false;
  }

  @SuppressWarnings("serial")
  public static final class NullObject implements Serializable {
      public NullObject() { }

      @Override
      public String toString() {
          return "ObjectType.NullObject";
      }

      @Override
      public int hashCode() {
          return toString().hashCode();
      }

      @Override
      public boolean equals(Object other) {
          if (other instanceof NullObject) {
              // should do equality of object? don't think so, just same type
              return true;
          } else {
              return false;
          }
      }
  }
}