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
package org.etk.entity.engine.plugins.model.xml;

import java.util.Iterator;
import java.util.Map;

import javolution.util.FastMap;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Aug 29, 2011  
 */
public class PKField {

  private Map<String, Field> pkMap = FastMap.newInstance();
  
  /**
   * Adds the Field to the FieldMap which contains the Field objects 
   * to configure in the entitydef.xml
   *   
   * @param object
   */
  public void addField(Object object) {
    Field field = (Field) object;
    pkMap.put(field.getName(), field);
  }
  
  public boolean hasPK() {
    return pkMap.size() > 0;
  }
  
  /**
   * Gets the PrimaryKey Iterator for binding.xml mapping
   * @return
   */
  public Iterator getFieldIterator() {
    return this.pkMap.values().iterator();
  }
}
