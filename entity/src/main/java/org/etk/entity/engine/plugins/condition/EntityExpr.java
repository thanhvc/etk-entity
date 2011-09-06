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
import java.util.List;
import java.util.Map;

import org.etk.entity.base.utils.ObjectType;
import org.etk.entity.base.utils.UtilGenerics;
import org.etk.entity.engine.api.Delegator;
import org.etk.entity.engine.core.DelegatorFactory;
import org.etk.entity.engine.core.GenericEntity;
import org.etk.entity.engine.core.GenericEntityException;
import org.etk.entity.engine.plugins.model.xml.Entity;
import org.etk.entity.engine.plugins.model.xml.Field;

import javolution.context.ObjectFactory;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 6, 2011  
 */
/**
 * Encapsulates simple expressions used for specifying queries
 *
 */
@SuppressWarnings("serial")
public class EntityExpr extends EntityCondition {
    public static final String module = EntityExpr.class.getName();

    protected static final ObjectFactory<EntityExpr> entityExprFactory = new ObjectFactory<EntityExpr>() {
        @Override
        protected EntityExpr create() {
            return new EntityExpr();
        }
    };

    private Object lhs = null;
    private EntityOperator<Object, Object, ?> operator = null;
    private Object rhs = null;

    protected EntityExpr() {}

