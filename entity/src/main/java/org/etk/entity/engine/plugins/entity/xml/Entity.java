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
package org.etk.entity.engine.plugins.entity.xml;

import java.util.List;

import javolution.util.FastList;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Aug 26, 2011  
 */
public class Entity {

  private List<Field> fields = FastList.newInstance();
  
  private List<Field> pkgs = FastList.newInstance();
  
  private List<Field> nopkg = FastList.newInstance();
  
  private List<Index> indexes = FastList.newInstance();
  
  private List<Relation> relations = FastList.newInstance();
  
  /**
   * Adds the field for the Entity.
   * @param object
   */
  public void addField(Object object) {
    Field field = (Field) object;
    fields.add(field);
  }
  
  /**
   * Adds the field for the Entity.
   * @param object
   */
  public void addPrimaryKey(Object object) {
    Field field = (Field) object;
    pkgs.add(field);
  }
  
  /**
   * Adds the Index for the Entity.
   * @param object
   */
  public void addIndex(Object object) {
    Index index = (Index) object;
    indexes.add(index);
  }
  
  /**
   * Adds the Relation for the Entity.
   * @param object
   */
  public void addRelation(Object object) {
    Relation relation = (Relation) object;
    relations.add(relation);
  }

  public List<Field> getFields() {
    return fields;
  }

  public List<Field> getPkgs() {
    return pkgs;
  }

  public List<Field> getNopkg() {
    return nopkg;
  }

  public List<Index> getIndexes() {
    return indexes;
  }

  public List<Relation> getRelations() {
    return relations;
  }
}
