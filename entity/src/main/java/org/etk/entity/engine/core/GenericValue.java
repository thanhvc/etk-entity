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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.etk.common.logging.Logger;
import org.etk.entity.base.utils.UtilMisc;
import org.etk.entity.base.utils.UtilValidate;
import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.plugins.condition.EntityCondition;
import org.etk.entity.engine.plugins.model.xml.Entity;
import org.etk.entity.engine.plugins.model.xml.Relation;
import org.etk.entity.engine.plugins.util.EntityUtil;

import javolution.context.ObjectFactory;
import javolution.lang.Reusable;
import javolution.util.FastMap;

/**
 * Generic Entity Value Object - Handles persistence for any defined entity.
 *
 */
@SuppressWarnings("serial")
public class GenericValue extends GenericEntity implements Reusable {

  private Logger logger = Logger.getLogger(GenericValue.class);
    public static final GenericValue NULL_VALUE = new NullGenericValue();

    protected static final ObjectFactory<GenericValue> genericValueFactory = new ObjectFactory<GenericValue>() {
        @Override
        protected GenericValue create() {
            return new GenericValue();
        }
    };

    /** Map to cache various related entity collections */
    public transient Map<String, List<GenericValue>> relatedCache = null;

    /** Map to cache various related cardinality one entity collections */
    public transient Map<String, GenericValue> relatedOneCache = null;

    /** This Map will contain the original field values from the database iff
     * this GenericValue came from the database. If it was made manually it will
     * no have this Map, ie it will be null to not take up memory.
     */
    protected Map<String, Object> originalDbValues = null;

    protected GenericValue() { }

    /** Creates new GenericValue */
    public static GenericValue create(Entity modelEntity) {
        GenericValue newValue = genericValueFactory.object();
        newValue.init(modelEntity);
        return newValue;
    }

    /** Creates new GenericValue from existing Map */
    public static GenericValue create(Delegator delegator, Entity modelEntity, Map<String, ? extends Object> fields) {
        GenericValue newValue = genericValueFactory.object();
        newValue.init(delegator, modelEntity, fields);
        return newValue;
    }

    /** Creates new GenericValue from existing Map */
    public static GenericValue create(Delegator delegator, Entity modelEntity, Object singlePkValue) {
        GenericValue newValue = genericValueFactory.object();
        newValue.init(delegator, modelEntity, singlePkValue);
        return newValue;
    }

    /** Creates new GenericValue from existing GenericValue */
    public static GenericValue create(GenericValue value) {
        GenericValue newValue = genericValueFactory.object();
        newValue.init(value);
        return newValue;
    }

    /** Creates new GenericValue from existing GenericValue */
    public static GenericValue create(GenericPK primaryKey) {
        GenericValue newValue = genericValueFactory.object();
        newValue.init(primaryKey);
        return newValue;
    }

    @Override
    public void reset() {
        // from GenericEntity
        super.reset();

        // from GenericValue
        this.relatedCache = null;
        this.relatedOneCache = null;
        this.originalDbValues = null;
    }

    @Override
    public void synchronizedWithDatasource() {
        super.synchronizedWithDatasource();
        this.copyOriginalDbValues();
    }

    public GenericValue create() throws GenericEntityException {
        return this.getDelegator().create(this);
    }

    public void store() throws GenericEntityException {
        this.getDelegator().store(this);
    }

    public void remove() throws GenericEntityException {
        this.getDelegator().removeValue(this);
    }

    public void refresh() throws GenericEntityException {
        this.getDelegator().refresh(this);
    }

    public void refreshFromCache() throws GenericEntityException {
        this.getDelegator().refreshFromCache(this);
    }

    public boolean originalDbValuesAvailable() {
        return this.originalDbValues != null ? true : false;
    }

    public Object getOriginalDbValue(String name) {
        if (getModelEntity().getField(name) == null) {
            throw new IllegalArgumentException("[GenericEntity.get] \"" + name + "\" is not a field of " + entityName);
        }
        if (originalDbValues == null) return null;
        return originalDbValues.get(name);
    }

