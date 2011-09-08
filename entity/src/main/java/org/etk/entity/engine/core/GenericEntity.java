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
package org.etk.entity.engine.core;

import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.ResourceBundle;
import java.util.TreeSet;

import org.etk.common.logging.Logger;
import org.etk.entity.base.collections.LocalizedMap;
import org.etk.entity.base.utils.GeneralException;
import org.etk.entity.base.utils.ObjectType;
import org.etk.entity.base.utils.TimeDuration;
import org.etk.entity.base.utils.UtilDateTime;
import org.etk.entity.base.utils.UtilGenerics;
import org.etk.entity.base.utils.UtilProperties;
import org.etk.entity.base.utils.UtilValidate;
import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.plugins.condition.EntityCondition;
import org.etk.entity.engine.plugins.jdbc.SqlJdbcUtil;
import org.etk.entity.engine.plugins.model.xml.Entity;
import org.etk.entity.engine.plugins.model.xml.Field;
import org.etk.entity.engine.plugins.model.xml.FieldType;
import org.etk.entity.engine.plugins.model.xml.ViewEntity;

import javolution.lang.Reusable;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * Generic Entity Value Object - Handles persistence for any defined entity.
 * <p>
 * Note that this class extends<code>Observable</code> to achieve change
 * notification for <code>Observer</code>s. Whenever a field changes the name of
 * the field will be passes to the <code>notifyObsersers()</code> method, and
 * through that the <code>update()</code> method of each <code>Observer</code>
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Sep
 * 6, 2011
 */
