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

import java.io.Serializable;

import org.etk.entity.engine.plugins.model.xml.Field;

/**
 * Represents a single parameter to be used in the preparedStatement
 *
 */
@SuppressWarnings("serial")
public class EntityConditionParam implements Serializable {
    protected Field modelField;
    protected Object fieldValue;

    protected EntityConditionParam() {}

    public EntityConditionParam(Field modelField, Object fieldValue) {
        if (modelField == null) {
            throw new IllegalArgumentException("modelField cannot be null");
        }
        this.modelField = modelField;
        this.fieldValue = fieldValue;
    }

    public Field getModelField() {
        return modelField;
    }

    public Object getFieldValue() {
        return fieldValue;
    }

    @Override
    public String toString() {
        return modelField.getColName() + "=" + fieldValue.toString();
    }
}