    /** This should only be called by the Entity Engine once a GenericValue has
     * been read from the database so that we have a copy of the original field
     * values from the Db.
     */
    public void copyOriginalDbValues() {
        this.originalDbValues = FastMap.newInstance();
        this.originalDbValues.putAll(this.fields);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelated(String relationName) throws GenericEntityException {
        return this.getDelegator().getRelated(relationName, null, null, this);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelated(String relationName, List<String> orderBy) throws GenericEntityException {
        return this.getDelegator().getRelated(relationName, FastMap.<String, Object>newInstance(), orderBy, this);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     * @param byAndFields the fields that must equal in order to keep; may be null
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelated(String relationName, Map<String, ? extends Object> byAndFields, List<String> orderBy) throws GenericEntityException {
        return this.getDelegator().getRelated(relationName, byAndFields, orderBy, this);
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store, looking first in the global generic cache (for the moment this isn't true, is same as EmbeddedCache variant)
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedCache(String relationName) throws GenericEntityException {
        return this.getDelegator().getRelatedCache(relationName, this);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent store across another Relation.
     * Helps to get related Values in a multi-to-multi relationship.
     * @param relationNameOne String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file, for first relation
     * @param relationNameTwo String containing the relation name for second relation
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedMulti(String relationNameOne, String relationNameTwo, List<String> orderBy) throws GenericEntityException {
        return this.getDelegator().getMultiRelation(this, relationNameOne, relationNameTwo, orderBy);
    }

    /**
     * Get the named Related Entity for the GenericValue from the persistent store across another Relation.
     * Helps to get related Values in a multi-to-multi relationship.
     * @param relationNameOne String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file, for first relation
     * @param relationNameTwo String containing the relation name for second relation
     * @return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedMulti(String relationNameOne, String relationNameTwo) throws GenericEntityException {
        return this.getDelegator().getMultiRelation(this, relationNameOne, relationNameTwo, null);
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store, looking first in the global generic cache (for the moment this isn't true, is same as EmbeddedCache variant)
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     * @param byAndFields the fields that must equal in order to keep; may be null
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedCache(String relationName, Map<String, ? extends Object> byAndFields, List<String> orderBy) throws GenericEntityException {
        List<GenericValue> col = getRelatedCache(relationName);

        if (byAndFields != null) col = EntityUtil.filterByAnd(col, byAndFields);
        if (UtilValidate.isNotEmpty(orderBy)) col = EntityUtil.orderBy(col, orderBy);
        return col;
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store, looking first in the global generic cache (for the moment this isn't true, is same as EmbeddedCache variant)
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedCache(String relationName, List<String> orderBy) throws GenericEntityException {
        return this.getRelatedCache(relationName, null, orderBy);
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store, looking first in a cache associated with this entity which is
     *  destroyed with this ValueObject when no longer used.
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedEmbeddedCache(String relationName) throws GenericEntityException {
        if (relatedCache == null) relatedCache = FastMap.newInstance();
        List<GenericValue> col = relatedCache.get(relationName);

        if (col == null) {
            col = getRelated(relationName);
            relatedCache.put(relationName, col);
        }
        return col;
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store, looking first in a cache associated with this entity which is
     *  destroyed with this ValueObject when no longer used.
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     * @param byAndFields the fields that must equal in order to keep; may be null
     * @param orderBy The fields of the named entity to order the query by; may be null;
     *      optionally add a " ASC" for ascending or " DESC" for descending
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedEmbeddedCache(String relationName, Map<String, ? extends Object> byAndFields, List<String> orderBy) throws GenericEntityException {
        List<GenericValue> col = getRelatedEmbeddedCache(relationName);

        if (byAndFields != null) col = EntityUtil.filterByAnd(col, byAndFields);
        if (UtilValidate.isNotEmpty(orderBy)) col = EntityUtil.orderBy(col, orderBy);
        return col;
    }

    public void removeRelatedEmbeddedCache(String relationName) {
        if (relatedCache == null) return;
        relatedCache.remove(relationName);
    }

    public void storeRelatedEmbeddedCache(String relationName, List<GenericValue> col) {
        if (relatedCache == null) relatedCache = FastMap.newInstance();
        relatedCache.put(relationName, col);
    }

    public void storeRelatedEmbeddedCache(String relationName, GenericValue value) {
        if (relatedCache == null) relatedCache = FastMap.newInstance();
        relatedCache.put(relationName, UtilMisc.toList(value));
    }

    public void clearEmbeddedCache() {
        relatedCache.clear();
    }

    /** Get the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@return List of GenericValue instances as specified in the relation definition
     */
    public GenericValue getRelatedOne(String relationName) throws GenericEntityException {
        return this.getDelegator().getRelatedOne(relationName, this);
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store, looking first in the global generic cache (for the moment this isn't true, is same as EmbeddedCache variant)
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@return List of GenericValue instances as specified in the relation definition
     */
    public GenericValue getRelatedOneCache(String relationName) throws GenericEntityException {
        return this.getDelegator().getRelatedOneCache(relationName, this);
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store, looking first in a cache associated with this entity which is
     *  destroyed with this ValueObject when no longer used.
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@return List of GenericValue instances as specified in the relation definition
     */
    public GenericValue getRelatedOneEmbeddedCache(String relationName) throws GenericEntityException {
        if (relatedOneCache == null) relatedOneCache = FastMap.newInstance();
        GenericValue value = relatedOneCache.get(relationName);

        if (value == null) {
            value = getRelatedOne(relationName);
            if (value != null) relatedOneCache.put(relationName, value);
        }
        return value;
    }

    /** Get the named Related Entity for the GenericValue from the persistent store and filter it
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@param fields the fields that must equal in order to keep
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedByAnd(String relationName, Map<String, ? extends Object> fields) throws GenericEntityException {
        return this.getDelegator().getRelated(relationName, fields, null, this);
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store and filter it, looking first in the global generic cache (for the moment this isn't true, is same as EmbeddedCache variant)
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@param fields the fields that must equal in order to keep
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedByAndCache(String relationName, Map<String, ? extends Object> fields) throws GenericEntityException {
        return EntityUtil.filterByAnd(this.getDelegator().getRelatedCache(relationName, this), fields);
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store and filter it, looking first in a cache associated with this entity which is
     *  destroyed with this ValueObject when no longer used.
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@param fields the fields that must equal in order to keep
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedByAndEmbeddedCache(String relationName, Map<String, ? extends Object> fields) throws GenericEntityException {
        return EntityUtil.filterByAnd(getRelatedEmbeddedCache(relationName), fields);
    }

    /** Get the named Related Entity for the GenericValue from the persistent store and order it
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@param orderBy the order that they should be returned
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedOrderBy(String relationName, List<String> orderBy) throws GenericEntityException {
        return this.getDelegator().getRelated(relationName, null, orderBy, this);
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store and order it, looking first in the global generic cache (for the moment this isn't true, is same as EmbeddedCache variant)
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@param orderBy the order that they should be returned
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedOrderByCache(String relationName, List<String> orderBy) throws GenericEntityException {
        return EntityUtil.orderBy(this.getDelegator().getRelatedCache(relationName, this), orderBy);
    }

    /** Get the named Related Entity for the GenericValue from the persistent
     *  store and order it, looking first in a cache associated with this entity which is
     *  destroyed with this ValueObject when no longer used.
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     *@param orderBy the order that they should be returned
     *@return List of GenericValue instances as specified in the relation definition
     */
    public List<GenericValue> getRelatedOrderByEmbeddedCache(String relationName, List<String> orderBy) throws GenericEntityException {
        return EntityUtil.orderBy(getRelatedEmbeddedCache(relationName), orderBy);
    }

    /** Remove the named Related Entity for the GenericValue from the persistent store
     *@param relationName String containing the relation name which is the combination of relation.title and relation.rel-entity-name as specified in the entity XML definition file
     */
    public void removeRelated(String relationName) throws GenericEntityException {
        this.getDelegator().removeRelated(relationName, this);
    }

    /** Get a dummy primary key for the named Related Entity for the GenericValue
     * @param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     * @return GenericPK containing a possibly incomplete PrimaryKey object representing the related entity or entities
     */
    public GenericPK getRelatedDummyPK(String relationName) throws GenericEntityException {
        return this.getDelegator().getRelatedDummyPK(relationName, null, this);
    }

    /** Get a dummy primary key for the named Related Entity for the GenericValue
     * @param relationName String containing the relation name which is the
     *      combination of relation.title and relation.rel-entity-name as
     *      specified in the entity XML definition file
     * @param byAndFields the fields that must equal in order to keep; may be null
     * @return GenericPK containing a possibly incomplete PrimaryKey object representing the related entity or entities
     */
    public GenericPK getRelatedDummyPK(String relationName, Map<String, ? extends Object> byAndFields) throws GenericEntityException {
        return this.getDelegator().getRelatedDummyPK(relationName, byAndFields, this);
    }

    /**
     * Checks to see if all foreign key records exist in the database. Will create a dummy value for
     * those missing when specified.
     *
     * @param insertDummy Create a dummy record using the provided fields
     * @return true if all FKs exist (or when all missing are created)
     * @throws GenericEntityException
     */
    public boolean checkFks(boolean insertDummy) throws GenericEntityException {
        Entity model = this.getModelEntity();
        Iterator<Relation> relItr = model.getRelationsIterator();
        while (relItr.hasNext()) {
            Relation relation = relItr.next();
            if ("one".equalsIgnoreCase(relation.getType())) {
                // see if the related value exists
                Map<String, Object> fields = FastMap.newInstance();
                for (int i = 0; i < relation.getKeyMapsSize(); i++) {
                    ModelKeyMap keyMap = relation.getKeyMap(i);
                    fields.put(keyMap.getRelFieldName(), this.get(keyMap.getFieldName()));
                }
                EntityFieldMap ecl = EntityCondition.makeCondition(fields);
                long count = this.getDelegator().findCountByCondition(relation.getRelEntityName(), ecl, null, null);
                if (count == 0) {
                    if (insertDummy) {
                        // create the new related value (dummy)
                        GenericValue newValue = this.getDelegator().makeValue(relation.getRelEntityName());
                        Iterator<ModelKeyMap> keyMapIter = relation.getKeyMapsIterator();
                        boolean allFieldsSet = true;
                        while (keyMapIter.hasNext()) {
                            ModelKeyMap mkm = keyMapIter.next();
                            if (this.get(mkm.getFieldName()) != null) {
                                newValue.set(mkm.getRelFieldName(), this.get(mkm.getFieldName()));
                                if (Debug.infoOn()) Debug.logInfo("Set [" + mkm.getRelFieldName() + "] to - " + this.get(mkm.getFieldName()), module);
                            } else {
                                allFieldsSet = false;
                            }
                        }
                        if (allFieldsSet) {
                            if (logger.isDebugEnabled()) logger.info("Creating place holder value : " + newValue);

                            // inherit create and update times from this value in order to make this not seem like new/fresh data
                            newValue.put(Entity.CREATE_STAMP_FIELD, this.get(Entity.CREATE_STAMP_FIELD));
                            newValue.put(Entity.CREATE_STAMP_TX_FIELD, this.get(Entity.CREATE_STAMP_TX_FIELD));
                            newValue.put(Entity.STAMP_FIELD, this.get(Entity.STAMP_FIELD));
                            newValue.put(Entity.STAMP_TX_FIELD, this.get(Entity.STAMP_TX_FIELD));
                            // set isFromEntitySync so that create/update stamp fields set above will be preserved
                            newValue.setIsFromEntitySync(true);
                            // check the FKs for the newly created entity
                            newValue.checkFks(true);
                            newValue.create();
                        }
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Clones this GenericValue, this is a shallow clone & uses the default shallow HashMap clone
     *@return Object that is a clone of this GenericValue
     */
    @Override
    public Object clone() {
        return GenericValue.create(this);
    }

    protected static class NullGenericValue extends GenericValue implements NULL {
        @Override
        public String getEntityName() {
            return "[null-entity-value]";
        }
        @Override
        public String toString() {
            return "[null-entity-value]";
        }
    }
}