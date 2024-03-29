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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.etk.entity.base.utils.UtilGenerics;
import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.core.GenericEntity;
import org.etk.entity.engine.core.GenericModelException;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.model.xml.Entity;
import org.etk.entity.engine.plugins.model.xml.Field;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 6, 2011  
 */
/**
 * Base class for operators (less than, greater than, equals, etc).
 *
 */
@SuppressWarnings("serial")
public abstract class EntityOperator<L, R, T> extends EntityConditionBase {

    public static final int ID_EQUALS = 1;
    public static final int ID_NOT_EQUAL = 2;
    public static final int ID_LESS_THAN = 3;
    public static final int ID_GREATER_THAN = 4;
    public static final int ID_LESS_THAN_EQUAL_TO = 5;
    public static final int ID_GREATER_THAN_EQUAL_TO = 6;
    public static final int ID_IN = 7;
    public static final int ID_BETWEEN = 8;
    public static final int ID_NOT = 9;
    public static final int ID_AND = 10;
    public static final int ID_OR = 11;
    public static final int ID_LIKE = 12;
    public static final int ID_NOT_IN = 13;
    public static final int ID_NOT_LIKE = 14;

    private static final AtomicInteger dynamicId = new AtomicInteger();
    private static HashMap<String, EntityOperator<?,?,?>> registry = new HashMap<String, EntityOperator<?,?,?>>();

    private static <L,R,T> void registerCase(String name, EntityOperator<L,R,T> operator) {
        registry.put(name.toLowerCase(), operator);
        registry.put(name.toUpperCase(), operator);
    }

    public static <L,R,T> void register(String name, EntityOperator<L,R,T> operator) {
        registerCase(name, operator);
        registerCase(name.replaceAll("-", "_"), operator);
        registerCase(name.replaceAll("_", "-"), operator);
    }

    public static <L,R,T> EntityOperator<L,R,T> lookup(String name) {
        return UtilGenerics.cast(registry.get(name));
    }

    public static <L,R> EntityComparisonOperator<L,R> lookupComparison(String name) {
        EntityOperator<?,?,Boolean> operator = lookup(name);
        if (!(operator instanceof EntityComparisonOperator<?,?>))
            throw new IllegalArgumentException(name + " is not a comparison operator");
        return UtilGenerics.cast(operator);
    }

    public static EntityJoinOperator lookupJoin(String name) {
        EntityOperator<?,?,Boolean> operator = lookup(name);
        if (!(operator instanceof EntityJoinOperator))
            throw new IllegalArgumentException(name + " is not a join operator");
        return UtilGenerics.cast(operator);
    }

    public static int requestId() {
        return dynamicId.get();
    }

