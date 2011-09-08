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
package org.etk.entity.engine.plugins.datasource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.etk.entity.engine.core.GenericEntityException;
import org.etk.entity.engine.core.GenericPK;
import org.etk.entity.engine.core.GenericValue;
import org.etk.entity.engine.plugins.condition.EntityCondition;
import org.etk.entity.engine.plugins.model.xml.Entity;
import org.etk.entity.engine.plugins.model.xml.Relation;
import org.etk.entity.engine.plugins.util.EntityFindOptions;
import org.etk.entity.engine.plugins.util.EntityListIterator;

/**
 * Generic Entity Helper Class
 *
 */
public interface GenericHelper {

    /** Gets the name of the server configuration that corresponds to this helper
     *@return server configuration name
     */
    public String getHelperName();

    public <T> Future<T> submitWork(Callable<T> callable) throws GenericEntityException;

    /** Creates a Entity in the form of a GenericValue and write it to the database
     *@return GenericValue instance containing the new instance
     */
    public GenericValue create(GenericValue value) throws GenericEntityException;

    /** Find a Generic Entity by its Primary Key
     *@param primaryKey The primary key to find by.
     *@return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKey(GenericPK primaryKey) throws GenericEntityException;

    /** Find a Generic Entity by its Primary Key and only returns the values requested by the passed keys (names)
     *@param primaryKey The primary key to find by.
     *@param keys The keys, or names, of the values to retrieve; only these values will be retrieved
     *@return The GenericValue corresponding to the primaryKey
     */
    public GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set<String> keys) throws GenericEntityException;

    /** Find a number of Generic Value objects by their Primary Keys, all at once
     *@param primaryKeys A List of primary keys to find by.
     *@return List of GenericValue objects corresponding to the passed primaryKey objects
     */
    public List<GenericValue> findAllByPrimaryKeys(List<GenericPK> primaryKeys) throws GenericEntityException;

    /** Remove a Generic Entity corresponding to the primaryKey
     *@param  primaryKey  The primary key of the entity to remove.
     *@return int representing number of rows effected by this operation
     */
    public int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException;

    public List<GenericValue> findByMultiRelation(GenericValue value, Relation modelRelationOne, Entity modelEntityOne,
        Relation modelRelationTwo, Entity modelEntityTwo, List<String> orderBy) throws GenericEntityException;

    /** Finds GenericValues by the conditions specified in the EntityCondition object, the the EntityCondition javadoc for more details.
     *@param modelEntity The ModelEntity of the Entity as defined in the entity XML file
     *@param whereEntityCondition The EntityCondition object that specifies how to constrain this query before any groupings are done (if this is a view entity with group-by aliases)
     *@param havingEntityCondition The EntityCondition object that specifies how to constrain this query after any groupings are done (if this is a view entity with group-by aliases)
     *@param fieldsToSelect The fields of the named entity to get from the database; if empty or null all fields will be retreived
     *@param orderBy The fields of the named entity to order the query by; optionally add a " ASC" for ascending or " DESC" for descending
     *@param findOptions An instance of EntityFindOptions that specifies advanced query options. See the EntityFindOptions JavaDoc for more details.
     *@return EntityListIterator representing the result of the query: NOTE THAT THIS MUST BE CLOSED WHEN YOU ARE
     *      DONE WITH IT, AND DON'T LEAVE IT OPEN TOO LONG BEACUSE IT WILL MAINTAIN A DATABASE CONNECTION.
     */
    public EntityListIterator findListIteratorByCondition(Entity modelEntity, EntityCondition whereEntityCondition,
        EntityCondition havingEntityCondition, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions)
        throws GenericEntityException;

    public long findCountByCondition(Entity modelEntity, EntityCondition whereEntityCondition,
            EntityCondition havingEntityCondition, EntityFindOptions findOptions) throws GenericEntityException;

    /** Removes/deletes Generic Entity records found by all the specified condition
     *@param modelEntity The ModelEntity of the Entity as defined in the entity XML file
     *@param condition The condition that restricts the list of removed values
     *@return int representing number of rows effected by this operation
     */
    public int removeByCondition(Entity modelEntity, EntityCondition condition) throws GenericEntityException;

    /** Stores a group of values in a single query
     *@param modelEntity The ModelEntity of the Entity as defined in the entity XML file
     *@param fieldsToSet The fields of the named entity to set in the database
     *@param condition The condition that restricts the list of updated values
     *@return int representing number of rows effected by this operation
     *@throws GenericEntityException
     */
    public int storeByCondition(Entity modelEntity, Map<String, ? extends Object> fieldsToSet, EntityCondition condition) throws GenericEntityException;

    /** Store the Entity from the GenericValue to the persistent store
     *@param value GenericValue instance containing the entity
     *@return int representing number of rows effected by this operation
     */
    public int store(GenericValue value) throws GenericEntityException;

    /** Check the datasource to make sure the entity definitions are correct, optionally adding missing entities or fields on the server
     *@param modelEntities Map of entityName names and ModelEntity values
     *@param messages List to put any result messages in
     *@param addMissing Flag indicating whether or not to add missing entities and fields on the server
     */
    public void checkDataSource(Map<String, Entity> modelEntities, List<String> messages, boolean addMissing) throws GenericEntityException;
}