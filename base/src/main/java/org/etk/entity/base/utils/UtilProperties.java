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

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 6, 2011  
 */
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.etk.common.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Generic Property Accessor with Cache - Utilities for working with properties files.
 * <p>UtilProperties divides properties files into two classes: non-locale-specific -
 * which are used for application parameters, configuration settings, etc; and
 * locale-specific - which are used for UI labels, system messages, etc. Each class
 * of properties files is kept in its own cache.</p>
 * <p>The locale-specific class of properties files can be in any one of three
 * formats: the standard text-based key=value format (*.properties file), the Java
 * XML properties format, and the OFBiz-specific XML file format
 * (see the <a href="#xmlToProperties(java.io.InputStream,%20java.util.Locale,%20java.util.Properties)">xmlToProperties</a>
 * method).</p>
 */
@SuppressWarnings("serial")
public class UtilProperties implements Serializable {

  private static final Logger logger = Logger.getLogger(UtilProperties.class);
    public static final String module = UtilProperties.class.getName();

    /** An instance of the generic cache for storing the non-locale-specific properties.
     *  Each Properties instance is keyed by the resource String.
     */
    protected static UtilCache<String, Properties> resourceCache = UtilCache.createUtilCache("properties.UtilPropertiesResourceCache");

    /** An instance of the generic cache for storing the non-locale-specific properties.
     *  Each Properties instance is keyed by the file's URL.
     */
    protected static UtilCache<String, Properties> urlCache = UtilCache.createUtilCache("properties.UtilPropertiesUrlCache");

    protected static Locale fallbackLocale = null;
    protected static Set<Locale> defaultCandidateLocales = null;
    protected static Set<String> propertiesNotFound = FastSet.newInstance();

    /** Compares the specified property to the compareString, returns true if they are the same, false otherwise
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param compareString The String to compare the property value to
     * @return True if the strings are the same, false otherwise
     */
    public static boolean propertyValueEquals(String resource, String name, String compareString) {
        String value = getPropertyValue(resource, name);

        if (value == null) return false;
        return value.trim().equals(compareString);
    }

    /** Compares Ignoring Case the specified property to the compareString, returns true if they are the same, false otherwise
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param compareString The String to compare the property value to
     * @return True if the strings are the same, false otherwise
     */
    public static boolean propertyValueEqualsIgnoreCase(String resource, String name, String compareString) {
        String value = getPropertyValue(resource, name);

        if (value == null) return false;
        return value.trim().equalsIgnoreCase(compareString);
    }

    /** Returns the value of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultValue is returned.
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param defaultValue The value to return if the property is not found
     * @return The value of the property in the properties file, or if not found then the defaultValue
     */
    public static String getPropertyValue(String resource, String name, String defaultValue) {
        String value = getPropertyValue(resource, name);

        if (UtilValidate.isEmpty(value))
            return defaultValue;
        else
            return value;
    }