    public static final EntityComparisonOperator<?,?> EQUALS = new ComparableEntityComparisonOperator<Object>(ID_EQUALS, "=") {
        @Override
        public boolean compare(Comparable<Object> lhs, Object rhs) { return EntityComparisonOperator.compareEqual(lhs, rhs); }
        @Override
        protected void makeRHSWhereString(Entity entity, List<EntityConditionParam> entityConditionParams, StringBuilder sb, Field field, Object rhs, DatasourceInfo datasourceInfo) {
            if (rhs == null || rhs == GenericEntity.NULL_FIELD) {
                //Debug.logInfo("makeRHSWhereString: field IS NULL: " + field.getName(), module);
                sb.append(" IS NULL");
            } else {
                //Debug.logInfo("makeRHSWhereString: field not null, doing super: " + field.getName() + ", type: " + rhs.getClass().getName() + ", value: " + rhs, module);
                super.makeRHSWhereString(entity, entityConditionParams, sb, field, rhs, datasourceInfo);
            }
        }
    };
    static { register("equals", EQUALS); }
    static { register("=", EQUALS); }
    public static final EntityComparisonOperator<?,?> NOT_EQUAL = new ComparableEntityComparisonOperator<Object>(ID_NOT_EQUAL, "<>") {
        @Override
        public boolean compare(Comparable<Object> lhs, Object rhs) { return EntityComparisonOperator.compareNotEqual(lhs, rhs); }
        @Override
        protected void makeRHSWhereString(Entity entity, List<EntityConditionParam> entityConditionParams, StringBuilder sb, Field field, Object rhs, DatasourceInfo datasourceInfo) {
            if (rhs == null || rhs == GenericEntity.NULL_FIELD) {
                sb.append(" IS NOT NULL");
            } else {
                super.makeRHSWhereString(entity, entityConditionParams, sb, field, rhs, datasourceInfo);
            }
        }
    };
    static { register("not-equal", NOT_EQUAL); }
    static { register("not-equals", NOT_EQUAL); }
    static { register("notEqual", NOT_EQUAL); }
    static { register("!=", NOT_EQUAL); }
    static { register("<>", NOT_EQUAL); }
    public static final EntityComparisonOperator<?,?> LESS_THAN = new ComparableEntityComparisonOperator<Object>(ID_LESS_THAN, "<") {
        @Override
        public boolean compare(Comparable<Object> lhs, Object rhs) { return EntityComparisonOperator.compareLessThan(lhs, rhs); }
    };
    static { register("less", LESS_THAN); }
    static { register("less-than", LESS_THAN); }
    static { register("lessThan", LESS_THAN); }
    static { register("<", LESS_THAN); }
    public static final EntityComparisonOperator<?,?> GREATER_THAN = new ComparableEntityComparisonOperator<Object>(ID_GREATER_THAN, ">") {
        @Override
        public boolean compare(Comparable<Object> lhs, Object rhs) { return EntityComparisonOperator.compareGreaterThan(lhs, rhs); }
    };
    static { register("greater", GREATER_THAN); }
    static { register("greater-than", GREATER_THAN); }
    static { register("greaterThan", GREATER_THAN); }
    static { register(">", GREATER_THAN); }
    public static final EntityComparisonOperator<?,?> LESS_THAN_EQUAL_TO = new ComparableEntityComparisonOperator<Object>(ID_LESS_THAN_EQUAL_TO, "<=") {
        @Override
        public boolean compare(Comparable<Object> lhs, Object rhs) { return EntityComparisonOperator.compareLessThanEqualTo(lhs, rhs); }
    };
    static { register("less-equals", LESS_THAN_EQUAL_TO); }
    static { register("less-than-equal-to", LESS_THAN_EQUAL_TO); }
    static { register("lessThanEqualTo", LESS_THAN_EQUAL_TO); }
    static { register("<=", LESS_THAN_EQUAL_TO); }
    public static final EntityComparisonOperator<?,?> GREATER_THAN_EQUAL_TO = new ComparableEntityComparisonOperator<Object>(ID_GREATER_THAN_EQUAL_TO, ">=") {
        @Override
        public boolean compare(Comparable<Object> lhs, Object rhs) { return EntityComparisonOperator.compareGreaterThanEqualTo(lhs, rhs); }
    };
    static { register("greater-equals", GREATER_THAN_EQUAL_TO); }
    static { register("greater-than-equal-to", GREATER_THAN_EQUAL_TO); }
    static { register("greaterThanEqualTo", GREATER_THAN_EQUAL_TO); }
    static { register(">=", GREATER_THAN_EQUAL_TO); }
    public static final EntityComparisonOperator<?,?> IN = new CollectionEntityComparisonOperator<Object>(ID_IN, "IN") {
        @Override
        public boolean compare(Comparable<Object> lhs, Collection<Comparable<Object>> rhs) { return EntityComparisonOperator.compareIn(lhs, rhs); }
        @Override
        protected void makeRHSWhereStringValue(Entity entity, List<EntityConditionParam> entityConditionParams, StringBuilder sb, Field field, Collection<Comparable<Object>> rhs, DatasourceInfo datasourceInfo) { appendRHSList(entityConditionParams, sb, field, rhs); }
    };
    static { register("in", IN); }
    public static final EntityComparisonOperator<?,?> BETWEEN = new CollectionEntityComparisonOperator<Object>(ID_BETWEEN, "BETWEEN") {
        @Override
        public boolean compare(Comparable<Object> lhs, Collection<Comparable<Object>> rhs) { return EntityComparisonOperator.compareIn(lhs, rhs); }
        @Override
        protected void makeRHSWhereStringValue(Entity entity, List<EntityConditionParam> entityConditionParams, StringBuilder sb, Field field, Collection<Comparable<Object>> rhs, DatasourceInfo datasourceInfo) { appendRHSBetweenList(entityConditionParams, sb, field, rhs); }
    };
    static { register("between", BETWEEN); }
    public static final EntityComparisonOperator<?,?> NOT = new EntityComparisonOperator<Object, EntityCondition>(ID_NOT, "NOT") {
        @Override
        public boolean compare(Object lhs, EntityCondition rhs) { throw new UnsupportedOperationException(); }
    };
    static { register("not", NOT); }
    public static final EntityJoinOperator AND = new EntityJoinOperator(ID_AND, "AND", false);
    static { register("and", AND); }
    public static final EntityJoinOperator OR = new EntityJoinOperator(ID_OR, "OR", true);
    static { register("or", OR); }
    public static final EntityComparisonOperator<?,?> LIKE = new ComparableEntityComparisonOperator<Object>(ID_LIKE, "LIKE") {
        @Override
        public boolean compare(Comparable<Object> lhs, Object rhs) { return EntityComparisonOperator.compareLike(lhs, rhs); }
    };
    static { register("like", LIKE); }
    public static final EntityComparisonOperator<?,?> NOT_LIKE = new ComparableEntityComparisonOperator<Object>(ID_NOT_LIKE, "NOT LIKE") {
        @Override
        public boolean compare(Comparable<Object> lhs, Object rhs) { return !EntityComparisonOperator.compareLike(lhs, rhs); }
    };
    static { register("not-like", NOT_LIKE); }
    public static final EntityComparisonOperator<?,?> NOT_IN = new CollectionEntityComparisonOperator<Object>(ID_NOT_IN, "NOT IN") {
        @Override
        public boolean compare(Comparable<Object> lhs, Collection<Comparable<Object>> rhs) { return !EntityComparisonOperator.compareIn(lhs, rhs); }
        @Override
        protected void makeRHSWhereStringValue(Entity entity, List<EntityConditionParam> entityConditionParams, StringBuilder sb, Field field, Collection<Comparable<Object>> rhs, DatasourceInfo datasourceInfo) { appendRHSList(entityConditionParams, sb, field, rhs); }
    };
    static { register("not-in", NOT_IN); }

