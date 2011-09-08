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
package org.etk.entity.engine.plugins.jdbc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 8, 2011  
 */
public abstract class AbstractCursorHandler implements InvocationHandler {

  protected String cursorName;
  protected int fetchSize;

  protected AbstractCursorHandler(String cursorName, int fetchSize) {
      this.cursorName = cursorName;
      this.fetchSize = fetchSize;
  }

  public void setCursorName(String cursorName) {
      this.cursorName = cursorName;
  }

  public String getCursorName() {
      return cursorName;
  }

  public void setFetchSize(int fetchSize) {
      this.fetchSize = fetchSize;
  }

  public int getFetchSize() {
      return fetchSize;
  }

  protected Object invoke(Object obj, Object proxy, Method method, Object... args) throws Throwable {
      if ("toString".equals(method.getName())) {
          String str = obj.toString();
          return getClass().getName() + "{" + str + "}";
      }
      return method.invoke(obj, args);
  }

  protected static <T> T newHandler(InvocationHandler handler, Class<T> implClass) throws IllegalAccessException, IllegalArgumentException, InstantiationException, InvocationTargetException, NoSuchMethodException, SecurityException {
      ClassLoader loader = implClass.getClassLoader();
      if (loader == null) loader = ClassLoader.getSystemClassLoader();
      Class<?> proxyClass = Proxy.getProxyClass(loader, implClass);
      Constructor<?> constructor = proxyClass.getConstructor(InvocationHandler.class);
      return implClass.cast(constructor.newInstance(handler));
  }
}