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

import org.apache.oro.text.regex.MalformedPatternException;
import org.etk.common.logging.Logger;
import org.etk.entity.base.utils.UtilGenerics;
import org.etk.entity.base.utils.UtilValidate;
import org.etk.entity.engine.api.Delegator;
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
 * Base class for comparisons.
 */
@SuppressWarnings("serial")
public abstract class EntityComparisonOperator<L, R> extends EntityOperator<L, R, Boolean> {

  public static final String  module = EntityComparisonOperator.class.getName();
  private static final Logger logger = Logger.getLogger(EntityComparisonOperator.class);

  protected transient static ThreadLocal<CompilerMatcher> compilerMatcher = CompilerMatcher.getThreadLocal();

  public static String makeOroPattern(String sqlLike) {
    try {
      sqlLike = compilerMatcher.get().substitute("s/([$^.+*?])/\\\\$1/g", sqlLike);
      sqlLike = compilerMatcher.get().substitute("s/%/.*/g", sqlLike);
      sqlLike = compilerMatcher.get().substitute("s/_/./g", sqlLike);
    } catch (Throwable t) {
      String errMsg = "Error in ORO pattern substitution for SQL like clause [" + sqlLike + "]: "
          + t.toString();
      logger.error(errMsg, t);
      throw new IllegalArgumentException(errMsg);
    }
    return sqlLike;
  }

  @Override
  public void validateSql(Entity entity, L lhs, R rhs) throws GenericModelException {
    if (lhs instanceof EntityConditionValue) {
      EntityConditionValue ecv = (EntityConditionValue) lhs;
      ecv.validateSql(entity);
    }
    if (rhs instanceof EntityConditionValue) {
      EntityConditionValue ecv = (EntityConditionValue) rhs;
      ecv.validateSql(entity);
    }
  }

  @Override
  public void visit(EntityConditionVisitor visitor, L lhs, R rhs) {
    visitor.accept(lhs);
    visitor.accept(rhs);
  }

  @Override
  public void addSqlValue(StringBuilder sql,
                          Entity entity,
                          List<EntityConditionParam> entityConditionParams,
                          boolean compat,
                          L lhs,
                          R rhs,
                          DatasourceInfo datasourceInfo) {
    // Debug.logInfo("EntityComparisonOperator.addSqlValue field=" + lhs +
    // ", value=" + rhs + ", value type=" + (rhs == null ? "null object" :
    // rhs.getClass().getName()), module);

    // if this is an IN operator and the rhs Object isEmpty, add "1=0" instead
    // of the normal SQL. Note that "FALSE" does not work with all databases.
    if (this.idInt == EntityOperator.ID_IN && UtilValidate.isEmpty(rhs)) {
      sql.append("1=0");
      return;
    }

    Field field;
    if (lhs instanceof EntityConditionValue) {
      EntityConditionValue ecv = (EntityConditionValue) lhs;
      ecv.addSqlValue(sql, entity, entityConditionParams, false, datasourceInfo);
      field = ecv.getModelField(entity);
    } else if (compat && lhs instanceof String) {
      field = getField(entity, (String) lhs);
      if (field == null) {
        sql.append(lhs);
      } else {
        sql.append(field.getColName());
      }
    } else {
      addValue(sql, null, lhs, entityConditionParams);
      field = null;
    }

    makeRHSWhereString(entity, entityConditionParams, sql, field, rhs, datasourceInfo);
  }

  @Override
  public boolean isEmpty(L lhs, R rhs) {
    return false;
  }

  protected void makeRHSWhereString(Entity entity,
                                    List<EntityConditionParam> entityConditionParams,
                                    StringBuilder sql,
                                    Field field,
                                    R rhs,
                                    DatasourceInfo datasourceInfo) {
    sql.append(' ').append(getCode()).append(' ');
    makeRHSWhereStringValue(entity, entityConditionParams, sql, field, rhs, datasourceInfo);
  }

  protected void makeRHSWhereStringValue(Entity entity,
                                         List<EntityConditionParam> entityConditionParams,
                                         StringBuilder sql,
                                         Field field,
                                         R rhs,
                                         DatasourceInfo datasourceInfo) {
    if (rhs instanceof EntityConditionValue) {
      EntityConditionValue ecv = (EntityConditionValue) rhs;
      ecv.addSqlValue(sql, entity, entityConditionParams, false, datasourceInfo);
    } else {
      addValue(sql, field, rhs, entityConditionParams);
    }
  }

