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

import java.util.Comparator;

import org.etk.entity.base.utils.UtilGenerics;
import org.etk.entity.engine.core.GenericEntity;
import org.etk.entity.engine.core.GenericModelException;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.model.xml.Entity;

public class OrderByItem implements Comparator<GenericEntity> {
  public static final int DEFAULT = 0;
  public static final int UPPER   = 1;
  public static final int LOWER   = 2;

  public static final String NULLS_FIRST = "NULLS FIRST";
  public static final String NULLS_LAST = "NULLS LAST";

  protected boolean descending;
  protected Boolean nullsFirst;
  protected EntityConditionValue value;

  public OrderByItem(EntityConditionValue value) {
      this.value = value;
  }

  public OrderByItem(EntityConditionValue value, boolean descending) {
      this(value);
      this.descending = descending;
  }

  public OrderByItem(EntityConditionValue value, boolean descending, Boolean nullsFirst) {
      this(value, descending);
      this.nullsFirst = nullsFirst;
  }

  public EntityConditionValue getValue() {
      return value;
  }

  public boolean getDescending() {
      return descending;
  }

  public static final OrderByItem parse(Object obj) {
      if (obj instanceof String) {
          return parse((String) obj);
      } else if (obj instanceof EntityConditionValue) {
          return new OrderByItem((EntityConditionValue) obj, false);
      } else if (obj instanceof OrderByItem) {
          return (OrderByItem) obj;
      } else {
          throw new IllegalArgumentException("unknown orderBy item: " + obj);
      }
  }

  public static final OrderByItem parse(String text) {
      text = text.trim();

      // handle nulls first/last
      Boolean nullsFirst = null;
      if (text.toUpperCase().endsWith(NULLS_FIRST)) {
          nullsFirst = true;
          text = text.substring(0, text.length() - NULLS_FIRST.length()).trim();
      }

      if (text.toUpperCase().endsWith(NULLS_LAST)) {
          nullsFirst = false;
          text = text.substring(0, text.length() - NULLS_LAST.length()).trim();
      }

      int startIndex = 0, endIndex = text.length();
      boolean descending;
      int caseSensitivity;
      if (text.endsWith(" DESC")) {
          descending = true;
          endIndex -= 5;
      } else if (text.endsWith(" ASC")) {
          descending = false;
          endIndex -= 4;
      } else if (text.startsWith("-")) {
          descending = true;
          startIndex++;
      } else if (text.startsWith("+")) {
          descending = false;
          startIndex++;
      } else {
          descending = false;
      }

      if (startIndex != 0 || endIndex != text.length()) {
          text = text.substring(startIndex, endIndex);
          startIndex = 0;
          endIndex = text.length();
      }

      if (text.endsWith(")")) {
          String upperText = text.toUpperCase();
          endIndex--;
          if (upperText.startsWith("UPPER(")) {
              caseSensitivity = UPPER;
              startIndex = 6;
          } else if (upperText.startsWith("LOWER(")) {
              caseSensitivity = LOWER;
              startIndex = 6;
          } else {
              caseSensitivity = DEFAULT;
          }
      } else {
          caseSensitivity = DEFAULT;
      }

      if (startIndex != 0 || endIndex != text.length()) {
          text = text.substring(startIndex, endIndex);
          startIndex = 0;
          endIndex = text.length();
      }
      EntityConditionValue value = EntityFieldValue.makeFieldValue(text);
      switch (caseSensitivity) {
          case UPPER:
              value = EntityFunction.UPPER(value);
              break;
          case LOWER:
              value = EntityFunction.LOWER(value);
              break;
      }
      return new OrderByItem(value, descending, nullsFirst);
  }

  public void checkOrderBy(Entity modelEntity) throws GenericModelException {
      value.validateSql(modelEntity);
  }

  public int compare(GenericEntity obj1, GenericEntity obj2) {
      Comparable<Object> value1 = UtilGenerics.cast(value.getValue(obj1));
      Object value2 = value.getValue(obj2);

      int result;
      // null is defined as the largest possible value
      if (value1 == null) {
          result = value2 == null ? 0 : 1;
      } else if (value2 == null) {
          result = value1 == null ? 0 : -1;
      } else {
          result = value1.compareTo(value2);
      }
      // if (Debug.infoOn()) Debug.logInfo("[OrderByComparator.compareAsc] Result is " + result + " for [" + value + "] and [" + value2 + "]", module);
      return descending ? -result : result;
  }

  public String makeOrderByString(Entity modelEntity, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) {
      StringBuilder sb = new StringBuilder();
      makeOrderByString(sb, modelEntity, includeTablenamePrefix, datasourceInfo);
      return sb.toString();
  }

  public void makeOrderByString(StringBuilder sb, Entity modelEntity, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) {
      if ((nullsFirst != null) && (!datasourceInfo.useOrderByNulls)) {
          sb.append("CASE WHEN ");
          getValue().addSqlValue(sb, modelEntity, null, includeTablenamePrefix, datasourceInfo);
          sb.append(" IS NULL THEN ");
          sb.append(nullsFirst ? "0" : "1");
          sb.append(" ELSE ");
          sb.append(nullsFirst ? "1" : "0");
          sb.append(" END, ");
      }

      getValue().addSqlValue(sb, modelEntity, null, includeTablenamePrefix, datasourceInfo);
      sb.append(descending ? " DESC" : " ASC");

      if ((nullsFirst != null) && (datasourceInfo.useOrderByNulls)) {
          sb.append(nullsFirst ? " NULLS FIRST" : " NULLS LAST");
      }
  }

  @Override
  public boolean equals(java.lang.Object obj) {
      if (!(obj instanceof OrderByItem)) return false;
      OrderByItem that = (OrderByItem) obj;

      return getValue().equals(that.getValue()) && getDescending() == that.getDescending();
  }

  @Override
  public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getValue());
      sb.append(descending ? " DESC" : " ASC");
      if (nullsFirst != null) {
          sb.append(nullsFirst ? " NULLS FIRST" : " NULLS LAST");
      }
      return sb.toString();
  }
}