    public <L,R,LL,RR> void init(L lhs, EntityComparisonOperator<LL,RR> operator, R rhs) {
        if (lhs == null) {
            throw new IllegalArgumentException("The field name/value cannot be null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("The operator argument cannot be null");
        }

        if (rhs == null || rhs == GenericEntity.NULL_FIELD) {
            if (!EntityOperator.NOT_EQUAL.equals(operator) && !EntityOperator.EQUALS.equals(operator)) {
                throw new IllegalArgumentException("Operator must be EQUALS or NOT_EQUAL when right/rhs argument is NULL ");
            }
        }

        if (EntityOperator.BETWEEN.equals(operator)) {
            if (!(rhs instanceof Collection<?>) || (((Collection<?>) rhs).size() != 2)) {
                throw new IllegalArgumentException("BETWEEN Operator requires a Collection with 2 elements for the right/rhs argument");
            }
        }

        if (lhs instanceof String) {
            this.lhs = EntityFieldValue.makeFieldValue((String) lhs);
        } else {
            this.lhs = lhs;
        }
        this.operator = UtilGenerics.cast(operator);
        this.rhs = rhs;

        //Debug.logInfo("new EntityExpr internal field=" + lhs + ", value=" + rhs + ", value type=" + (rhs == null ? "null object" : rhs.getClass().getName()), module);
    }

    public void init(EntityCondition lhs, EntityJoinOperator operator, EntityCondition rhs) {
        if (lhs == null) {
            throw new IllegalArgumentException("The left EntityCondition argument cannot be null");
        }
        if (rhs == null) {
            throw new IllegalArgumentException("The right EntityCondition argument cannot be null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("The operator argument cannot be null");
        }

        this.lhs = lhs;
        this.operator = UtilGenerics.cast(operator);
        this.rhs = rhs;
    }

    public void reset() {
        this.lhs = null;
        this.operator = null;
        this.rhs = null;
    }

    public Object getLhs() {
        return lhs;
    }

    public <L,R,T> EntityOperator<L,R,T> getOperator() {
        return UtilGenerics.cast(operator);
    }

    public Object getRhs() {
        return rhs;
    }

    @Override
    public boolean isEmpty() {
        return operator.isEmpty(lhs, rhs);
    }

    @Override
    public String makeWhereString(Entity modelEntity, List<EntityConditionParam> entityConditionParams, DatasourceInfo datasourceInfo) {
        // if (Debug.verboseOn()) Debug.logVerbose("makeWhereString for entity " + modelEntity.getEntityName(), module);

        this.checkRhsType(modelEntity, null);

        StringBuilder sql = new StringBuilder();
        operator.addSqlValue(sql, modelEntity, entityConditionParams, true, lhs, rhs, datasourceInfo);
        return sql.toString();
    }

    @Override
    public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map) {
        return operator.mapMatches(delegator, map, lhs, rhs);
    }

    @Override
    public void checkCondition(Entity modelEntity) throws GenericModelException {
        // if (Debug.verboseOn()) Debug.logVerbose("checkCondition for entity " + modelEntity.getEntityName(), module);
        if (lhs instanceof EntityCondition) {
            ((EntityCondition) lhs).checkCondition(modelEntity);
            ((EntityCondition) rhs).checkCondition(modelEntity);
        }
    }

    @Override
    protected void addValue(StringBuilder buffer, Field field, Object value, List<EntityConditionParam> params) {
        if (rhs instanceof EntityFunction.UPPER) {
            if (value instanceof String) {
                value = ((String) value).toUpperCase();
            }
        }
        super.addValue(buffer, field, value, params);
    }

    @Override
    public EntityCondition freeze() {
        return operator.freeze(lhs, rhs);
    }

    @Override
    public void encryptConditionFields(Entity modelEntity, Delegator delegator) {
        if (this.lhs instanceof String) {
            Field modelField = modelEntity.getField((String) this.lhs);
            if (modelField != null && modelField.getEncrypt()) {
                if (!(rhs instanceof EntityConditionValue)) {
                    try {
                        this.rhs = delegator.encryptFieldValue(modelEntity.getEntityName(), this.rhs);
                    } catch (EntityCryptoException e) {
                        Debug.logWarning(e, "Error encrypting field [" + modelEntity.getEntityName() + "." + modelField.getName() + "] with value: " + this.rhs, module);
                    }
                }
            }
        }
    }

    @Override
    public void visit(EntityConditionVisitor visitor) {
        visitor.acceptEntityOperator(operator, lhs, rhs);
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.acceptEntityExpr(this);
    }

    public void checkRhsType(Entity modelEntity, Delegator delegator) {
        if (this.rhs == null || this.rhs == GenericEntity.NULL_FIELD || modelEntity == null) return;

        Object value = this.rhs;
        if (this.rhs instanceof EntityFunction<?>) {
            value = UtilGenerics.<EntityFunction<?>>cast(this.rhs).getOriginalValue();
        }

        if (value instanceof Collection<?>) {
            Collection<?> valueCol = UtilGenerics.cast(value);
            if (valueCol.size() > 0) {
                value = valueCol.iterator().next();
            } else {
                value = null;
            }
        }

        if (delegator == null) {
            // this will be the common case for now as the delegator isn't available where we want to do this
            // we'll cheat a little here and assume the default delegator
            delegator = DelegatorFactory.getDelegator("default");
        }

        String fieldName = null;
        Field curField;
        if (this.lhs instanceof EntityFieldValue) {
            EntityFieldValue efv = (EntityFieldValue) this.lhs;
            fieldName = efv.getFieldName();
            curField = efv.getModelField(modelEntity);
        } else {
            // nothing to check
            return;
        }

        if (curField == null) {
            throw new IllegalArgumentException("FieldName " + fieldName + " not found for entity: " + modelEntity.getEntityName());
        }
        FieldType type = null;
        try {
            type = delegator.getEntityFieldType(modelEntity, curField.getType());
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        if (type == null) {
            throw new IllegalArgumentException("Type " + curField.getType() + " not found for entity [" + modelEntity.getEntityName() + "]; probably because there is no datasource (helper) setup for the entity group that this entity is in: [" + delegator.getEntityGroupName(modelEntity.getEntityName()) + "]");
        }
        if (value instanceof EntityConditionSubSelect){
            FieldType valueType = null;
            try {
                Entity valueModelEntity= ((EntityConditionSubSelect) value).getModelEntity();
                valueType = delegator.getEntityFieldType(valueModelEntity,  valueModelEntity.getField(((EntityConditionSubSelect) value).getKeyFieldName()).getType());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
          // make sure the type of keyFieldName of EntityConditionSubSelect  matches the field Java type
            try {
                if (!ObjectType.instanceOf(ObjectType.loadClass(valueType.getJavaType()), type.getJavaType())) {
                    String errMsg = "Warning using ["+ value.getClass().getName() + "] and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]. The Java type of keyFieldName : [" + valueType.getJavaType()+ "] is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                    // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                    Debug.logWarning(new Exception("Location of database type warning"), "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + errMsg, module);
                }
            } catch (ClassNotFoundException e) {
                String errMsg = "Warning using ["+ value.getClass().getName() + "] and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]. The Java type of keyFieldName : [" + valueType.getJavaType()+ "] could not be found]";
                // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                Debug.logWarning(e, "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + errMsg, module);
             }
        } else if (value instanceof EntityFieldValue) {
            EntityFieldValue efv = (EntityFieldValue) this.lhs;
            String rhsFieldName = efv.getFieldName();
            Field rhsField = efv.getModelField(modelEntity);
            if (rhsField == null) {
                throw new IllegalArgumentException("FieldName " + rhsFieldName + " not found for entity: " + modelEntity.getEntityName());
            }
            FieldType rhsType = null;
            try {
                rhsType = delegator.getEntityFieldType(modelEntity, rhsField.getType());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
            try {
                if (!ObjectType.instanceOf(ObjectType.loadClass(rhsType.getJavaType()), type.getJavaType())) {
                    String errMsg = "Warning using ["+ value.getClass().getName() + "] and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]. The Java type [" + rhsType.getJavaType() + "] of rhsFieldName : [" + rhsFieldName + "] is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                    // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                    Debug.logWarning(new Exception("Location of database type warning"), "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=- " + errMsg, module);
                }
            } catch (ClassNotFoundException e) {
                String errMsg = "Warning using ["+ value.getClass().getName() + "] and entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "]. The Java type [" + rhsType.getJavaType() + "] of rhsFieldName : [" + rhsFieldName + "] could not be found]";
                // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                Debug.logWarning(e, "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + errMsg, module);
            }
        } else {
        // make sure the type matches the field Java type
            if (!ObjectType.instanceOf(value, type.getJavaType())) {
                String errMsg = "In entity field [" + modelEntity.getEntityName() + "." + curField.getName() + "] set the value passed in [" + value.getClass().getName() + "] is not compatible with the Java type of the field [" + type.getJavaType() + "]";
                // eventually we should do this, but for now we'll do a "soft" failure: throw new IllegalArgumentException(errMsg);
                Debug.logWarning(new Exception("Location of database type warning"), "=-=-=-=-=-=-=-=-= Database type warning in EntityExpr =-=-=-=-=-=-=-=-= " + errMsg, module);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityExpr)) return false;
        EntityExpr other = (EntityExpr) obj;
        boolean isEqual = equals(lhs, other.lhs) &&
               equals(operator, other.operator) &&
               equals(rhs, other.rhs);
        //if (!isEqual) {
        //    Debug.logWarning("EntityExpr.equals is false for: \n-this.lhs=" + this.lhs + "; other.lhs=" + other.lhs +
        //            "\nthis.operator=" + this.operator + "; other.operator=" + other.operator +
        //            "\nthis.rhs=" + this.rhs + "other.rhs=" + other.rhs, module);
        //}
        return isEqual;
    }

    @Override
    public int hashCode() {
        return hashCode(lhs) +
               hashCode(operator) +
               hashCode(rhs);
    }
}