  public abstract boolean compare(L lhs, R rhs);

  public Boolean eval(Delegator delegator, Map<String, ? extends Object> map, L lhs, R rhs) {
    return Boolean.valueOf(mapMatches(delegator, map, lhs, rhs));
  }

  @Override
  public boolean mapMatches(Delegator delegator, Map<String, ? extends Object> map, L lhs, R rhs) {
    Object leftValue;
    if (lhs instanceof EntityConditionValue) {
      EntityConditionValue ecv = (EntityConditionValue) lhs;
      leftValue = ecv.getValue(delegator, map);
    } else if (lhs instanceof String) {
      leftValue = map.get(lhs);
    } else {
      leftValue = lhs;
    }
    Object rightValue;
    if (rhs instanceof EntityConditionValue) {
      EntityConditionValue ecv = (EntityConditionValue) rhs;
      rightValue = ecv.getValue(delegator, map);
    } else {
      rightValue = rhs;
    }

    if (leftValue == WILDCARD || rightValue == WILDCARD)
      return true;
    return compare(UtilGenerics.<L> cast(leftValue), UtilGenerics.<R> cast(rightValue));
  }

  @Override
  public EntityCondition freeze(L lhs, R rhs) {
    return EntityCondition.makeCondition(freeze(lhs), this, freeze(rhs));
  }

  protected Object freeze(Object item) {
    if (item instanceof EntityConditionValue) {
      EntityConditionValue ecv = (EntityConditionValue) item;
      return ecv.freeze();
    } else {
      return item;
    }
  }

  public EntityComparisonOperator(int id, String code) {
    super(id, code);
  }

  public static final <T> boolean compareEqual(Comparable<T> lhs, T rhs) {
    if (lhs == null) {
      if (rhs != null) {
        return false;
      }
    } else if (!lhs.equals(rhs)) {
      return false;
    }
    return true;
  }

  public static final <T> boolean compareNotEqual(Comparable<T> lhs, T rhs) {
    if (lhs == null) {
      if (rhs == null) {
        return false;
      }
    } else if (lhs.equals(rhs)) {
      return false;
    }
    return true;
  }

  public static final <T> boolean compareGreaterThan(Comparable<T> lhs, T rhs) {
    if (lhs == null) {
      if (rhs != null) {
        return false;
      }
    } else if (lhs.compareTo(rhs) <= 0) {
      return false;
    }
    return true;
  }

  public static final <T> boolean compareGreaterThanEqualTo(Comparable<T> lhs, T rhs) {
    if (lhs == null) {
      if (rhs != null) {
        return false;
      }
    } else if (lhs.compareTo(rhs) < 0) {
      return false;
    }
    return true;
  }

  public static final <T> boolean compareLessThan(Comparable<T> lhs, T rhs) {
    if (lhs == null) {
      if (rhs != null) {
        return false;
      }
    } else if (lhs.compareTo(rhs) >= 0) {
      return false;
    }
    return true;
  }

  public static final <T> boolean compareLessThanEqualTo(Comparable<T> lhs, T rhs) {
    if (lhs == null) {
      if (rhs != null) {
        return false;
      }
    } else if (lhs.compareTo(rhs) > 0) {
      return false;
    }
    return true;
  }

  public static final <L, R> boolean compareIn(L lhs, R rhs) {
    if (lhs == null) {
      if (rhs != null) {
        return false;
      } else {
        return true;
      }
    } else if (rhs instanceof Collection<?>) {
      if (((Collection<?>) rhs).contains(lhs)) {
        return true;
      } else {
        return false;
      }
    } else if (lhs.equals(rhs)) {
      return true;
    } else {
      return false;
    }
  }

  public static final <L, R> boolean compareLike(L lhs, R rhs) {
    if (lhs == null) {
      if (rhs != null) {
        return false;
      }
    } else if (lhs instanceof String && rhs instanceof String) {
      // see if the lhs value is like the rhs value, rhs will have the pattern
      // characters in it...
      try {
        return compilerMatcher.get().matches((String) lhs, makeOroPattern((String) rhs));
      } catch (MalformedPatternException e) {
        logger.error(e.getMessage(), e);
        return false;
      }
    }
    return true;
  }
}