public class GenericEntity extends Observable implements Map<String, Object>, LocalizedMap<Object>,
    Serializable, Comparable<GenericEntity>, Cloneable, Reusable {
  private static Logger             logger            = Logger.getLogger(GenericEntity.class);

  public static final GenericEntity NULL_ENTITY       = new NullGenericEntity();

  public static final NullField     NULL_FIELD        = new NullField();

  /**
   * Name of the GenericDelegator, used to re-get the GenericDelegator when
   * deserialized
   */
  protected String                  delegatorName     = null;

  protected transient Delegator     internalDelegator = null;

  protected Map<String, Object>     fields            = FastMap.newInstance();

  protected String                  entityName        = null;

  protected transient Entity        entity            = null;

  /**
   * Denotes whether or not this entity has been modified, or it known to be out
   * of sync with the persistent record
   */
  protected boolean                 modified          = false;

  protected boolean                 generateHashCode  = true;

  protected int                     cachedHashCode    = 0;

  /**
   * Used to specify whether or not this representation of the entity can be
   * changed; generally cleared when this object comes from a cache
   */
  protected boolean                 mutable           = true;

  /**
   * This is an internal field used to specify that a value has come from a sync
   * process and that the auto-stamps should not be over-written
   */
  protected boolean                 isFromEntitySync  = false;

  protected GenericEntity() {
  }

  public static GenericEntity createGenericEntity(Entity entity) {
    if (entity == null) {
      throw new IllegalArgumentException("Cannot create a GenericEntity with a null modelEntity parameter");
    }

    GenericEntity newEntity = new GenericEntity();
    newEntity.init(entity);
    return newEntity;
  }

  /** Creates new GenericEntity */
  protected void init(Entity entity) {
    if (entity == null) {
      throw new IllegalArgumentException("Cannot create a GenericEntity with a null modelEntity parameter");
    }

    this.entity = entity;
    this.entityName = entity.getEntityName();

    // check some things
    if (this.entityName == null) {
      throw new IllegalArgumentException("Cannot create a GenericEntity with a null entityName in the Entity parameter.");
    }
  }

  protected void init(Delegator delegator, Entity entity, Map<String, ? extends Object> fields) {
    if (entity == null) {
      throw new IllegalArgumentException("Cannot create a GenericEntity with a null modelEntity parameter");
    }
    this.entity = entity;
    this.entityName = entity.getEntityName();
    this.delegatorName = delegator.getDelegatorName();
    this.internalDelegator = delegator;
    setFields(fields);

    // check some things
    if (this.entityName == null) {
      throw new IllegalArgumentException("Cannot create a GenericEntity with a null entityName in the modelEntity parameter");
    }
  }

  /** Creates new GenericEntity from existing Map */
  protected void init(Delegator delegator, Entity entity, Object singlePkValue) {
    if (entity == null) {
      throw new IllegalArgumentException("Cannot create a GenericEntity with a null modelEntity parameter");
    }
    if (entity.getPksSize() != 1) {
      throw new IllegalArgumentException("Cannot create a GenericEntity with more than one primary key field");
    }
    this.entity = entity;
    this.entityName = entity.getEntityName();
    this.delegatorName = delegator.getDelegatorName();
    this.internalDelegator = delegator;
    set(entity.getOnlyPk().getName(), singlePkValue);

    // check some things
    if (this.entityName == null) {
      throw new IllegalArgumentException("Cannot create a GenericEntity with a null entityName in the modelEntity parameter");
    }
  }

  /** Copy Constructor: Creates new GenericEntity from existing GenericEntity */
  protected void init(GenericEntity value) {
    // check some things
    if (value.entityName == null) {
      throw new IllegalArgumentException("Cannot create a GenericEntity with a null entityName in the modelEntity parameter");
    }
    this.entityName = value.getEntityName();
    // NOTE: could call getModelEntity to insure we have a value, just in case
    // the value passed in has been serialized, but might as well leave it null
    // to keep the object light if it isn't there
    this.entity = value.entity;
    if (value.fields != null)
      this.fields.putAll(value.fields);
    this.delegatorName = value.delegatorName;
    this.internalDelegator = value.internalDelegator;
  }

  public void reset() {
    // from GenericEntity
    this.delegatorName = null;
    this.internalDelegator = null;
    this.fields = FastMap.newInstance();
    this.entityName = null;
    this.entity = null;
    this.modified = false;
    this.generateHashCode = true;
    this.cachedHashCode = 0;
    this.mutable = true;
    this.isFromEntitySync = false;
  }

  public void refreshFromValue(GenericEntity newValue) throws GenericEntityException {
    if (newValue == null) {
      throw new GenericEntityException("Could not refresh value, new value not found for: " + this);
    }
    GenericPK thisPK = this.getPrimaryKey();
    GenericPK newPK = newValue.getPrimaryKey();
    if (!thisPK.equals(newPK)) {
      throw new GenericEntityException("Could not refresh value, new value did not have the same primary key; this PK="
          + thisPK + ", new value PK=" + newPK);
    }
    this.fields = newValue.fields;
    this.setDelegator(newValue.getDelegator());
    this.generateHashCode = newValue.generateHashCode;
    this.cachedHashCode = newValue.cachedHashCode;
    this.modified = false;
  }

  public boolean isModified() {
    return this.modified;
  }

  public void synchronizedWithDatasource() {
    this.modified = false;
  }

  public void removedFromDatasource() {
    // seems kind of minimal, but should do for now...
    this.modified = true;
  }

  public boolean isMutable() {
    return this.mutable;
  }

  public void setImmutable() {
    this.mutable = false;
  }

  /**
   * @return Returns the isFromEntitySync.
   */
  public boolean getIsFromEntitySync() {
    return this.isFromEntitySync;
  }

  /**
   * @param isFromEntitySync The isFromEntitySync to set.
   */
  public void setIsFromEntitySync(boolean isFromEntitySync) {
    this.isFromEntitySync = isFromEntitySync;
  }

  public String getEntityName() {
    return entityName;
  }

  public Entity getModelEntity() {
    if (entity == null) {
      if (entityName != null)
        entity = this.getDelegator().getEntity(entityName);
      if (entity == null) {
        throw new IllegalStateException("[GenericEntity.getModelEntity] could not find modelEntity for entityName "
            + entityName);
      }
    }
    return entity;
  }

  /**
   * Get the GenericDelegator instance that created this value object and that
   * is responsible for it.
   * 
   * @return GenericDelegator object
   */
  public Delegator getDelegator() {
    if (internalDelegator == null) {
      if (delegatorName == null)
        delegatorName = "default";
      if (delegatorName != null)
        internalDelegator = DelegatorFactory.getDelegator(delegatorName);
      if (internalDelegator == null) {
        throw new IllegalStateException("[GenericEntity.getDelegator] could not find delegator with name "
            + delegatorName);
      }
    }
    return internalDelegator;
  }

  /**
   * Set the GenericDelegator instance that created this value object and that
   * is responsible for it.
   */
  public void setDelegator(Delegator internalDelegator) {
    if (internalDelegator == null)
      return;
    this.delegatorName = internalDelegator.getDelegatorName();
    this.internalDelegator = internalDelegator;
  }

  public Object get(String name) {
    if (getModelEntity().getField(name) == null) {
      logger.warn("The field name (or key) ["
          + name
          + "] is not valid for entity ["
          + this.getEntityName()
          + "], printing IllegalArgumentException instead of throwing it because Map interface specification does not allow throwing that exception.");
      return null;
    }
    return fields.get(name);
  }

  /**
   * Returns true if the entity contains all of the primary key fields, but NO
   * others.
   */
  public boolean isPrimaryKey() {
    return isPrimaryKey(false);
  }

  public boolean isPrimaryKey(boolean requireValue) {
    TreeSet<String> fieldKeys = new TreeSet<String>(this.fields.keySet());
    for (Field curPk : this.getModelEntity().getPkFieldsUnmodifiable()) {
      String fieldName = curPk.getName();
      if (requireValue) {
        if (this.fields.get(fieldName) == null)
          return false;
      } else {
        if (!this.fields.containsKey(fieldName))
          return false;
      }
      fieldKeys.remove(fieldName);
    }
    if (!fieldKeys.isEmpty())
      return false;
    return true;
  }

  /** Returns true if the entity contains all of the primary key fields. */
  public boolean containsPrimaryKey() {
    return containsPrimaryKey(false);
  }

  public boolean containsPrimaryKey(boolean requireValue) {
    // TreeSet fieldKeys = new TreeSet(fields.keySet());
    for (Field curPk : this.getModelEntity().getPkFieldsUnmodifiable()) {
      String fieldName = curPk.getName();
      if (requireValue) {
        if (this.fields.get(fieldName) == null)
          return false;
      } else {
        if (!this.fields.containsKey(fieldName))
          return false;
      }
    }
    return true;
  }

  public String getPkShortValueString() {
    StringBuilder sb = new StringBuilder();
    for (Field curPk : this.getModelEntity().getPkFieldsUnmodifiable()) {
      if (sb.length() > 0) {
        sb.append("::");
      }
      sb.append(this.get(curPk.getName()));
    }
    return sb.toString();
  }

  /**
   * Sets the named field to the passed value, even if the value is null
   * 
   * @param name The field name to set
   * @param value The value to set
   */
  public void set(String name, Object value) {
    set(name, value, true);
  }

  /**
   * Sets the named field to the passed value. If value is null, it is only set
   * if the setIfNull parameter is true. This is useful because an update will
   * only set values that are included in the HashMap and will store null values
   * in the HashMap to the datastore. If a value is not in the HashMap, it will
   * be left unmodified in the datastore.
   * 
   * @param name The field name to set
   * @param value The value to set
   * @param setIfNull Specifies whether or not to set the value if it is null
   */
  public synchronized Object set(String name, Object value, boolean setIfNull) {
    if (!this.mutable) {
      // comment this out to disable the mutable check
      throw new IllegalStateException("This object has been flagged as immutable (unchangeable), probably because it came from an Entity Engine cache. Cannot set a value in an immutable entity object.");
    }

    Field modelField = getModelEntity().getField(name);
    if (modelField == null) {
      throw new IllegalArgumentException("[GenericEntity.set] \"" + name + "\" is not a field of "
          + entityName + ", must be one of: " + getModelEntity().fieldNameString());
    }
    if (value != null || setIfNull) {
      FieldType type = null;
      try {
        type = getDelegator().getEntityFieldType(getModelEntity(), modelField.getType());
      } catch (GenericEntityException e) {
        logger.warn(e.getMessage(), e);
      }
      if (type == null) {
        throw new IllegalArgumentException("Type "
            + modelField.getType()
            + " not found for entity ["
            + this.getEntityName()
            + "]; probably because there is no datasource (helper) setup for the entity group that this entity is in: ["
            + this.getDelegator().getEntityGroupName(this.getEntityName()) + "]");
      }

      if (value instanceof Boolean) {
        // if this is a Boolean check to see if we should convert from an
        // indicator or just leave as is
        try {
          int fieldType = SqlJdbcUtil.getType(type.getJavaType());
          if (fieldType != 10) {
            value = ((Boolean) value).booleanValue() ? "Y" : "N";
          }
        } catch (GenericNotImplementedException e) {
          throw new IllegalArgumentException(e.getMessage());
        }
      } else if (value != null && !(value instanceof NULL)) {
        // make sure the type matches the field Java type
        if (value instanceof TimeDuration) {
          try {
            value = ObjectType.simpleTypeConvert(value, type.getJavaType(), null, null);
          } catch (GeneralException e) {
          }
        }
        if (!ObjectType.instanceOf(value, type.getJavaType())) {
          if (!("java.sql.Blob".equals(type.getJavaType()) && (value instanceof byte[] || ObjectType.instanceOf(value,
                                                                                                                ByteBuffer.class)))) {
            String errMsg = "In entity field [" + this.getEntityName() + "." + name
                + "] set the value passed in [" + value.getClass().getName()
                + "] is not compatible with the Java type of the field [" + type.getJavaType()
                + "]";
            // eventually we should do this, but for now we'll do a "soft"
            // failure: throw new IllegalArgumentException(errMsg);
            logger.warn("=-=-=-=-=-=-=-=-= Database type warning GenericEntity.set =-=-=-=-=-=-=-=-= "
                            + errMsg,
                        new Exception("Location of database type warning"));
          }
        }
      }
      Object old = fields.put(name, value);

      generateHashCode = true;
      modified = true;
      this.setChanged();
      this.notifyObservers(name);
      return old;
    } else {
      return fields.get(name);
    }
  }

  public void dangerousSetNoCheckButFast(Field modelField, Object value) {
    if (modelField == null)
      throw new IllegalArgumentException("Cannot set field with a null modelField");
    generateHashCode = true;
    this.fields.put(modelField.getName(), value);
  }

  public Object dangerousGetNoCheckButFast(Field modelField) {
    if (modelField == null)
      throw new IllegalArgumentException("Cannot get field with a null modelField");
    return this.fields.get(modelField.getName());
  }

  /**
   * Sets the named field to the passed value, converting the value from a
   * String to the corrent type using <code>Type.valueOf()</code>
   * 
   * @param name The field name to set
   * @param value The String value to convert and set
   */
  public void setString(String name, String value) {
    if (value == null) {
      set(name, null);
      return;
    }

    boolean isNullString = false;
    if ("null".equals(value)) {
      // count this as a null too, but only for numbers and stuff, not for
      // Strings
      isNullString = true;
    }

    Field field = getModelEntity().getField(name);
    if (field == null)
      set(name, value); // this will get an error in the set() method...

    FieldType type = null;
    try {
      type = getDelegator().getEntityFieldType(getModelEntity(), field.getType());
    } catch (GenericEntityException e) {
      logger.warn(e.getMessage(), e);
    }
    if (type == null)
      throw new IllegalArgumentException("Type " + field.getType() + " not found");
    String fieldType = type.getJavaType();

    try {
      switch (SqlJdbcUtil.getType(fieldType)) {
      case 1:
        set(name, value);
        break;

      case 2:
        set(name, isNullString ? null : java.sql.Timestamp.valueOf(value));
        break;

      case 3:
        set(name, isNullString ? null : java.sql.Time.valueOf(value));
        break;

      case 4:
        set(name, isNullString ? null : java.sql.Date.valueOf(value));
        break;

      case 5:
        set(name, isNullString ? null : Integer.valueOf(value));
        break;

      case 6:
        set(name, isNullString ? null : Long.valueOf(value));
        break;

      case 7:
        set(name, isNullString ? null : Float.valueOf(value));
        break;

      case 8:
        set(name, isNullString ? null : Double.valueOf(value));
        break;

      case 9: // BigDecimal
        set(name, isNullString ? null : new BigDecimal(value));
        break;

      case 10:
        set(name, isNullString ? null : Boolean.valueOf(value));
        break;

      case 11: // Object
        set(name, value);
        break;

      case 12: // java.sql.Blob
        // TODO: any better way to handle Blob from String?
        set(name, value);
        break;

      case 13: // java.sql.Clob
        // TODO: any better way to handle Clob from String?
        set(name, value);
        break;

      case 14: // java.util.Date
        set(name, UtilDateTime.toDate(value));
        break;

      case 15: // java.util.Collection
        // TODO: how to convert from String to Collection? ie what should the
        // default behavior be?
        set(name, value);
        break;
      }
    } catch (GenericNotImplementedException ex) {
      throw new IllegalArgumentException(ex.getMessage());
    }
  }

  /**
   * Sets a field with an array of bytes, wrapping them automatically for easy
   * use.
   * 
   * @param name The field name to set
   * @param bytes The byte array to be wrapped and set
   */
  public void setBytes(String name, byte[] bytes) {
    this.set(name, bytes);
  }

  public void setNextSeqId() {
    List<String> pkFieldNameList = this.entity.getPkFieldNames();
    if (pkFieldNameList.size() != 1) {
      throw new IllegalArgumentException("Cannot setNextSeqId for entity [" + this.getEntityName()
          + "] that does not have a single primary key field, instead has ["
          + pkFieldNameList.size() + "]");
    }

    String pkFieldName = pkFieldNameList.get(0);
    if (this.get(pkFieldName) != null) {
      // don't throw exception, too much of a pain and usually intended: throw
      // new IllegalArgumentException("Cannot setNextSeqId, pk field [" +
      // pkFieldName + "] of entity [" + this.getEntityName() +
      // "] already has a value [" + this.get(pkFieldName) + "]");
    }

    String sequencedValue = this.getDelegator().getNextSeqId(this.getEntityName());
    this.set(pkFieldName, sequencedValue);
  }

  public Boolean getBoolean(String name) {
    Object obj = get(name);

    if (obj == null) {
      return null;
    }
    if (obj instanceof Boolean) {
      return (Boolean) obj;
    } else if (obj instanceof String) {
      String value = (String) obj;

      if ("Y".equalsIgnoreCase(value) || "T".equalsIgnoreCase(value)) {
        return Boolean.TRUE;
      } else if ("N".equalsIgnoreCase(value) || "F".equalsIgnoreCase(value)) {
        return Boolean.FALSE;
      } else {
        throw new IllegalArgumentException("getBoolean could not map the String '" + value
            + "' to Boolean type");
      }
    } else {
      throw new IllegalArgumentException("getBoolean could not map the object '" + obj.toString()
          + "' to Boolean type, unknown object type: " + obj.getClass().getName());
    }
  }

  /**
   * Returns the specified field as a <code>TimeDuration</code> instance. The
   * field's Java data type can be either <code>String</code> or
   * <code>Number</code>. Invalid Java data types will throw
   * <code>IllegalArgumentException</code>.
   * 
   * @param name The name of the desired field
   * @return A <code>TimeDuration</code> instance or <code>null</code>
   */
  public TimeDuration getDuration(String name) {
    Object obj = get(name);
    if (obj == null) {
      return null;
    }
    try {
      Number number = (Number) obj;
      return TimeDuration.fromNumber(number);
    } catch (Exception e) {
    }
    try {
      String duration = (String) obj;
      return TimeDuration.parseDuration(duration);
    } catch (Exception e) {
    }
    throw new IllegalArgumentException("getDuration could not map the object '" + obj.toString()
        + "' to TimeDuration type, incompatible object type: " + obj.getClass().getName());
  }

  public String getString(String name) {
    // might be nice to add some ClassCastException handling... and auto
    // conversion? hmmm...
    Object object = get(name);
    if (object == null)
      return null;
    if (object instanceof java.lang.String) {
      return (String) object;
    } else {
      return object.toString();
    }
  }

  public java.sql.Timestamp getTimestamp(String name) {
    return (java.sql.Timestamp) get(name);
  }

  public java.sql.Time getTime(String name) {
    return (java.sql.Time) get(name);
  }

  public java.sql.Date getDate(String name) {
    return (java.sql.Date) get(name);
  }

  public Integer getInteger(String name) {
    return (Integer) get(name);
  }

  public Long getLong(String name) {
    return (Long) get(name);
  }

  public Float getFloat(String name) {
    return (Float) get(name);
  }

  public Double getDouble(String name) {
    // this "hack" is needed for now until the Double/BigDecimal issues are all
    // resolved
    Object value = get(name);
    if (value instanceof BigDecimal) {
      return Double.valueOf(((BigDecimal) value).doubleValue());
    } else {
      return (Double) value;
    }
  }

  public BigDecimal getBigDecimal(String name) {
    // this "hack" is needed for now until the Double/BigDecimal issues are all
    // resolved
    // NOTE: for things to generally work properly BigDecimal should really be
    // used as the java-type in the field type def XML files
    Object value = get(name);
    if (value instanceof Double) {
      return BigDecimal.valueOf(((Double) value).doubleValue());
    } else {
      return (BigDecimal) value;
    }
  }

  @SuppressWarnings("deprecation")
  public byte[] getBytes(String name) {
    Object value = get(name);
    if (value == null) {
      return null;
    }
    if (value instanceof Blob) {
      try {
        Blob valueBlob = (Blob) value;
        return valueBlob.getBytes(1, (int) valueBlob.length());
      } catch (SQLException e) {
        String errMsg = "Error getting byte[] from Blob: " + e.toString();
        logger.error(errMsg, e);
        return null;
      }
    }
    if (value instanceof byte[]) {
      return (byte[]) value;
    }
    if (value instanceof org.ofbiz.entity.util.ByteWrapper) {
      // NOTE DEJ20071022: the use of ByteWrapper is not recommended and is
      // deprecated, only old data should be stored that way
      logger.warn("Found a ByteWrapper object in the database for field ["
          + this.getEntityName()
          + "."
          + name
          + "]; converting to byte[] and returning, but note that you need to update your database to unwrap these objects for future compatibility");
      org.ofbiz.entity.util.ByteWrapper wrapper = (org.ofbiz.entity.util.ByteWrapper) value;
      return wrapper.getBytes();
    }
    // uh-oh, this shouldn't happen...
    throw new IllegalArgumentException("In call to getBytes the value is not a supported type, should be byte[] or ByteWrapper, is: "
        + value.getClass().getName());
  }

  /**
   * Checks a resource bundle for a value for this field using the entity name,
   * the field name and a composite of the Primary Key field values as a key. If
   * no value is found in the resource then the field value is returned. Uses
   * the default-resource-name from the entity definition as the resource name.
   * To specify a resource name manually, use the other getResource method. So,
   * the key in the resource bundle (properties file) should be as follows:
   * <entity
   * -name>.<field-name>.<pk-field-value-1>.<pk-field-value-2>...<pk-field
   * -value-n> For example: ProductType.description.FINISHED_GOOD
   * 
   * @param name The name of the field on the entity
   * @param locale The locale to use when finding the ResourceBundle, if null
   *          uses the default locale for the current instance of Java
   * @return If the corresponding resource is found and contains a key as
   *         described above, then that property value is returned; otherwise
   *         returns the field value
   */
  public Object get(String name, Locale locale) {
    return get(name, null, locale);
  }

  /**
   * Same as the getResource method that does not take resource name, but
   * instead allows manually specifying the resource name. In general you should
   * use the other method for more consistent naming and use of the
   * corresponding properties files.
   * 
   * @param name The name of the field on the entity
   * @param resource The name of the resource to get the value from; if null
   *          defaults to the default-resource-name on the entity definition, if
   *          specified there
   * @param locale The locale to use when finding the ResourceBundle, if null
   *          uses the default locale for the current instance of Java
   * @return If the specified resource is found and contains a key as described
   *         above, then that property value is returned; otherwise returns the
   *         field value
   */
  public Object get(String name, String resource, Locale locale) {
    Object fieldValue = null;
    try {
      fieldValue = get(name);
    } catch (IllegalArgumentException e) {
      logger.warn("The field name (or key) ["
          + name
          + "] is not valid for entity ["
          + this.getEntityName()
          + "], printing IllegalArgumentException instead of throwing it because Map interface specification does not allow throwing that exception.");
      fieldValue = null;

      // In case of view entity first try to retrieve with View field names
      Entity modelEntityToUse = this.getModelEntity();
      Object resourceValue = get(this.getModelEntity(), modelEntityToUse, name, resource, locale);
      if (resourceValue == null) {
        if (modelEntityToUse instanceof ViewEntity) {
          // now try to retrieve with the field heading from the real entity
          // linked to the view
          ViewEntity modelViewEntity = (ViewEntity) modelEntityToUse;
          Iterator<ModelAlias> it = modelViewEntity.getAliasesIterator();
          while (it.hasNext()) {
            ModelAlias modelAlias = it.next();
            if (modelAlias.getName().equalsIgnoreCase(name)) {
              modelEntityToUse = modelViewEntity.getMemberModelEntity(modelAlias.getEntityAlias());
              name = modelAlias.getField();
              break;
            }
          }
          resourceValue = get(this.getModelEntity(), modelEntityToUse, name, resource, locale);
          if (resourceValue == null) {
            return fieldValue;
          } else {
            return resourceValue;
          }
        } else {
          return fieldValue;
        }
      } else {
        return resourceValue;
      }
    }
  }

  /**
   * call by the previous method to be able to read with View entityName and
   * entity Field and after for real entity
   * 
   * @param modelEntity the modelEntity, for a view it's the ViewEntity
   * @param modelEntityToUse, same as before except if it's a second call for a
   *          view, and so it's the real modelEntity
   * @return null or resourceValue
   */
  private Object get(Entity modelEntity,
                     Entity modelEntityToUse,
                     String name,
                     String resource,
                     Locale locale) {
    if (UtilValidate.isEmpty(resource)) {
      resource = modelEntityToUse.getDefaultResourceName();
      // still empty? return null
      if (UtilValidate.isEmpty(resource)) {
        // Debug.logWarning("Tried to getResource value for field named " + name
        // +
        // " but no resource name was passed to the method or specified in the default-resource-name attribute of the entity definition",
        // module);
        return null;
      }
    }
    if (UtilProperties.isPropertiesResourceNotFound(resource, locale, false)) {
      // Properties do not exist for this resource+locale combination
      return null;
    }
    ResourceBundle bundle = null;
    try {
      bundle = UtilProperties.getResourceBundle(resource, locale);
    } catch (IllegalArgumentException e) {
      bundle = null;
    }
    if (bundle == null) {
      // Debug.logWarning("Tried to getResource value for field named " + name +
      // " but no resource was found with the name " + resource +
      // " in the locale " + locale, module);
      return null;
    }

    StringBuilder keyBuffer = new StringBuilder();
    // start with the Entity Name
    keyBuffer.append(modelEntityToUse.getEntityName());
    // next add the Field Name
    keyBuffer.append('.');
    keyBuffer.append(name);
    // finish off by adding the values of all PK fields
    Iterator<Field> iter = modelEntity.getPksIterator();
    while (iter != null && iter.hasNext()) {
      Field curField = iter.next();
      keyBuffer.append('.');
      keyBuffer.append(this.get(curField.getName()));
    }

    String bundleKey = keyBuffer.toString();

    Object resourceValue = null;
    try {
      resourceValue = bundle.getObject(bundleKey);
    } catch (MissingResourceException e) {
      return null;
    }
    return resourceValue;
  }

  public GenericPK getPrimaryKey() {
    Collection<String> pkNames = FastList.newInstance();
    Iterator<Field> iter = this.getModelEntity().getPksIterator();
    while (iter != null && iter.hasNext()) {
      Field curField = iter.next();
      pkNames.add(curField.getName());
    }
    return GenericPK.create(this.getDelegator(), getModelEntity(), this.getFields(pkNames));
  }

  /**
   * go through the pks and for each one see if there is an entry in fields to
   * set
   */
  public void setPKFields(Map<? extends Object, ? extends Object> fields) {
    setAllFields(fields, true, null, Boolean.TRUE);
  }

  /**
   * go through the pks and for each one see if there is an entry in fields to
   * set
   */
  public void setPKFields(Map<? extends Object, ? extends Object> fields, boolean setIfEmpty) {
    setAllFields(fields, setIfEmpty, null, Boolean.TRUE);
  }

  /**
   * go through the non-pks and for each one see if there is an entry in fields
   * to set
   */
  public void setNonPKFields(Map<? extends Object, ? extends Object> fields) {
    setAllFields(fields, true, null, Boolean.FALSE);
  }

  /**
   * go through the non-pks and for each one see if there is an entry in fields
   * to set
   */
  public void setNonPKFields(Map<? extends Object, ? extends Object> fields, boolean setIfEmpty) {
    setAllFields(fields, setIfEmpty, null, Boolean.FALSE);
  }

  /**
   * Intelligently sets fields on this entity from the Map of fields passed in
   * 
   * @param fields The fields Map to get the values from
   * @param setIfEmpty Used to specify whether empty/null values in the field
   *          Map should over-write non-empty values in this entity
   * @param namePrefix If not null or empty will be pre-pended to each field
   *          name (upper-casing the first letter of the field name first), and
   *          that will be used as the fields Map lookup name instead of the
   *          field-name
   * @param pks If null, get all values, if TRUE just get PKs, if FALSE just get
   *          non-PKs
   */
  public void setAllFields(Map<? extends Object, ? extends Object> fields,
                           boolean setIfEmpty,
                           String namePrefix,
                           Boolean pks) {
    if (fields == null) {
      return;
    }
    Iterator<Field> iter = null;
    if (pks != null) {
      if (pks.booleanValue()) {
        iter = this.getModelEntity().getPksIterator();
      } else {
        iter = this.getModelEntity().getNopksIterator();
      }
    } else {
      iter = this.getModelEntity().getFieldsIterator();
    }

    while (iter != null && iter.hasNext()) {
      Field curField = iter.next();
      String fieldName = curField.getName();
      String sourceFieldName = null;
      if (UtilValidate.isNotEmpty(namePrefix)) {
        sourceFieldName = namePrefix + Character.toUpperCase(fieldName.charAt(0))
            + fieldName.substring(1);
      } else {
        sourceFieldName = curField.getName();
      }

      if (fields.containsKey(sourceFieldName)) {
        Object field = fields.get(sourceFieldName);

        if (setIfEmpty) {
          // if empty string, set to null
          if (field != null && field instanceof String && ((String) field).length() == 0) {
            this.set(curField.getName(), null);
          } else {
            this.set(curField.getName(), field);
          }
        } else {
          // okay, only set if not empty...
          if (field != null) {
            // if it's a String then we need to check length, otherwise set it
            // because it's not null
            if (field instanceof String) {
              String fieldStr = (String) field;

              if (fieldStr.length() > 0) {
                this.set(curField.getName(), fieldStr);
              }
            } else {
              this.set(curField.getName(), field);
            }
          }
        }
      }
    }
  }

  /**
   * Returns keys of entity fields
   * 
   * @return java.util.Collection
   */
  public Collection<String> getAllKeys() {
    return fields.keySet();
  }

  /**
   * Returns key/value pairs of entity fields
   * 
   * @return java.util.Map
   */
  public Map<String, Object> getAllFields() {
    Map<String, Object> newMap = FastMap.newInstance();
    newMap.putAll(this.fields);
    return newMap;
  }

  /**
   * Used by clients to specify exactly the fields they are interested in
   * 
   * @param keysofFields the name of the fields the client is interested in
   * @return java.util.Map
   */
  public Map<String, Object> getFields(Collection<String> keysofFields) {
    if (keysofFields == null)
      return null;
    Map<String, Object> aMap = FastMap.newInstance();

    for (String aKey : keysofFields) {
      aMap.put(aKey, this.fields.get(aKey));
    }
    return aMap;
  }

  /**
   * Determines the equality of two GenericEntity objects, overrides the default
   * equals
   * 
   * @param obj The object (GenericEntity) to compare this two
   * @return boolean stating if the two objects are equal
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof GenericEntity))
      return false;

    // from here, use the compareTo method since it is more efficient:
    try {
      return this.compareTo((GenericEntity) obj) == 0;
    } catch (ClassCastException e) {
      return false;
    }
  }

  public void setFields(Map<? extends String, ? extends Object> keyValuePairs) {
    if (keyValuePairs == null)
      return;
    // this could be implement with Map.putAll, but we 'll leave it like this
    // for the extra feature it has.
    for (Map.Entry<? extends String, ? extends Object> anEntry : keyValuePairs.entrySet()) {
      this.set(anEntry.getKey(), anEntry.getValue(), true);
    }
  }

  public boolean matchesFields(Map<String, ? extends Object> keyValuePairs) {
    if (fields == null)
      return true;
    if (UtilValidate.isEmpty(keyValuePairs))
      return true;
    for (Map.Entry<String, ? extends Object> anEntry : keyValuePairs.entrySet()) {
      if (!UtilValidate.areEqual(anEntry.getValue(), this.fields.get(anEntry.getKey()))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Used to indicate if locking is enabled for this entity
   * 
   * @return True if locking is enabled
   */
  public boolean lockEnabled() {
    return getModelEntity().lock();
  }

  /**
   * Creates a hashCode for the entity, using the default String hashCode and
   * Map hashCode, overrides the default hashCode
   * 
   * @return Hashcode corresponding to this entity
   */
  @Override
  public int hashCode() {
    // divide both by two (shift to right one bit) to maintain scale and add
    // together
    if (generateHashCode) {
      cachedHashCode = 0;
      if (getEntityName() != null) {
        cachedHashCode += getEntityName().hashCode() >> 1;
      }
      cachedHashCode += fields.hashCode() >> 1;
      generateHashCode = false;
    }
    return cachedHashCode;
  }

  protected int compareToFields(GenericEntity that, String name) {
    Comparable<Object> thisVal = UtilGenerics.cast(this.fields.get(name));
    Object thatVal = that.fields.get(name);

    if (thisVal == null) {
      if (thatVal == null)
        return 0;
      // if thisVal is null, but thatVal is not, return 1 to put this earlier in
      // the list
      else
        return 1;
    } else {
      // if thatVal is null, put the other earlier in the list
      if (thatVal == null)
        return -1;
      else
        return thisVal.compareTo(thatVal);
    }
  }

  /**
   * Compares this GenericEntity to the passed object
   * 
   * @param that Object to compare this to
   * @return int representing the result of the comparison (-1,0, or 1)
   */
  public int compareTo(GenericEntity that) {
    // if null, it will push to the beginning
    if (that == null)
      return -1;

    int tempResult = this.entityName.compareTo(that.entityName);

    // if they did not match, we know the order, otherwise compare the primary
    // keys
    if (tempResult != 0)
      return tempResult;

    // both have same entityName, should be the same so let's compare PKs
    Iterator<Field> pkIter = getModelEntity().getPksIterator();
    while (pkIter.hasNext()) {
      Field curField = pkIter.next();
      tempResult = compareToFields(that, curField.getName());
      if (tempResult != 0)
        return tempResult;
    }

    // okay, if we got here it means the primaryKeys are exactly the SAME, so
    // compare the rest of the fields
    Iterator<Field> nopkIter = getModelEntity().getNopksIterator();
    while (nopkIter.hasNext()) {
      Field curField = nopkIter.next();
      if (!curField.getIsAutoCreatedInternal()) {
        tempResult = compareToFields(that, curField.getName());
        if (tempResult != 0)
          return tempResult;
      }
    }

    // if we got here it means the two are exactly the same, so return
    // tempResult, which should be 0
    return tempResult;
  }

  /**
   * Clones this GenericEntity, this is a shallow clone & uses the default
   * shallow HashMap clone
   * 
   * @return Object that is a clone of this GenericEntity
   */
  @Override
  public Object clone() {
    GenericEntity newEntity = new GenericEntity();
    newEntity.init(this);

    newEntity.setDelegator(internalDelegator);
    return newEntity;
  }

  // ---- Methods added to implement the Map interface: ----

  public Object remove(Object key) {
    return this.fields.remove(key);
  }

  public boolean containsKey(Object key) {
    return this.fields.containsKey(key);
  }

  public java.util.Set<Map.Entry<String, Object>> entrySet() {
    return Collections.unmodifiableMap(this.fields).entrySet();
  }

  public Object put(String key, Object value) {
    return this.set(key, value, true);
  }

  public void putAll(java.util.Map<? extends String, ? extends Object> map) {
    this.setFields(map);
  }

  public void clear() {
    this.fields.clear();
  }

  public Object get(Object key) {
    return this.get((String) key);
  }

  public java.util.Set<String> keySet() {
    return Collections.unmodifiableSet(this.fields.keySet());
  }

  public boolean isEmpty() {
    return this.fields.isEmpty();
  }

  public java.util.Collection<Object> values() {
    return Collections.unmodifiableMap(this.fields).values();
  }

  public boolean containsValue(Object value) {
    return this.fields.containsValue(value);
  }

  public int size() {
    return this.fields.size();
  }

  public boolean matches(EntityCondition condition) {
    return condition.entityMatches(this);
  }

  public static interface NULL {
  }

  public static class NullGenericEntity extends GenericEntity implements NULL {
    protected NullGenericEntity() {
    }

    @Override
    public String getEntityName() {
      return "[null-entity]";
    }

    @Override
    public String toString() {
      return "[null-entity]";
    }
  }

  public static class NullField implements NULL, Comparable<NullField> {
    protected NullField() {
    }

    @Override
    public String toString() {
      return "[null-field]";
    }

    public int compareTo(NullField other) {
      return this != other ? -1 : 0;
    }
  }

}
