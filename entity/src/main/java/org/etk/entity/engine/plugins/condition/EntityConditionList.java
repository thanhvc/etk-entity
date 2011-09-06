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
package org.etk.entity.engine.plugins.condition;

import java.util.Iterator;

import javolution.context.ObjectFactory;

/**
 * Encapsulates a list of EntityConditions to be used as a single EntityCondition combined as specified
 *
 */
@SuppressWarnings("serial")
public class EntityConditionList<T extends EntityCondition> extends EntityConditionListBase<T> {
    public static final String module = EntityConditionList.class.getName();

    protected static final ObjectFactory<EntityConditionList<EntityCondition>> entityConditionListFactory = new ObjectFactory<EntityConditionList<EntityCondition>>() {
        @Override
        protected EntityConditionList<EntityCondition> create() {
            return new EntityConditionList<EntityCondition>();
        }
    };

    protected EntityConditionList() {
        super();
    }

    @Override
    public int getConditionListSize() {
        return super.getConditionListSize();
    }

    @Override
    public Iterator<T> getConditionIterator() {
        return super.getConditionIterator();
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityConditionList(this);
    }
}