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

import org.etk.common.logging.Logger;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Aug 26, 2011  
 */
public final class Configuration implements Cloneable {

  private static final Logger log = Logger.getLogger(Configuration.class);
  public static final String KERNEL_CONFIGURATION_1_0_URI = "http://www.exoplaform.org/xml/ns/kernel_1_0.xsd";
  
  /** The map which contains the Entities Map binding from entityDef.xml.*/
  private Map<String, Entity> entitiesMap = FastMap.newInstance();
  /** The map which contains the View Entities Map binding from entityDef.xml*/
  private Map<String, View> viewsMap = FastMap.newInstance();
  
  /**
   * Adds the Entity to the EntityMap which contains the Entity objects 
   * to configure in the entitydef.xml
   *   
   * @param object
   */
  public void addEntity(Object object) {
    Entity entity = (Entity) object;
    //TODO key = packageName + entityName;
    String key = "";
    entitiesMap.put(key, entity);
  }
  
  /**
   * Adds the View Entity to the ViewMap which contains the View objects 
   * to configure in the entitydef.xml
   *   
   * @param object
   */
  public void addView(Object object) {
    View entityView = (View) object;
    //TODO key = packageName + viewName;
    String key = "";
    viewsMap.put(key, entityView);
    
    //need to processing when view relate to provided Entity.
    
  }
  
  public boolean hasEntity() {
    return entitiesMap.size() > 0;
  }
  
  public boolean hasView() {
    return viewsMap.size() > 0;
  }
  
  /**
   * Gets the Entity Iterator for Binding.xml mapping
   * @return
   */
  public Iterator getEntityIterator() {
    return this.entitiesMap.values().iterator();
  }
  
  /**
   * Gets the View Iterator for Binding.xml mapping
   * @return
   */
  public Iterator getViewIterator() {
    return this.viewsMap.values().iterator();
  }
  
}
