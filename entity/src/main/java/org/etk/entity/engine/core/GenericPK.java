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

import java.util.Map;

import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.plugins.model.xml.Entity;

import javolution.context.ObjectFactory;

/**
 * Generic Entity Primary Key Object
 *
 */
@SuppressWarnings("serial")
public class GenericPK extends GenericEntity {

    protected static final ObjectFactory<GenericPK> genericPKFactory = new ObjectFactory<GenericPK>() {
        @Override
        protected GenericPK create() {
            return new GenericPK();
        }
    };

    protected GenericPK() { }

    /** Creates new GenericPK */
    public static GenericPK create(Entity modelEntity) {
        GenericPK newPK = genericPKFactory.object();
        newPK.init(modelEntity);
        return newPK;
    }

    /** Creates new GenericPK from existing Map */
    public static GenericPK create(Delegator delegator, Entity modelEntity, Map<String, ? extends Object> fields) {
        GenericPK newPK = genericPKFactory.object();
        newPK.init(delegator, modelEntity, fields);
        return newPK;
    }

    /** Creates new GenericPK from existing Map */
    public static GenericPK create(Delegator delegator, Entity modelEntity, Object singlePkValue) {
        GenericPK newPK = genericPKFactory.object();
        newPK.init(delegator, modelEntity, singlePkValue);
        return newPK;
    }

    /** Creates new GenericPK from existing GenericPK */
    public static GenericPK create(GenericPK value) {
        GenericPK newPK = genericPKFactory.object();
        newPK.init(value);
        return newPK;
    }

    /** Clones this GenericPK, this is a shallow clone & uses the default shallow HashMap clone
     *@return Object that is a clone of this GenericPK
     */
    @Override
    public Object clone() {
        return GenericPK.create(this);
    }
}