    protected int idInt;
    protected String codeString;

    public EntityOperator(int id, String code) {
        idInt = id;
        codeString = code;
    }

    public String getCode() {
        if (codeString == null) {
            return "null";
        } else {
            return codeString;
        }
    }

    public int getId() {
        return idInt;
    }

    @Override
    public String toString() {
        return codeString;
    }

    @Override
    public int hashCode() {
        return this.codeString.hashCode();
    }

    // FIXME: CCE
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EntityOperator<?,?,?>) {
            EntityOperator<?,?,?> otherOper = UtilGenerics.cast(obj);
            return this.idInt == otherOper.idInt;
        }
        return false;
    }

    public boolean entityMatches(GenericEntity entity, L lhs, R rhs) {
        return mapMatches(entity.getDelegator(), entity, lhs, rhs);
    }

    protected void appendRHSList(List<EntityConditionParam> entityConditionParams, StringBuilder whereStringBuilder, Field field, R rhs) {
        whereStringBuilder.append('(');

        if (rhs instanceof Collection<?>) {
            Iterator<R> rhsIter = UtilGenerics.<Collection<R>>cast(rhs).iterator();

            while (rhsIter.hasNext()) {
                Object inObj = rhsIter.next();

                addValue(whereStringBuilder, field, inObj, entityConditionParams);
                if (rhsIter.hasNext()) {
                    whereStringBuilder.append(", ");
                }
            }
        } else {
            addValue(whereStringBuilder, field, rhs, entityConditionParams);
        }
        whereStringBuilder.append(')');
    }

    protected <X> void appendRHSBetweenList(List<EntityConditionParam> entityConditionParams, StringBuilder whereStringBuilder, Field field, X rhs) {
        if (rhs instanceof Collection<?>) {
            Iterator<R> rhsIter = UtilGenerics.<Collection<R>>cast(rhs).iterator();

            while (rhsIter.hasNext()) {
                Object inObj = rhsIter.next();

                addValue(whereStringBuilder, field, inObj, entityConditionParams);
                if (rhsIter.hasNext()) {
                    whereStringBuilder.append(" AND ");
                }
            }
        }
    }


    /*
    public T eval(Delegator delegator, Map<String, ? extends Object> map, Object lhs, Object rhs) {
        return castBoolean(mapMatches(delegator, map, lhs, rhs));
    }
    */

    public abstract boolean isEmpty(L lhs, R rhs);
    public abstract boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map, L lhs, R rhs);
    public abstract void validateSql(Entity entity, L lhs, R rhs) throws GenericModelException;
    public void addSqlValue(StringBuilder sql, Entity entity, List<EntityConditionParam> entityConditionParams, L lhs, R rhs, DatasourceInfo datasourceInfo) {
        addSqlValue(sql, entity, entityConditionParams, true, lhs, rhs, datasourceInfo);
    }

    public abstract void addSqlValue(StringBuilder sql, Entity entity, List<EntityConditionParam> entityConditionParams, boolean compat, L lhs, R rhs, DatasourceInfo datasourceInfo);
    public abstract EntityCondition freeze(L lhs, R rhs);
    public abstract void visit(EntityConditionVisitor visitor, L lhs, R rhs);

    public static final Comparable<?> WILDCARD = new Comparable<Object>() {
        public int compareTo(Object obj) {
            if (obj != WILDCARD) throw new ClassCastException();
            return 0;
        }

        @Override
        public String toString() {
            return "(WILDCARD)";
        }
    };

    /**
     * Comparison operator for <code>Collection</code> types.
     *
     * @param <E>
     */
    public static abstract class CollectionEntityComparisonOperator<E> extends EntityComparisonOperator<Comparable<E>, Collection<Comparable<E>>> {
        public CollectionEntityComparisonOperator(int id, String code) {
            super(id, code);
        }
    }

    /**
     * Comparison operator for <code>Comparable</code> types.
     *
     * @param <E>
     */
    public static abstract class ComparableEntityComparisonOperator<E> extends EntityComparisonOperator<Comparable<E>, E> {
        public ComparableEntityComparisonOperator(int id, String code) {
            super(id, code);
        }
    }
}