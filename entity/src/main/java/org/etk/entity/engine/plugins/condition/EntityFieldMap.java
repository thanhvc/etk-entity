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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.etk.entity.base.utils.UtilGenerics;
import org.etk.entity.engine.plugins.util.EntityUtil;
import javolution.context.ObjectFactory;
/**
 * Encapsulates simple expressions used for specifying queries
 *
 */
@SuppressWarnings("serial")
public class EntityFieldMap extends EntityConditionListBase<EntityExpr> {

    protected static final ObjectFactory<EntityFieldMap> entityFieldMapFactory = new ObjectFactory<EntityFieldMap>() {
        @Override
        protected EntityFieldMap create() {
            return new EntityFieldMap();
        }
    };

    protected Map<String, ? extends Object> fieldMap = null;

    protected EntityFieldMap() {
        super();
    }

    public static <V> List<EntityExpr> makeConditionList(EntityComparisonOperator<?,V> op, V... keysValues) {
        return makeConditionList(EntityUtil.makeFields(keysValues), op);
    }

    public static <V> List<EntityExpr> makeConditionList(Map<String, V> fieldMap, EntityComparisonOperator<?,V> op) {
        if (fieldMap == null) return new ArrayList<EntityExpr>();
        List<EntityExpr> list = new ArrayList<EntityExpr>(fieldMap.size());
        for (Map.Entry<String, ? extends Object> entry: fieldMap.entrySet()) {
            list.add(EntityCondition.makeCondition(entry.getKey(), op, entry.getValue()));
        }
        return list;
    }

    public <V> void init(EntityComparisonOperator<?,?> compOp, EntityJoinOperator joinOp, V... keysValues) {
        super.init(makeConditionList(EntityUtil.makeFields(keysValues), UtilGenerics.<EntityComparisonOperator<String,V>>cast(compOp)), joinOp);
        this.fieldMap = EntityUtil.makeFields(keysValues);
        if (this.fieldMap == null) this.fieldMap = FastMap.newInstance();
        this.operator = joinOp;
    }

    public <V> void init(Map<String, V> fieldMap, EntityComparisonOperator<?,?> compOp, EntityJoinOperator joinOp) {
        super.init(makeConditionList(fieldMap, UtilGenerics.<EntityComparisonOperator<String,V>>cast(compOp)), joinOp);
        this.fieldMap = fieldMap;
        if (this.fieldMap == null) this.fieldMap = FastMap.newInstance();
        this.operator = joinOp;
    }

    @Override
    public void reset() {
        super.reset();
        this.fieldMap = null;
    }

    public Object getField(String name) {
        return this.fieldMap.get(name);
    }

    public boolean containsField(String name) {
        return this.fieldMap.containsKey(name);
    }

    public Iterator<String> getFieldKeyIterator() {
        return Collections.unmodifiableSet(this.fieldMap.keySet()).iterator();
    }

    public Iterator<Map.Entry<String, Object>> getFieldEntryIterator() {
        return Collections.unmodifiableMap(this.fieldMap).entrySet().iterator();
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityFieldMap(this);
    }
}
