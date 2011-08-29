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

import java.net.URL;
import java.util.List;

import org.etk.kernel.container.configuration.ConfigurationManagerImpl;
import org.jibx.runtime.IMarshallingContext;

import javolution.util.FastList;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          thanhvucong.78@exoplatform.com
 * Aug 26, 2011  
 */
public class Entity implements Comparable<Entity> {

  private URL documentURL;
  
  private List<Field> fields = FastList.newInstance();
  
  private List<Field> pkgs = FastList.newInstance();
  
  private List<Field> nopkg = FastList.newInstance();
  
  private List<Index> indexes = FastList.newInstance();
  
  private List<Relation> relations = FastList.newInstance();
  
  /** The entity-name that defined this Entity. */
  protected String entityName = "";
  /** The table-name of the Entity */
  protected String tableName = "";
  /** The package-name of the Entity */
  protected String packageName = "";
  /** The description of the Entity */
  protected String description = "";
  
  /**
   * The entity-name of the Entity that this Entity is dependent on,
   *  If empty then no dependency
   */
  protected String dependentOn = "";
  
  protected boolean neverCache = false;
  protected boolean neverCheck = false;
  protected boolean autoClearCache = true;
  protected Boolean hasFieldWithAuditLog = null;
  
  protected int priority = 0;
  
  public Entity() {
    documentURL = ConfigurationManagerImpl.getCurrentURL();
  }
  
  public URL getDocumentURL() {
    return documentURL;
  }

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
  
  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public int compareTo(Entity o) {
    return getPriority() - o.getPriority();
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDependentOn() {
    return dependentOn;
  }

  public void setDependentOn(String dependentOn) {
    this.dependentOn = dependentOn;
  }

  public boolean isNeverCache() {
    return neverCache;
  }

  public void setNeverCache(boolean neverCache) {
    this.neverCache = neverCache;
  }

  public boolean isNeverCheck() {
    return neverCheck;
  }

  public void setNeverCheck(boolean neverCheck) {
    this.neverCheck = neverCheck;
  }

  public boolean isAutoClearCache() {
    return autoClearCache;
  }

  public void setAutoClearCache(boolean autoClearCache) {
    this.autoClearCache = autoClearCache;
  }

  public Boolean getHasFieldWithAuditLog() {
    return hasFieldWithAuditLog;
  }

  public void setHasFieldWithAuditLog(Boolean hasFieldWithAuditLog) {
    this.hasFieldWithAuditLog = hasFieldWithAuditLog;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  public void setPkgs(List<Field> pkgs) {
    this.pkgs = pkgs;
  }

  public void setNopkg(List<Field> nopkg) {
    this.nopkg = nopkg;
  }

  public void setIndexes(List<Index> indexes) {
    this.indexes = indexes;
  }

  public void setRelations(List<Relation> relations) {
    this.relations = relations;
  }
  
  public void preGet(IMarshallingContext ictx) {
    ConfigurationMarshallerUtil.addURLToContent(documentURL, ictx);
}   
  
}