    public static double getPropertyNumber(String resource, String name, double defaultValue) {
        String str = getPropertyValue(resource, name);
        if (str == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static double getPropertyNumber(String resource, String name) {
        return getPropertyNumber(resource, name, 0.00000);
    }

    /**
     * Returns the Number as a Number-Object of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultObject is returned.
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param defaultNumber Optional: The Number to return if the property is not found.
     * @param type A String of the the Object the Number is converted to (like "Integer").
     * @return A Number-Object of the property as the defined type; or if not found the defaultObject
     */
    private static Number getPropertyNumber(String resource, String name, Number defaultNumber, String type) {
        String str = getPropertyValue(resource, name);
        if (UtilValidate.isEmpty(str)) {
            logger.warn("Error converting String \"" + str + "\" to " + type + "; using defaultNumber " + defaultNumber + ".", module);
            return defaultNumber;
        } else
            try {
                return (Number)(ObjectType.simpleTypeConvert(str, type, null, null));
            } catch (GeneralException e) {
                Debug.logWarning("Error converting String \"" + str + "\" to " + type + "; using defaultNumber " + defaultNumber + ".", module);
            }
            return defaultNumber;
    }

    /**
     * Returns a Boolean-Object of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultValue is returned.
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param defaultValue Optional: The Value to return if the property is not found or not the correct format.
     * @return A Boolean-Object of the property; or if not found the defaultValue
     */
    public static Boolean getPropertyAsBoolean(String resource, String name, boolean defaultValue) {
        String str = getPropertyValue(resource, name);
        if ("true".equalsIgnoreCase(str)) return Boolean.TRUE;
        else if ("false".equalsIgnoreCase(str)) return Boolean.FALSE;
        else return defaultValue;
    }

    /**
     * Returns an Integer-Object of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultNumber is returned.
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param defaultNumber Optional: The Value to return if the property is not found.
     * @return An Integer-Object of the property; or if not found the defaultNumber
     */
    public static Integer getPropertyAsInteger(String resource, String name, int defaultNumber) {
        return (Integer)getPropertyNumber(resource, name, defaultNumber, "Integer");
    }

    /**
     * Returns a Long-Object of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultNumber is returned.
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param defaultNumber Optional: The Value to return if the property is not found.
     * @return A Long-Object of the property; or if not found the defaultNumber
     */
    public static Long getPropertyAsLong(String resource, String name, long defaultNumber) {
        return (Long)getPropertyNumber(resource, name, defaultNumber, "Long");
    }

    /**
     * Returns a Float-Object of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultNumber is returned.
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param defaultNumber Optional: The Value to return if the property is not found.
     * @return A Long-Object of the property; or if not found the defaultNumber
     */
    public static Float getPropertyAsFloat(String resource, String name, float defaultNumber) {
        return (Float)getPropertyNumber(resource, name, defaultNumber, "Float");
    }

    /**
     * Returns a Double-Object of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultNumber is returned.
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param defaultNumber Optional: The Value to return if the property is not found.
     * @return A Double-Object of the property; or if not found the defaultNumber
     */
    public static Double getPropertyAsDouble(String resource, String name, double defaultNumber) {
        return (Double)getPropertyNumber(resource, name, defaultNumber, "Double");
    }

    /**
     * Returns a BigInteger-Object of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultNumber is returned.
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param defaultNumber Optional: The Value to return if the property is not found.
     * @return A BigInteger-Object of the property; or if not found the defaultNumber
     */
    public static BigInteger getPropertyAsBigInteger(String resource, String name, BigInteger defaultNumber) {
        String strValue = getPropertyValue(resource, name);
        BigInteger result = defaultNumber;
        try {
            result = new BigInteger(strValue);
        } catch (NumberFormatException nfe) {
            Debug.logWarning("Couldnt convert String \"" + strValue + "\" to BigInteger; using defaultNumber " + defaultNumber.toString() + ".", module);
        }
        return result;
    }

    /**
     * Returns a BigDecimal-Object of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultNumber is returned.
     * @param resource The name of the resource - if the properties file is 'webevent.properties', the resource name is 'webevent'
     * @param name The name of the property in the properties file
     * @param defaultNumber Optional: The Value to return if the property is not found.
     * @return A BigDecimal-Object of the property; or if not found the defaultNumber
     */
    public static BigDecimal getPropertyAsBigDecimal(String resource, String name, BigDecimal defaultNumber) {
        String strValue = getPropertyValue(resource, name);
        BigDecimal result = defaultNumber;
        try {
            result = new BigDecimal(strValue);
        } catch (NumberFormatException nfe) {
            logger.warn("Couldnt convert String \"" + strValue + "\" to BigDecimal; using defaultNumber " + defaultNumber.toString() + ".");
        }
        return result;
    }

    /** Returns the value of the specified property name from the specified resource/properties file
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @return The value of the property in the properties file
     */
    public static String getPropertyValue(String resource, String name) {
        if (resource == null || resource.length() <= 0) return "";
        if (name == null || name.length() <= 0) return "";

        Properties properties = getProperties(resource);
        if (properties == null) {
            return "";
        }

        String value = null;

        try {
            value = properties.getProperty(name);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return value == null ? "" : value.trim();
    }

    /** Returns the specified resource/properties file
     * @param resource The name of the resource - can be a file, class, or URL
     * @return The properties file
     */
    public static Properties getProperties(String resource) {
        if (resource == null || resource.length() <= 0) {
            return null;
        }
        String cacheKey = resource.replace(".properties", "");
        Properties properties = resourceCache.get(cacheKey);
        if (properties == null) {
            try {
                URL url = UtilURL.fromResource(resource);

                if (url == null)
                    return null;
                properties = getProperties(url);
                resourceCache.put(cacheKey, properties);
            } catch (MissingResourceException e) {
              logger.error(e.getMessage(), e);
            }
        }
        if (properties == null) {
            logger.info("[UtilProperties.getProperties] could not find resource: " + resource);
            return null;
        }
        return properties;
    }

    /** Returns the specified resource/properties file
     * @param url The URL to the resource
     * @return The properties file
     */
    public static Properties getProperties(URL url) {
        if (url == null) {
            return null;
        }
        Properties properties = urlCache.get(url.toString());
        if (properties == null) {
            try {
                properties = new Properties();
                properties.load(url.openStream());
                urlCache.put(url.toString(), properties);
            } catch (Exception e) {
              logger.error(e.getMessage(), e);
            }
        }
        if (properties == null) {
            logger.info("[UtilProperties.getProperties] could not find resource: " + url);
            return null;
        }
        return properties;
    }


    // ========= URL Based Methods ==========

    /** Compares the specified property to the compareString, returns true if they are the same, false otherwise
     * @param url URL object specifying the location of the resource
     * @param name The name of the property in the properties file
     * @param compareString The String to compare the property value to
     * @return True if the strings are the same, false otherwise
     */
    public static boolean propertyValueEquals(URL url, String name, String compareString) {
        String value = getPropertyValue(url, name);

        if (value == null) return false;
        return value.trim().equals(compareString);
    }

    /** Compares Ignoring Case the specified property to the compareString, returns true if they are the same, false otherwise
     * @param url URL object specifying the location of the resource
     * @param name The name of the property in the properties file
     * @param compareString The String to compare the property value to
     * @return True if the strings are the same, false otherwise
     */
    public static boolean propertyValueEqualsIgnoreCase(URL url, String name, String compareString) {
        String value = getPropertyValue(url, name);

        if (value == null) return false;
        return value.trim().equalsIgnoreCase(compareString);
    }

    /** Returns the value of the specified property name from the specified resource/properties file.
     * If the specified property name or properties file is not found, the defaultValue is returned.
     * @param url URL object specifying the location of the resource
     * @param name The name of the property in the properties file
     * @param defaultValue The value to return if the property is not found
     * @return The value of the property in the properties file, or if not found then the defaultValue
     */
    public static String getPropertyValue(URL url, String name, String defaultValue) {
        String value = getPropertyValue(url, name);

        if (value == null || value.length() <= 0)
            return defaultValue;
        else
            return value;
    }

    public static double getPropertyNumber(URL url, String name, double defaultValue) {
        String str = getPropertyValue(url, name);
        if (str == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static double getPropertyNumber(URL url, String name) {
        return getPropertyNumber(url, name, 0.00000);
    }

    /** Returns the value of the specified property name from the specified resource/properties file
     * @param url URL object specifying the location of the resource
     * @param name The name of the property in the properties file
     * @return The value of the property in the properties file
     */
    public static String getPropertyValue(URL url, String name) {
        if (url == null) return "";
        if (name == null || name.length() <= 0) return "";
        Properties properties = getProperties(url);

        if (properties == null) {
            return null;
        }

        String value = null;

        try {
            value = properties.getProperty(name);
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
        return value == null ? "" : value.trim();
    }

    /** Returns the value of a split property name from the specified resource/properties file
     * Rather than specifying the property name the value of a name.X property is specified which
     * will correspond to a value.X property whose value will be returned. X is a number from 1 to
     * whatever and all values are checked until a name.X for a certain X is not found.
     * @param url URL object specifying the location of the resource
     * @param name The name of the split property in the properties file
     * @return The value of the split property from the properties file
     */
    public static String getSplitPropertyValue(URL url, String name) {
        if (url == null) return "";
        if (name == null || name.length() <= 0) return "";

        Properties properties = getProperties(url);

        if (properties == null) {
            return null;
        }

        String value = null;

        try {
            int curIdx = 1;
            String curName = null;

            while ((curName = properties.getProperty("name." + curIdx)) != null) {
                if (name.equals(curName)) {
                    value = properties.getProperty("value." + curIdx);
                    break;
                }
                curIdx++;
            }
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
        return value == null ? "" : value.trim();
    }

    /** Sets the specified value of the specified property name to the specified resource/properties file
     * @param resource The name of the resource - must be a file
     * @param name The name of the property in the properties file
     * @param value The value of the property in the properties file */
     public static void setPropertyValue(String resource, String name, String value) {
         if (resource == null || resource.length() <= 0) return;
         if (name == null || name.length() <= 0) return;

         Properties properties = getProperties(resource);
         if (properties == null) {
             return;
         }

         try {
             properties.setProperty(name, value);
             FileOutputStream propFile = new FileOutputStream(resource);
             if ("XuiLabels".equals(name)) {
                 properties.store(propFile,
                     "##############################################################################\n"
                     +"# Licensed to the Apache Software Foundation (ASF) under one                   \n"
                     +"# or more contributor license agreements.  See the NOTICE file                 \n"
                     +"# distributed with this work for additional information                        \n"
                     +"# regarding copyright ownership.  The ASF licenses this file                   \n"
                     +"# to you under the Apache License, Version 2.0 (the                            \n"
                     +"# \"License\"); you may not use this file except in compliance                 \n"
                     +"# with the License.  You may obtain a copy of the License at                   \n"
                     +"#                                                                              \n"
                     +"# http://www.apache.org/licenses/LICENSE-2.0                                   \n"
                     +"#                                                                              \n"
                     +"# Unless required by applicable law or agreed to in writing,                   \n"
                     +"# software distributed under the License is distributed on an                  \n"
                     +"# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                     \n"
                     +"# KIND, either express or implied.  See the License for the                    \n"
                     +"# specific language governing permissions and limitations                      \n"
                     +"# under the License.                                                           \n"
                     +"###############################################################################\n"
                     +"#                                                                              \n"
                     +"# Dynamically modified by OFBiz Framework (org.ofbiz.base.util : UtilProperties.setPropertyValue)\n"
                     +"#                                                                              \n"
                     +"# By default the screen is 1024x768 wide. If you want to use another screen size,\n"
                     +"# you must create a new directory under specialpurpose/pos/screens, like the 800x600.\n"
                     +"# You must also set the 3 related parameters (StartClass, ClientWidth, ClientHeight) accordingly.\n"
                     +"#");
             } else {
                 properties.store(propFile,
                     "##############################################################################\n"
                     +"# Licensed to the Apache Software Foundation (ASF) under one                   \n"
                     +"# or more contributor license agreements.  See the NOTICE file                 \n"
                     +"# distributed with this work for additional information                        \n"
                     +"# regarding copyright ownership.  The ASF licenses this file                   \n"
                     +"# to you under the Apache License, Version 2.0 (the                            \n"
                     +"# \"License\"); you may not use this file except in compliance                 \n"
                     +"# with the License.  You may obtain a copy of the License at                   \n"
                     +"#                                                                              \n"
                     +"# http://www.apache.org/licenses/LICENSE-2.0                                   \n"
                     +"#                                                                              \n"
                     +"# Unless required by applicable law or agreed to in writing,                   \n"
                     +"# software distributed under the License is distributed on an                  \n"
                     +"# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                     \n"
                     +"# KIND, either express or implied.  See the License for the                    \n"
                     +"# specific language governing permissions and limitations                      \n"
                     +"# under the License.                                                           \n"
                     +"###############################################################################\n"
                     +"#                                                                              \n"
                     +"# Dynamically modified by OFBiz Framework (org.ofbiz.base.util : UtilProperties.setPropertyValue)\n"
                     +"# The comments have been removed, you may still find them on the OFBiz repository... \n"
                     +"#");
             }

             propFile.close();
         } catch (FileNotFoundException e) {
             logger.error("Unable to located the resource file.", e);
         } catch (IOException e) {
           logger.error(e.getMessage(), e);
         }
     }

     /** Sets the specified value of the specified property name to the specified resource/properties in memory, does not persist it
      * @param resource The name of the resource
      * @param name The name of the property in the resource
      * @param value The value of the property to set in memory */
      public static void setPropertyValueInMemory(String resource, String name, String value) {
          if (resource == null || resource.length() <= 0) return;
          if (name == null || name.length() <= 0) return;

          Properties properties = getProperties(resource);
          if (properties == null) {
              return;
          }
          properties.setProperty(name, value);
      }

    // ========= Locale & Resource Based Methods ==========

    /** Returns the value of the specified property name from the specified
     *  resource/properties file corresponding to the given locale.
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @param locale The locale that the given resource will correspond to
     * @return The value of the property in the properties file
     */
    public static String getMessage(String resource, String name, Locale locale) {
        if (resource == null || resource.length() <= 0) return "";
        if (name == null || name.length() <= 0) return "";

        ResourceBundle bundle = getResourceBundle(resource, locale);

        if (bundle == null) return name;

        String value = null;
        try {
            value = bundle.getString(name);
        } catch (Exception e) {
            //Debug.log(e, module);
        }
        return value == null ? name : value.trim();
    }

    /** Returns the value of the specified property name from the specified resource/properties file corresponding
     * to the given locale and replacing argument place holders with the given arguments using the MessageFormat class
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @param locale The locale that the given resource will correspond to
     * @param arguments An array of Objects to insert into the message argument place holders
     * @return The value of the property in the properties file
     */
    public static String getMessage(String resource, String name, Object[] arguments, Locale locale) {
        String value = getMessage(resource, name, locale);

        if (UtilValidate.isEmpty(value)) {
            return "";
        } else {
            if (arguments != null && arguments.length > 0) {
                value = MessageFormat.format(value, arguments);
            }
            return value;
        }
    }

    /** Returns the value of the specified property name from the specified resource/properties file corresponding
     * to the given locale and replacing argument place holders with the given arguments using the MessageFormat class
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @param locale The locale that the given resource will correspond to
     * @param arguments A List of Objects to insert into the message argument place holders
     * @return The value of the property in the properties file
     */
    public static <E> String getMessage(String resource, String name, List<E> arguments, Locale locale) {
        String value = getMessage(resource, name, locale);

        if (UtilValidate.isEmpty(value)) {
            return "";
        } else {
            if (UtilValidate.isNotEmpty(arguments)) {
                value = MessageFormat.format(value, arguments.toArray());
            }
            return value;
        }
    }

    public static String getMessageList(String resource, String name, Locale locale, Object... arguments) {
        return getMessage(resource, name, arguments, locale);
    }

    /** Returns the value of the specified property name from the specified resource/properties file corresponding
     * to the given locale and replacing argument place holders with the given arguments using the FlexibleStringExpander class
     * @param resource The name of the resource - can be a file, class, or URL
     * @param name The name of the property in the properties file
     * @param locale The locale that the given resource will correspond to
     * @param context A Map of Objects to insert into the message place holders using the ${} syntax of the FlexibleStringExpander
     * @return The value of the property in the properties file
     */
    public static String getMessage(String resource, String name, Map<String, ? extends Object> context, Locale locale) {
        String value = getMessage(resource, name, locale);

        if (UtilValidate.isEmpty(value)) {
            return "";
        } else {
            if (UtilValidate.isNotEmpty(context)) {
                value = FlexibleStringExpander.expandString(value, context, locale);
            }
            return value;
        }
    }

    public static String getMessageMap(String resource, String name, Locale locale, Object... context) {
        return getMessage(resource, name, UtilGenerics.toMap(String.class, context), locale);
    }

    protected static Set<String> resourceNotFoundMessagesShown = FastSet.newInstance();
    /** Returns the specified resource/properties file as a ResourceBundle
     * @param resource The name of the resource - can be a file, class, or URL
     * @param locale The locale that the given resource will correspond to
     * @return The ResourceBundle
     */
    public static ResourceBundle getResourceBundle(String resource, Locale locale) {
        if (UtilValidate.isEmpty(resource)) {
            throw new IllegalArgumentException("resource cannot be null or empty");
        }
        if (locale == null) {
            throw new IllegalArgumentException("locale cannot be null");
        }
        ResourceBundle bundle = null;
        try {
            bundle = UtilResourceBundle.getBundle(resource, locale, (ClassLoader) null);
        } catch (MissingResourceException e) {
            String resourceCacheKey = createResourceName(resource, locale, false);
            if (!resourceNotFoundMessagesShown.contains(resourceCacheKey)) {
                resourceNotFoundMessagesShown.add(resourceCacheKey);
                logger.info("[UtilProperties.getPropertyValue] could not find resource: " + resource + " for locale " + locale);
            }
            throw new IllegalArgumentException("Could not find resource bundle [" + resource + "] in the locale [" + locale + "]");
        }
        return bundle;
    }

    /** Returns the specified resource/properties file as a Map with the original
     *  ResourceBundle in the Map under the key _RESOURCE_BUNDLE_
     * @param resource The name of the resource - can be a file, class, or URL
     * @param locale The locale that the given resource will correspond to
     * @return Map containing all entries in The ResourceBundle
     */
    public static ResourceBundleMapWrapper getResourceBundleMap(String resource, Locale locale) {
        return new ResourceBundleMapWrapper(getResourceBundle(resource, locale));
    }

    /** Returns the specified resource/properties file as a Map with the original
     *  ResourceBundle in the Map under the key _RESOURCE_BUNDLE_
     * @param resource The name of the resource - can be a file, class, or URL
     * @param locale The locale that the given resource will correspond to
     * @param context The screen rendering context
     * @return Map containing all entries in The ResourceBundle
     */
    public static ResourceBundleMapWrapper getResourceBundleMap(String resource, Locale locale, Map<String, Object> context) {
        return new ResourceBundleMapWrapper(getResourceBundle(resource, locale), context);
    }

    /** Returns the specified resource/properties file.<p>Note that this method
     * will return a Properties instance for the specified locale <em>only</em> -
     * if you need <a href="http://www.w3.org/International/">I18n</a> properties, then use
     * <a href="#getResourceBundle(java.lang.String,%20java.util.Locale)">
     * getResourceBundle(String resource, Locale locale)</a>. This method is
     * intended to be used primarily by the UtilProperties class.</p>
     * @param resource The name of the resource - can be a file, class, or URL
     * @param locale The desired locale
     * @return The Properties instance, or null if no matching properties are found
     */
    public static Properties getProperties(String resource, Locale locale) {
        if (UtilValidate.isEmpty(resource)) {
            throw new IllegalArgumentException("resource cannot be null or empty");
        }
        if (locale == null) {
            throw new IllegalArgumentException("locale cannot be null");
        }
        Properties properties = null;
        URL url = resolvePropertiesUrl(resource, locale);
        if (url != null) {
            try {
                properties = new ExtendedProperties(url, locale);
            } catch (Exception e) {
                if (UtilValidate.isNotEmpty(e.getMessage())) {
                  logger.error(e.getMessage(), e);
                } else {
                    logger.error("Exception thrown: " + e.getClass().getName());
                }
                properties = null;
            }
        }
        if (UtilValidate.isNotEmpty(properties)) {
            if (logger.isDebugEnabled()) logger.debug("Loaded " + properties.size() + " properties for: " + resource + " (" + locale + ")");
        }
        return properties;
    }

    // ========= Classes and Methods for expanded Properties file support ========== //

    /** Returns the configured fallback locale. UtilProperties uses this locale
     * to resolve locale-specific XML properties.<p>The fallback locale can be
     * configured using the <code>locale.properties.fallback</code> property in
     * <code>general.properties</code>.
     * @return The configured fallback locale
     * @deprecated Use <code>java.util.ResourceBundle.Control.getFallbackLocale(...)</code>
     */
    @Deprecated
    public static Locale getFallbackLocale() {
        if (fallbackLocale == null) {
            synchronized (UtilProperties.class) {
                if (fallbackLocale == null) {
                    String locale = getPropertyValue("general", "locale.properties.fallback");
                    if (UtilValidate.isNotEmpty(locale)) {
                        fallbackLocale = UtilMisc.parseLocale(locale);
                    }
                    if (fallbackLocale == null) {
                        fallbackLocale = UtilMisc.parseLocale("en");
                    }
                }
            }
        }
        return fallbackLocale;
    }

    /** Converts a Locale instance to a candidate Locale list. The list
     * is ordered most-specific to least-specific. Example:
     * <code>localeToCandidateList(Locale.US)</code> would return
     * a list containing <code>en_US</code> and <code>en</code>.
     * @return A list of candidate locales.
     */
    public static List<Locale> localeToCandidateList(Locale locale) {
        List<Locale> localeList = FastList.newInstance();
        localeList.add(locale);
        String localeString = locale.toString();
        int pos = localeString.lastIndexOf("_", localeString.length());
        while (pos != -1) {
            localeString = localeString.substring(0, pos);
            localeList.add(new Locale(localeString));
            pos = localeString.lastIndexOf("_", localeString.length());
        }
        return localeList;
    }

    /** Returns the default candidate Locale list. The list is populated
     * with the JVM's default locale, the OFBiz fallback locale, and
     * the <code>LOCALE_ROOT</code> (empty) locale - in that order.
     * @return A list of default candidate locales.
     */
    public static Set<Locale> getDefaultCandidateLocales() {
        if (defaultCandidateLocales == null) {
            synchronized (UtilProperties.class) {
                if (defaultCandidateLocales == null) {
                    defaultCandidateLocales = FastSet.newInstance();
                    defaultCandidateLocales.addAll(localeToCandidateList(Locale.getDefault()));
                    defaultCandidateLocales.addAll(localeToCandidateList(getFallbackLocale()));
                    defaultCandidateLocales.add(Locale.ROOT);
                }
            }
        }
        return defaultCandidateLocales;
    }

    /** Returns a list of candidate locales based on a supplied locale.
     * The returned list consists of the supplied locale and the
     * <a href="#getDefaultCandidateLocales()">default candidate locales</a>
     * - in that order.
     * @param locale The desired locale
     * @return A list of candidate locales
     * @deprecated Use <code>java.util.ResourceBundle.Control.getCandidateLocales(...)</code>
     */
    @Deprecated
    public static List<Locale> getCandidateLocales(Locale locale) {
        // Java 6 conformance
        if (Locale.ROOT.equals(locale)) {
            return UtilMisc.toList(locale);
        }
        Set<Locale> localeSet = FastSet.newInstance();
        localeSet.addAll(localeToCandidateList(locale));
        localeSet.addAll(getDefaultCandidateLocales());
        List<Locale> localeList = FastList.newInstance();
        localeList.addAll(localeSet);
        return localeList;
    }

    /** Create a localized resource name based on a resource name and
     * a locale.
     * @param resource The desired resource
     * @param locale The desired locale
     * @param removeExtension Remove file extension from resource String
     * @return Localized resource name
     */
    public static String createResourceName(String resource, Locale locale, boolean removeExtension) {
        String resourceName = resource;
        if (removeExtension) {
            if (resourceName.endsWith(".xml")) {
                resourceName = resourceName.replace(".xml", "");
            } else if (resourceName.endsWith(".properties")) {
                resourceName = resourceName.replace(".properties", "");
            }
        }
        if (locale != null) {
            if (UtilValidate.isNotEmpty(locale.toString())) {
                resourceName = resourceName + "_" + locale;
            }
        }
        return resourceName;
    }

    public static boolean isPropertiesResourceNotFound(String resource, Locale locale, boolean removeExtension) {
        return propertiesNotFound.contains(UtilProperties.createResourceName(resource, locale, removeExtension));
    }

    /** Resolve a properties file URL.
     * <p>This method uses the following strategy:<br />
     * <ul>
     * <li>Locate the XML file specified in <code>resource (MyProps.xml)</code></li>
     * <li>Locate the file that starts with the name specified in
     * <code>resource</code> and ends with the locale's string and
     * <code>.xml (MyProps_en.xml)</code></li>
     * <li>Locate the file that starts with the name specified in
     * <code>resource</code> and ends with the locale's string and
     * <code>.properties (MyProps_en.properties)</code></li>
     * <li>Locate the file that starts with the name specified in
     * <code>resource and ends with the locale's string (MyProps_en)</code></li>
     * </ul>
     * <br />
     * The <code>component://</code> protocol is supported in the
     * <code>resource</code> parameter.
     * </p>
     *
     * @param resource The resource to resolve
     * @param locale The desired locale
     * @return A URL instance or null if not found.
     */
    public static URL resolvePropertiesUrl(String resource, Locale locale) {
        if (UtilValidate.isEmpty(resource)) {
            throw new IllegalArgumentException("resource cannot be null or empty");
        }
        String resourceName = createResourceName(resource, locale, false);
        if (propertiesNotFound.contains(resourceName)) {
            return null;
        }
        URL url = null;
        try {
            // Check for complete URL first
            if (resource.endsWith(".xml") || resource.endsWith(".properties")) {
                url = FlexibleLocation.resolveLocation(resource);
                if (url != null) {
                    return url;
                }
            }
            // Check for *.properties file
            url = FlexibleLocation.resolveLocation(resourceName + ".properties");
            if (url != null) {
                return url;
            }
            // Check for Java XML properties file
            url = FlexibleLocation.resolveLocation(resourceName + ".xml");
            if (url != null) {
                return url;
            }
            // Check for Custom XML properties file
            url = FlexibleLocation.resolveLocation(resource + ".xml");
            if (url != null) {
                return url;
            }
            url = FlexibleLocation.resolveLocation(resourceName);
            if (url != null) {
                return url;
            }
        } catch (Exception e) {
            logger.info("Properties resolver: invalid URL - " + e.getMessage());
        }
        if (propertiesNotFound.size() <= 300) {
            // Sanity check - list could get quite large
            propertiesNotFound.add(resourceName);
        }
        return null;
    }

    /** Convert XML property file to Properties instance. This method will convert
     * both the Java XML properties file format and the OFBiz custom XML
     * properties file format.
     * <p>
     * The format of the custom XML properties file is:<br />
     * <br />
     * <code>
     * &lt;resource&gt;<br />
     * &nbsp;&lt;property key="key"&gt;<br />
     * &nbsp;&nbsp;&lt;value xml:lang="locale 1"&gt;Some value&lt;/value&gt<br />
     * &nbsp;&nbsp;&lt;value xml:lang="locale 2"&gt;Some value&lt;/value&gt<br />
     * &nbsp;&nbsp;...<br />
     * &nbsp;&lt;/property&gt;<br />
     * &nbsp;...<br />
     * &lt;/resource&gt;<br /><br /></code> where <em>"locale 1", "locale 2"</em> are valid Locale strings.
     * </p>
     *
     * @param in XML file InputStream
     * @param locale The desired locale
     * @param properties Optional Properties object to populate
     * @return Properties instance or null if not found
     */
    public static Properties xmlToProperties(InputStream in, Locale locale, Properties properties) throws IOException, InvalidPropertiesFormatException {
        if (in == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        Document doc = null;
        try {
            // set validation true when we have a DTD for the custom XML format
            doc = UtilXml.readXmlDocument(in, false, "XML Properties file");
            in.close();
        } catch (Exception e) {
            logger.warn("XML Locale file for locale " + locale + " could not be loaded.", e);
            in.close();
            return null;
        }
        Element resourceElement = doc.getDocumentElement();
        List<? extends Element> propertyList = UtilXml.childElementList(resourceElement, "property");
        if (UtilValidate.isNotEmpty(propertyList)) {
            // Custom XML properties file format
            if (locale == null) {
                throw new IllegalArgumentException("locale cannot be null");
            }
            String localeString = locale.toString();
            for (Element property : propertyList) {
                Element value = UtilXml.firstChildElement(property, "value", "xml:lang", localeString);
                if (value != null) {
                    if (properties == null) {
                        properties = new Properties();
                    }
                    String valueString = UtilXml.elementValue(value);
                    if (valueString != null) {
                        properties.put(property.getAttribute("key"), valueString);
                    }
                }
            }
            return properties;
        }
        propertyList = UtilXml.childElementList(resourceElement, "entry");
        if (UtilValidate.isEmpty(propertyList)) {
            throw new InvalidPropertiesFormatException("XML properties file invalid or empty");
        }
        // Java XML properties file format
        for (Element property : propertyList) {
            String value = UtilXml.elementValue(property);
            if (value != null) {
                if (properties == null) {
                    properties = new Properties();
                }
                properties.put(property.getAttribute("key"), value);
            }
        }
        return properties;
    }

    /** Custom ResourceBundle class. This class extends ResourceBundle
     * to add custom bundle caching code and support for the OFBiz custom XML
     * properties file format.
     */
    public static class UtilResourceBundle extends ResourceBundle {
        protected static UtilCache<String, UtilResourceBundle> bundleCache = UtilCache.createUtilCache("properties.UtilPropertiesBundleCache");
        protected Properties properties = null;
        protected Locale locale = null;
        protected int hashCode = hashCode();

        protected UtilResourceBundle() {}

        public UtilResourceBundle(Properties properties, Locale locale, UtilResourceBundle parent) {
            this.properties = properties;
            this.locale = locale;
            setParent(parent);
            String hashString = properties.toString();
            if (parent != null) {
                hashString += parent.properties;
            }
            this.hashCode = hashString.hashCode();
        }

        public static ResourceBundle getBundle(String resource, Locale locale, ClassLoader loader) throws MissingResourceException {
            String resourceName = createResourceName(resource, locale, true);
            UtilResourceBundle bundle = bundleCache.get(resourceName);
            if (bundle == null) {
                synchronized (bundleCache) {
                    double startTime = System.currentTimeMillis();
                    FastList<Locale> candidateLocales = (FastList<Locale>) getCandidateLocales(locale);
                    UtilResourceBundle parentBundle = null;
                    int numProperties = 0;
                    while (candidateLocales.size() > 0) {
                        Locale candidateLocale = candidateLocales.removeLast();
                        // ResourceBundles are connected together as a singly-linked list
                        String lookupName = createResourceName(resource, candidateLocale, true);
                        UtilResourceBundle lookupBundle = bundleCache.get(lookupName);
                        if (lookupBundle == null) {
                            Properties newProps = getProperties(resource, candidateLocale);
                            if (UtilValidate.isNotEmpty(newProps)) {
                                // The last bundle we found becomes the parent of the new bundle
                                parentBundle = bundle;
                                bundle = new UtilResourceBundle(newProps, candidateLocale, parentBundle);
                                bundleCache.put(lookupName, bundle);
                                numProperties = newProps.size();
                            }
                        } else {
                            parentBundle = bundle;
                            bundle = lookupBundle;
                        }
                    }
                    if (bundle == null) {
                        throw new MissingResourceException("Resource " + resource + ", locale " + locale + " not found", null, null);
                    } else if (!bundle.getLocale().equals(locale)) {
                        // Create a "dummy" bundle for the requested locale
                        bundle = new UtilResourceBundle(bundle.properties, locale, parentBundle);
                    }
                    double totalTime = System.currentTimeMillis() - startTime;
                    Debug.logInfo("ResourceBundle " + resource + " (" + locale + ") created in " + totalTime/1000.0 + "s with " + numProperties + " properties", module);
                    bundleCache.put(resourceName, bundle);
                }
            }
            return bundle;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == null ? false : obj.hashCode() == this.hashCode;
        }

        @Override
        public Locale getLocale() {
            return this.locale;
        }

        @Override
        protected Object handleGetObject(String key) {
            return properties.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return new Enumeration<String>() {
                Iterator<String> i = UtilGenerics.cast(properties.keySet().iterator());
                public boolean hasMoreElements() {
                    return (i.hasNext());
                }
                public String nextElement() {
                    return i.next();
                }
            };
        }

    }

    /** Custom Properties class. Extended from Properties to add support
     * for the OFBiz custom XML file format.
     */
    public static class ExtendedProperties extends Properties {
        public ExtendedProperties() {
            super();
        }
        public ExtendedProperties(Properties defaults) {
            super(defaults);
        }
        public ExtendedProperties(URL url, Locale locale) throws IOException, InvalidPropertiesFormatException {
            InputStream in = new BufferedInputStream(url.openStream());
            if (url.getFile().endsWith(".xml")) {
                xmlToProperties(in, locale, this);
            } else {
                load(in);
            }
            in.close();
        }
        @Override
        public void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
            xmlToProperties(in, null, this);
            in.close();
        }
    }
}