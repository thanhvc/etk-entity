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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.etk.common.logging.Logger;
import org.etk.entity.base.lang.Factory;
import org.etk.entity.base.lang.SourceMonitored;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Sep
 * 6, 2011
 */
@SourceMonitored
public final class UtilObject {
  private UtilObject() {
  }

  private static final Logger logger = Logger.getLogger(UtilObject.class);

  public static byte[] getBytes(InputStream is) {
    byte[] buffer = new byte[4 * 1024];
    byte[] data = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {

        int numBytesRead;
        while ((numBytesRead = is.read(buffer)) != -1) {
          bos.write(buffer, 0, numBytesRead);
        }
        data = bos.toByteArray();
      } finally {
        bos.close();
      }
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }
    }

    return data;
  }

  /** Serialize an object to a byte array */
  public static byte[] getBytes(Object obj) {
    byte[] data = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        try {
          oos.writeObject(obj);
          data = bos.toByteArray();
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        } finally {
          oos.flush();
          oos.close();
        }
      } catch (IOException e) {
        // I don't know how to force an error during flush or
        // close of ObjectOutputStream; since OOS is wrapping
        // BAOS, and BAOS does not throw IOException during
        // write, I don't think this can happen.
        logger.error(e.getMessage(), e);
      } finally {
        bos.close();
      }
    } catch (IOException e) {
      // How could this ever happen? BAOS.close() is listed as
      // throwing the exception, but I don't understand why this
      // is.
      logger.error(e.getMessage(), e);
    }

    return data;
  }

  /**
   * Returns the size of a serializable object. Non-serializable objects will
   * throw an <code>IOException</code>.
   * <p>
   * It is important to note that the returned value is length of the byte
   * stream after the object has been serialized. The returned value does not
   * represent the amount of memory the object uses. There is no accurate way to
   * determine the size of an object in memory.
   * </p>
   * 
   * @param obj
   * @return the number of bytes in the serialized object
   * @throws IOException
   */
  public static long getByteCount(Object obj) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(obj);
    oos.flush();
    long size = bos.size();
    bos.close();
    return size;
  }

  /** Deserialize a byte array back to an object */
  public static Object getObject(byte[] bytes) {
    Object obj = null;
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      try {
        ObjectInputStream ois = new ObjectInputStream(bis);
        try {
          obj = ois.readObject();
        } catch (ClassNotFoundException e) {
          logger.error(e.getMessage(), e);
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        } finally {
          ois.close();
        }
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      } finally {
        bis.close();
      }
    } catch (IOException e) {
      // How could this ever happen? BAIS.close() is listed as
      // throwing the exception, but I don't understand why this
      // is.
      logger.error(e.getMessage(), e);
    }
    return obj;
  }

  public static boolean equalsHelper(Object o1, Object o2) {
    if (o1 == o2) {
      // handles same-reference, or null
      return true;
    } else if (o1 == null || o2 == null) {
      // either o1 or o2 is null, but not both
      return false;
    } else {
      return o1.equals(o2);
    }
  }

  public static <T> int compareToHelper(Comparable<T> o1, T o2) {
    if (o1 == o2) {
      // handles same-reference, or null
      return 0;
    } else if (o1 == null) {
      return -1;
    } else if (o2 == null) {
      // either o1 or o2 is null, but not both
      return 1;
    } else {
      return o1.compareTo(o2);
    }
  }

  public static int doHashCode(Object o1) {
    if (o1 == null)
      return 0;
    if (o1.getClass().isArray()) {
      int length = Array.getLength(o1);
      int result = 0;
      for (int i = 0; i < length; i++) {
        result += doHashCode(Array.get(o1, i));
      }
      return result;
    }
    return o1.hashCode();
  }

  public static <A, R> R getObjectFromFactory(Class<? extends Factory<R, A>> factoryInterface, A obj) throws ClassNotFoundException {
    Iterator<? extends Factory<R, A>> it = ServiceLoader.load(factoryInterface).iterator();
    while (it.hasNext()) {
      Factory<R, A> factory = it.next();
      R instance = factory.getInstance(obj);
      if (instance != null) {
        return instance;
      }
    }
    throw new ClassNotFoundException(factoryInterface.getClass().getName());
  }
}
