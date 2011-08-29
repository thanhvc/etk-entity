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
package org.etk.entity.engine.plugins.model.metadata;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;

/**
 * Author : ThanhVuCong
 *          thanhvucong.78@gmail.com
 * Aug 26, 2011  
 */
public class EntityMeta extends EntityInfo implements Comparable<EntityMeta>, Serializable {

  public static final String  module = EntityMeta.class.getName();
  
  /** The name of the time stamp field for locking/synchronization */
  public static final String STAMP_FIELD = "lastUpdatedStamp";
  public static final String STAMP_TX_FIELD = "lastUpdatedTxStamp";
  public static final String CREATE_STAMP_FIELD = "createdStamp";
  public static final String CREATE_STAMP_TX_FIELD = "createdTxStamp";
  /**
   * The entity-name that defined this Entity.
   */
  protected String entityName = "";
  /**
   * The table-name of the Entity
   */
  protected String tableName = "";
  /**
   * The package-name of the Entity
   */
  protected String packageName = "";
  
  /**
   * The entity-name of the Entity that this Entity is dependent on,
   *  If empty then no dependency
   */
  protected String dependentOn = "";
  
  protected Integer sequenceBankSize = null;
  
  /**A List of the Field objects for the Entity*/
  protected List<EntityFieldMeta> fields = FastList.newInstance();
  protected Map<String, EntityFieldMeta> fieldsMap = null;
  
  /**A List of the Field object for the Entity, one for each PrimaryKey*/
  protected List<EntityFieldMeta> pks = FastList.newInstance();
  
  /** A List of the Field objects for the Entity, one for each NON Primary Key */
  protected List<EntityFieldMeta> nopks = FastList.newInstance();
  /**relations defining relationships between this entity and other entities */
  protected List<EntityRelationMeta> relations = FastList.newInstance();
  
  /**indexes on fields/columns in this Entity*/
  protected List<EntityIndexMeta> indexes = FastList.newInstance();
  
  /**The reference of the dependentOn entity*/
  protected List<EntityMeta> specializationOfEntityMeta = null;
  
  /**The list of entities that are specialization of on this entity*/
  protected Set<String> viewEntities = FastSet.newInstance();
  
  /** An indicator to specify if this entity requires locking for updates*/
  protected boolean doLock = false;
  
  /**Can be used to disable automatically creating update stamp fields and population them on inserts and updateds*/
  protected boolean noAutoStamp = false;
  /**
   * An indicator to specify if this entity is never cached.
   * If true causes the delegator to not clear cachs on write and to not get
   * from cache on read showing a warning message to that effect.
   */
  protected boolean neverCache = false;
  protected boolean neverCheck = false;
  protected boolean autoClearCache = true;
  protected Boolean hasFieldWithAuditLog = null;
  
  @Override
  public int compareTo(EntityMeta o) {
    // TODO Auto-generated method stub
    return 0;
  }

}
