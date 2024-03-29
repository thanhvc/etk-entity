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
package org.etk.entity.engine.plugins.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javolution.util.FastMap;

import org.etk.entity.base.utils.UtilGenerics;
import org.etk.entity.base.utils.UtilValidate;
import org.etk.entity.engine.core.GenericDataSourceException;
import org.etk.entity.engine.core.GenericEntity;
import org.etk.entity.engine.core.GenericEntityException;
import org.etk.entity.engine.core.GenericModelException;
import org.etk.entity.engine.core.GenericNotImplementedException;
import org.etk.entity.engine.core.GenericValue;
import org.etk.entity.engine.plugins.condition.EntityCondition;
import org.etk.entity.engine.plugins.condition.EntityConditionParam;
import org.etk.entity.engine.plugins.condition.OrderByList;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.model.xml.Entity;
import org.etk.entity.engine.plugins.model.xml.Field;
import org.etk.entity.engine.plugins.model.xml.FieldType;
import org.etk.entity.engine.plugins.model.xml.ViewEntity;

/**
 * GenericDAO Utility methods for general tasks
 *
 */
public class SqlJdbcUtil {
    public static final String module = SqlJdbcUtil.class.getName();

    public static final int CHAR_BUFFER_SIZE = 4096;

    /** Makes the FROM clause and when necessary the JOIN clause(s) as well */
    public static String makeFromClause(Entity modelEntity, ModelFieldTypeReader modelFieldTypeReader, DatasourceInfo datasourceInfo) throws GenericEntityException {
        StringBuilder sql = new StringBuilder(" FROM ");

        if (modelEntity instanceof ViewEntity) {
            ViewEntity modelViewEntity = (ViewEntity) modelEntity;

            if ("ansi".equals(datasourceInfo.joinStyle) || "ansi-no-parenthesis".equals(datasourceInfo.joinStyle)) {
                boolean useParenthesis = true;
                if ("ansi-no-parenthesis".equals(datasourceInfo.joinStyle)) {
                    useParenthesis = false;
                }

                // FROM clause: in this case will be a bunch of joins that correspond with the view-links

                // BIG NOTE on the JOIN clauses: the order of joins is determined by the order of the
                // view-links; for more flexible order we'll have to figure something else out and
                // extend the DTD for the nested view-link elements or something

                // At this point it is assumed that in each view-link the left hand alias will
                // either be the first alias in the series or will already be in a previous
                // view-link and already be in the big join; SO keep a set of all aliases
                // in the join so far and if the left entity alias isn't there yet, and this
                // isn't the first one, throw an exception
                Set<String> joinedAliasSet = new TreeSet<String>();

                // TODO: at view-link read time make sure they are ordered properly so that each
                // left hand alias after the first view-link has already been linked before

                StringBuilder openParens = null;
                if (useParenthesis) openParens = new StringBuilder();
                StringBuilder restOfStatement = new StringBuilder();

                for (int i = 0; i < modelViewEntity.getViewLinksSize(); i++) {
                    // don't put starting parenthesis
                    if (i > 0 && useParenthesis) openParens.append('(');

                    ViewEntity.ModelViewLink viewLink = modelViewEntity.getViewLink(i);

                    Entity linkEntity = modelViewEntity.getMemberModelEntity(viewLink.getEntityAlias());
                    Entity relLinkEntity = modelViewEntity.getMemberModelEntity(viewLink.getRelEntityAlias());

                    // ModelViewEntity.ModelMemberEntity linkMemberEntity = modelViewEntity.getMemberModelMemberEntity(viewLink.getEntityAlias());
                    // ModelViewEntity.ModelMemberEntity relLinkMemberEntity = modelViewEntity.getMemberModelMemberEntity(viewLink.getRelEntityAlias());

                    if (i == 0) {
                        // this is the first referenced member alias, so keep track of it for future use...
                        restOfStatement.append(makeViewTable(linkEntity, modelFieldTypeReader, datasourceInfo));
                        //another possible one that some dbs might need, but not sure of any yet: restOfStatement.append(" AS ");
                        restOfStatement.append(" ");
                        restOfStatement.append(viewLink.getEntityAlias());

                        joinedAliasSet.add(viewLink.getEntityAlias());
                    } else {
                        // make sure the left entity alias is already in the join...
                        if (!joinedAliasSet.contains(viewLink.getEntityAlias())) {
                            throw new GenericModelException("Tried to link the " + viewLink.getEntityAlias() + " alias to the " + viewLink.getRelEntityAlias() + " alias of the " + modelViewEntity.getEntityName() + " view-entity, but it is not the first view-link and has not been included in a previous view-link. In other words, the left/main alias isn't connected to the rest of the member-entities yet.");
                        }
                    }
                    // now put the rel (right) entity alias into the set that is in the join
                    joinedAliasSet.add(viewLink.getRelEntityAlias());

                    if (viewLink.isRelOptional()) {
                        restOfStatement.append(" LEFT OUTER JOIN ");
                    } else {
                        restOfStatement.append(" INNER JOIN ");
                    }

                    restOfStatement.append(makeViewTable(relLinkEntity, modelFieldTypeReader, datasourceInfo));
                    //another possible one that some dbs might need, but not sure of any yet: restOfStatement.append(" AS ");
                    restOfStatement.append(" ");
                    restOfStatement.append(viewLink.getRelEntityAlias());
                    restOfStatement.append(" ON ");

                    StringBuilder condBuffer = new StringBuilder();

                    for (int j = 0; j < viewLink.getKeyMapsSize(); j++) {
                        ModelKeyMap keyMap = viewLink.getKeyMap(j);
                        Field linkField = linkEntity.getField(keyMap.getFieldName());
                        if (linkField == null) {
                            throw new GenericModelException("Invalid field name in view-link key-map for the " + viewLink.getEntityAlias() + " and the " + viewLink.getRelEntityAlias() + " member-entities of the " + modelViewEntity.getEntityName() + " view-entity; the field [" + keyMap.getFieldName() + "] does not exist on the [" + linkEntity.getEntityName() + "] entity.");
                        }
                        Field relLinkField = relLinkEntity.getField(keyMap.getRelFieldName());
                        if (relLinkField == null) {
                            throw new GenericModelException("Invalid related field name in view-link key-map for the " + viewLink.getEntityAlias() + " and the " + viewLink.getRelEntityAlias() + " member-entities of the " + modelViewEntity.getEntityName() + " view-entity; the field [" + keyMap.getRelFieldName() + "] does not exist on the [" + relLinkEntity.getEntityName() + "] entity.");
                        }

                        if (condBuffer.length() > 0) {
                            condBuffer.append(" AND ");
                        }

                        condBuffer.append(viewLink.getEntityAlias());
                        condBuffer.append(".");
                        condBuffer.append(linkField.getColName());

                        condBuffer.append(" = ");

                        condBuffer.append(viewLink.getRelEntityAlias());
                        condBuffer.append(".");
                        condBuffer.append(relLinkField.getColName());
                    }
                    if (condBuffer.length() == 0) {
                        throw new GenericModelException("No view-link/join key-maps found for the " + viewLink.getEntityAlias() + " and the " + viewLink.getRelEntityAlias() + " member-entities of the " + modelViewEntity.getEntityName() + " view-entity.");
                    }

                    ViewEntity.ViewEntityCondition viewEntityCondition = viewLink.getViewEntityCondition();
                    if (viewEntityCondition != null) {
                        EntityCondition whereCondition = viewEntityCondition.getWhereCondition(modelFieldTypeReader, null);
                        if (whereCondition != null) {
                            condBuffer.append(" AND ");
                            condBuffer.append(whereCondition.makeWhereString(modelEntity, null, datasourceInfo));
                        }
                    }

                    restOfStatement.append(condBuffer.toString());

                    // don't put ending parenthesis
                    if (i < (modelViewEntity.getViewLinksSize() - 1) && useParenthesis) restOfStatement.append(')');
                }

                if (useParenthesis) sql.append(openParens.toString());
                sql.append(restOfStatement.toString());

                // handle tables not included in view-link
                boolean fromEmpty = restOfStatement.length() == 0;
                for (String aliasName: modelViewEntity.getMemberModelMemberEntities().keySet()) {
                    Entity fromEntity = modelViewEntity.getMemberModelEntity(aliasName);

                    if (!joinedAliasSet.contains(aliasName)) {
                        if (!fromEmpty) sql.append(", ");
                        fromEmpty = false;

                        sql.append(makeViewTable(fromEntity, modelFieldTypeReader, datasourceInfo));
                        sql.append(" ");
                        sql.append(aliasName);
                    }
                }


            } else if ("theta-oracle".equals(datasourceInfo.joinStyle) || "theta-mssql".equals(datasourceInfo.joinStyle)) {
                // FROM clause
                Iterator<String> meIter = modelViewEntity.getMemberModelMemberEntities().keySet().iterator();

                while (meIter.hasNext()) {
                    String aliasName = meIter.next();
                    Entity fromEntity = modelViewEntity.getMemberModelEntity(aliasName);

                    sql.append(makeViewTable(fromEntity, modelFieldTypeReader, datasourceInfo));
                    sql.append(" ");
                    sql.append(aliasName);
                    if (meIter.hasNext()) sql.append(", ");
                }

                // JOIN clause(s): none needed, all the work done in the where clause for theta-oracle
            } else {
                throw new GenericModelException("The join-style " + datasourceInfo.joinStyle + " is not yet supported");
            }
        } else {
            sql.append(modelEntity.getTableName(datasourceInfo));
        }
        return sql.toString();
    }

    /** Makes a WHERE clause String with "<col name>=?" if not null or "<col name> IS null" if null, all AND separated */
    @Deprecated
    public static String makeWhereStringFromFields(List<Field> modelFields, Map<String, Object> fields, String operator) {
        return makeWhereStringFromFields(new StringBuilder(), modelFields, fields, operator, null).toString();
    }

    public static StringBuilder makeWhereStringFromFields(StringBuilder sb, List<Field> modelFields, Map<String, Object> fields, String operator) {
        return makeWhereStringFromFields(sb, modelFields, fields, operator, null);
    }

    /** Makes a WHERE clause String with "<col name>=?" if not null or "<col name> IS null" if null, all AND separated */
    @Deprecated
    public static String makeWhereStringFromFields(List<Field> modelFields, Map<String, Object> fields, String operator, List<EntityConditionParam> entityConditionParams) {
        return makeWhereStringFromFields(new StringBuilder(), modelFields, fields, operator, entityConditionParams).toString();
    }

    /** Makes a WHERE clause String with "<col name>=?" if not null or "<col name> IS null" if null, all AND separated */
    public static StringBuilder makeWhereStringFromFields(StringBuilder sb, List<ModelField> modelFields, Map<String, Object> fields, String operator, List<EntityConditionParam> entityConditionParams) {
        if (modelFields.size() < 1) {
            return sb;
        }

        Iterator<Field> iter = modelFields.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            Object name = null;
            Field modelField = null;
            if (item instanceof Field) {
                modelField = (Field) item;
                sb.append(modelField.getColValue());
                name = modelField.getName();
            } else {
                sb.append(item);
                name = item;
            }

            Object fieldValue = fields.get(name);
            if (fieldValue != null && fieldValue != GenericEntity.NULL_FIELD) {
                sb.append('=');
                addValue(sb, modelField, fieldValue, entityConditionParams);
            } else {
                sb.append(" IS NULL");
            }

            if (iter.hasNext()) {
                sb.append(' ');
                sb.append(operator);
                sb.append(' ');
            }
        }

        return sb;
    }

    public static String makeWhereClause(Entity modelEntity, List<Field> modelFields, Map<String, Object> fields, String operator, String joinStyle) throws GenericEntityException {
        StringBuilder whereString = new StringBuilder("");

        if (UtilValidate.isNotEmpty(modelFields)) {
            whereString.append(makeWhereStringFromFields(modelFields, fields, "AND"));
        }

        String viewClause = makeViewWhereClause(modelEntity, joinStyle);

        if (viewClause.length() > 0) {
            if (whereString.length() > 0) {
                whereString.append(' ');
                whereString.append(operator);
                whereString.append(' ');
            }

            whereString.append(viewClause);
        }

        if (whereString.length() > 0) {
            return " WHERE " + whereString.toString();
        }

        return "";
    }

    public static String makeViewWhereClause(Entity modelEntity, String joinStyle) throws GenericEntityException {
        if (modelEntity instanceof ViewEntity) {
            StringBuilder whereString = new StringBuilder();
            ViewEntity modelViewEntity = (ViewEntity) modelEntity;

            if ("ansi".equals(joinStyle) || "ansi-no-parenthesis".equals(joinStyle)) {
                // nothing to do here, all done in the JOIN clauses
            } else if ("theta-oracle".equals(joinStyle) || "theta-mssql".equals(joinStyle)) {
                boolean isOracleStyle = "theta-oracle".equals(joinStyle);
                boolean isMssqlStyle = "theta-mssql".equals(joinStyle);

                for (int i = 0; i < modelViewEntity.getViewLinksSize(); i++) {
                    ViewEntity.ModelViewLink viewLink = modelViewEntity.getViewLink(i);

                    Entity linkEntity = modelViewEntity.getMemberModelEntity(viewLink.getEntityAlias());
                    Entity relLinkEntity = modelViewEntity.getMemberModelEntity(viewLink.getRelEntityAlias());

                    if (linkEntity == null) {
                        throw new GenericEntityException("Link entity not found with alias: " + viewLink.getEntityAlias() + " for entity: " + modelViewEntity.getEntityName());
                    }

                    if (relLinkEntity == null) {
                        throw new GenericEntityException("Rel-Link entity not found with alias: " + viewLink.getRelEntityAlias() + " for entity: " + modelViewEntity.getEntityName());
                    }

                    // ModelViewEntity.ModelMemberEntity linkMemberEntity = modelViewEntity.getMemberModelMemberEntity(viewLink.getEntityAlias());
                    // ModelViewEntity.ModelMemberEntity relLinkMemberEntity = modelViewEntity.getMemberModelMemberEntity(viewLink.getRelEntityAlias());

                    for (int j = 0; j < viewLink.getKeyMapsSize(); j++) {
                        ModelKeyMap keyMap = viewLink.getKeyMap(j);
                        Field linkField = linkEntity.getField(keyMap.getFieldName());
                        Field relLinkField = relLinkEntity.getField(keyMap.getRelFieldName());

                        if (whereString.length() > 0) {
                            whereString.append(" AND ");
                        }
                        whereString.append(viewLink.getEntityAlias());
                        whereString.append(".");
                        whereString.append(linkField.getColName());

                        // check to see whether the left or right members are optional, if so:
                        // oracle: use the (+) on the optional side
                        // mssql: use the * on the required side

                        // NOTE: not testing if original table is optional, ONLY if related table is optional; otherwise things get really ugly...
                        // if (isOracleStyle && linkMemberEntity.getOptional()) whereString.append(" (+) ");
                        if (isMssqlStyle && viewLink.isRelOptional()) whereString.append("*");
                        whereString.append("=");
                        // if (isMssqlStyle && linkMemberEntity.getOptional()) whereString.append("*");
                        if (isOracleStyle && viewLink.isRelOptional()) whereString.append(" (+) ");

                        whereString.append(viewLink.getRelEntityAlias());
                        whereString.append(".");
                        whereString.append(relLinkField.getColName());
                   }
                }
            } else {
                throw new GenericModelException("The join-style " + joinStyle + " is not supported");
            }

            if (whereString.length() > 0) {
                return "(" + whereString.toString() + ")";
            }
        }
        return "";
    }

    public static String makeOrderByClause(Entity modelEntity, List<String> orderBy, DatasourceInfo datasourceInfo) throws GenericModelException {
        return makeOrderByClause(modelEntity, orderBy, false, datasourceInfo);
    }

    public static String makeOrderByClause(Entity modelEntity, List<String> orderBy, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) throws GenericModelException {
        StringBuilder sql = new StringBuilder("");
        //String fieldPrefix = includeTablenamePrefix ? (modelEntity.getTableName(datasourceInfo) + ".") : "";

        if (UtilValidate.isNotEmpty(orderBy)) {
            if (Debug.verboseOn()) Debug.logVerbose("Order by list contains: " + orderBy.size() + " entries.", module);
            OrderByList orderByList = new OrderByList(orderBy);
            orderByList.checkOrderBy(modelEntity);
            orderByList.makeOrderByString(sql, modelEntity, includeTablenamePrefix, datasourceInfo);
        }
        if (Debug.verboseOn()) Debug.logVerbose("makeOrderByClause: " + sql.toString(), module);
        return sql.toString();
    }

    public static String makeViewTable(Entity modelEntity, ModelFieldTypeReader modelFieldTypeReader, DatasourceInfo datasourceInfo) throws GenericEntityException {
        if (modelEntity instanceof ViewEntity) {
            StringBuilder sql = new StringBuilder("(SELECT ");
            Iterator<Field> fieldsIter = modelEntity.getFieldsIterator();
            if (fieldsIter.hasNext()) {
                Field curField = fieldsIter.next();
                sql.append(curField.getColValue());
                sql.append(" AS ");
                sql.append(curField.getColName());
                while (fieldsIter.hasNext()) {
                    curField = fieldsIter.next();
                    sql.append(", ");
                    sql.append(curField.getColValue());
                    sql.append(" AS ");
                    sql.append(curField.getColName());
                }
            }
            sql.append(makeFromClause(modelEntity, modelFieldTypeReader, datasourceInfo));
            String viewWhereClause = makeViewWhereClause(modelEntity, datasourceInfo.joinStyle);
            if (UtilValidate.isNotEmpty(viewWhereClause)) {
                sql.append(" WHERE ");
                sql.append(viewWhereClause);
            }
            ViewEntity modelViewEntity = (ViewEntity)modelEntity;
            modelViewEntity.colNameString(modelViewEntity.getGroupBysCopy(), sql, " GROUP BY ", ", ", "", false);

            sql.append(")");
            return sql.toString();
        } else {
            return modelEntity.getTableName(datasourceInfo);
        }
    }

    public static String filterColName(String colName) {
        return colName.replace('.', '_').replace('(','_').replace(')','_');
    }

    /* ====================================================================== */

    /* ====================================================================== */

    /**
     *  The elements (ModelFields) of the list are bound to an SQL statement
     *  (SQL-Processor)
     *
     * @param sqlP
     * @param list
     * @param entity
     * @throws GenericEntityException
     */
    public static void setValues(SQLProcessor sqlP, List<Field> list, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        for (Field curField: list) {
            setValue(sqlP, curField, entity, modelFieldTypeReader);
        }
    }

    /**
     *  The elements (ModelFields) of the list are bound to an SQL statement
     *  (SQL-Processor), but values must not be null.
     *
     * @param sqlP
     * @param list
     * @param dummyValue
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setValuesWhereClause(SQLProcessor sqlP, List<Field> list, GenericValue dummyValue, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        for (Field curField: list) {
            // for where clause variables only setValue if not null...
            if (dummyValue.get(curField.getName()) != null) {
                setValue(sqlP, curField, dummyValue, modelFieldTypeReader);
            }
        }
    }

    /**
     *  Get all primary keys from the model entity and bind their values
     *  to the an SQL statement (SQL-Processor)
     *
     * @param sqlP
     * @param modelEntity
     * @param entity
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setPkValues(SQLProcessor sqlP, Entity modelEntity, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        Iterator<Field> pksIter = modelEntity.getPksIterator();
        while (pksIter.hasNext()) {
            Field curField = pksIter.next();

            // for where clause variables only setValue if not null...
            if (entity.dangerousGetNoCheckButFast(curField) != null) {
                setValue(sqlP, curField, entity, modelFieldTypeReader);
            }
        }
    }

    public static void getValue(ResultSet rs, int ind, Field curField, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        FieldType mft = modelFieldTypeReader.getModelFieldType(curField.getType());

        if (mft == null) {
            throw new GenericModelException("definition fieldType " + curField.getType() + " not found, cannot getValue for field " +
                    entity.getEntityName() + "." + curField.getName() + ".");
        }

        // ----- Try out the new handler code -----

        JdbcValueHandler<?> handler = mft.getJdbcValueHandler();
        if (handler != null) {
            try {
                entity.dangerousSetNoCheckButFast(curField, handler.getValue(rs, ind));
                return;
            } catch (Exception e) {
                Debug.logError(e, module);
            }
        } else {
            Debug.logWarning("JdbcValueHandler not found for java-type " + mft.getJavaType() +
                    ", falling back on switch statement. Entity = " +
                    curField.getModelEntity().getEntityName() +
                    ", field = " + curField.getName() + ".", module);
        }

        // ------------------------------------------

        String fieldType = mft.getJavaType();

        try {
            // checking to see if the object is null is really only necessary for the numbers
            int typeValue = getType(fieldType);
            ResultSetMetaData rsmd = rs.getMetaData();
            int colType = rsmd.getColumnType(ind);

            if (typeValue <= 4 || typeValue >= 11) {
                switch (typeValue) {
                case 1:
                    if (java.sql.Types.CLOB == colType) {
                        // Debug.logInfo("For field " + curField.getName() + " of entity " + entity.getEntityName() + " getString is a CLOB, trying getCharacterStream", module);
                        // if the String is empty, try to get a text input stream, this is required for some databases for larger fields, like CLOBs

                        Clob valueClob = rs.getClob(ind);
                        Reader valueReader = null;
                        if (valueClob != null) {
                            valueReader = valueClob.getCharacterStream();
                        }

                        //Reader valueReader = rs.getCharacterStream(ind);
                        if (valueReader != null) {
                            char[] inCharBuffer = new char[CHAR_BUFFER_SIZE];
                            StringBuilder strBuf = new StringBuilder();
                            int charsRead = 0;
                            try {
                                while ((charsRead = valueReader.read(inCharBuffer, 0, CHAR_BUFFER_SIZE)) > 0) {
                                    strBuf.append(inCharBuffer, 0, charsRead);
                                }
                                valueReader.close();
                            } catch (IOException e) {
                                throw new GenericEntityException("Error reading long character stream for field " + curField.getName() + " of entity " + entity.getEntityName(), e);
                            }
                            entity.dangerousSetNoCheckButFast(curField, strBuf.toString());
                        } else {
                            entity.dangerousSetNoCheckButFast(curField, null);
                        }
                    } else {
                        String value = rs.getString(ind);
                        entity.dangerousSetNoCheckButFast(curField, value);
                    }
                    break;

                case 2:
                    entity.dangerousSetNoCheckButFast(curField, rs.getTimestamp(ind));
                    break;

                case 3:
                    entity.dangerousSetNoCheckButFast(curField, rs.getTime(ind));
                    break;

                case 4:
                    entity.dangerousSetNoCheckButFast(curField, rs.getDate(ind));
                    break;

                case 11:
                    Object obj = null;

                    byte[] originalBytes = rs.getBytes(ind);
                    obj = deserializeField(originalBytes, ind, curField);

                    if (obj != null) {
                        entity.dangerousSetNoCheckButFast(curField, obj);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, originalBytes);
                    }
                    break;
                case 12:
                    Object originalObject;
                    byte[] fieldBytes;
                    try {
                        Blob theBlob = rs.getBlob(ind);
                        fieldBytes = theBlob != null ? theBlob.getBytes(1, (int) theBlob.length()) : null;
                        originalObject = theBlob;
                    } catch (SQLException e) {
                        // for backward compatibility if getBlob didn't work try getBytes
                        fieldBytes = rs.getBytes(ind);
                        originalObject = fieldBytes;
                    }

                    if (originalObject != null) {
                        // for backward compatibility, check to see if there is a serialized object and if so return that
                        Object blobObject = deserializeField(fieldBytes, ind, curField);
                        if (blobObject != null) {
                            entity.dangerousSetNoCheckButFast(curField, blobObject);
                        } else {
                            if (originalObject instanceof Blob) {
                                // NOTE using SerialBlob here instead of the Blob from the database to make sure we can pass it around, serialize it, etc
                                entity.dangerousSetNoCheckButFast(curField, new SerialBlob((Blob) originalObject));
                            } else {
                                entity.dangerousSetNoCheckButFast(curField, originalObject);
                            }
                        }
                    }

                    break;
                case 13:
                    entity.dangerousSetNoCheckButFast(curField, new SerialClob(rs.getClob(ind)));
                    break;
                case 14:
                case 15:
                    entity.dangerousSetNoCheckButFast(curField, rs.getObject(ind));
                    break;
                }
            } else {
                switch (typeValue) {
                case 5:
                    int intValue = rs.getInt(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, Integer.valueOf(intValue));
                    }
                    break;

                case 6:
                    long longValue = rs.getLong(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, Long.valueOf(longValue));
                    }
                    break;

                case 7:
                    float floatValue = rs.getFloat(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, Float.valueOf(floatValue));
                    }
                    break;

                case 8:
                    double doubleValue = rs.getDouble(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, Double.valueOf(doubleValue));
                    }
                    break;

                case 9:
                    BigDecimal bigDecimalValue = rs.getBigDecimal(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, bigDecimalValue);
                    }
                    break;

                case 10:
                    boolean booleanValue = rs.getBoolean(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, Boolean.valueOf(booleanValue));
                    }
                    break;
                }
            }
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while getting value : " + curField.getName() + " [" + curField.getColName() + "] (" + ind + ")", sqle);
        }
    }

    private static Object deserializeField(byte[] fieldBytes, int ind, ModelField curField) throws GenericDataSourceException {
        // NOTE DEJ20071022: the following code is to convert the byte[] back into an object; if that fails
        //just return the byte[]; this was for the ByteWrapper thing which is now deprecated, so this may
        //be removed in the near future to enhance performance
        InputStream binaryInput = null;
        if (fieldBytes != null && fieldBytes.length > 0) {
            binaryInput = new ByteArrayInputStream(fieldBytes);
        }

        if (fieldBytes != null && fieldBytes.length <= 0) {
            Debug.logWarning("Got bytes back for Object field with length: " + fieldBytes.length + " while getting value : " + curField.getName() + " [" + curField.getColName() + "] (" + ind + "): ", module);
        }

        //alt 1: binaryInput = rs.getBinaryStream(ind);
        //alt 2: Blob blobLocator = rs.getBlob(ind);
        //if (blobLocator != null) {
        //    binaryInput = blobLocator.getBinaryStream();
        //}

        if (binaryInput != null) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(binaryInput);
                return in.readObject();
            } catch (IOException ex) {
                if (Debug.verboseOn()) Debug.logVerbose("Unable to read BLOB data from input stream while getting value : " + curField.getName() + " [" + curField.getColName() + "] (" + ind + "): " + ex.toString(), module);
                return null;
            } catch (ClassNotFoundException ex) {
                if (Debug.verboseOn()) Debug.logVerbose("Class not found: Unable to cast BLOB data to an Java object while getting value: " + curField.getName() + " [" + curField.getColName() + "] (" + ind + "); most likely because it is a straight byte[], so just using the raw bytes" + ex.toString(), module);
                return null;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new GenericDataSourceException("Unable to close binary input stream while getting value : " + curField.getName() + " [" + curField.getColName() + "] (" + ind + "): " + e.toString(), e);
                    }
                }
            }
        }

        return null;
    }

    public static void setValue(SQLProcessor sqlP, Field modelField, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        Object fieldValue = entity.dangerousGetNoCheckButFast(modelField);

        setValue(sqlP, modelField, entity.getEntityName(), fieldValue, modelFieldTypeReader);
    }

    public static <T> void setValue(SQLProcessor sqlP, Field modelField, String entityName, Object fieldValue, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        FieldType mft = modelFieldTypeReader.getModelFieldType(modelField.getType());

        if (mft == null) {
            throw new GenericModelException("GenericDAO.getValue: definition fieldType " + modelField.getType() + " not found, cannot setValue for field " +
                    entityName + "." + modelField.getName() + ".");
        }

        // if the value is the GenericEntity.NullField, treat as null
        if (fieldValue == GenericEntity.NULL_FIELD) {
            fieldValue = null;
        }

        // ----- Try out the new handler code -----

        JdbcValueHandler<T> handler = UtilGenerics.cast(mft.getJdbcValueHandler());
        if (handler != null) {
            try {
                sqlP.setValue(handler, handler.getJavaClass().cast(fieldValue));
                return;
            } catch (SQLException e) {
                throw new GenericDataSourceException("SQL Exception while setting value on field [" + modelField.getName() + "] of entity " + entityName + ": ", e);
            }
        } else {
            Debug.logWarning("JdbcValueHandler not found for java-type " + mft.getJavaType() +
                    ", falling back on switch statement. Entity = " +
                    modelField.getModelEntity().getEntityName() +
                    ", field = " + modelField.getName() + ".", module);
        }

        // ------------------------------------------

        String fieldType = mft.getJavaType();
        if (fieldValue != null) {
            if (!ObjectType.instanceOf(fieldValue, fieldType)) {
                // this is only an info level message because under normal operation for most JDBC
                // drivers this will be okay, but if not then the JDBC driver will throw an exception
                // and when lower debug levels are on this should help give more info on what happened
                String fieldClassName = fieldValue.getClass().getName();
                if (fieldValue instanceof byte[]) {
                    fieldClassName = "byte[]";
                }

                if (Debug.verboseOn()) Debug.logVerbose("type of field " + entityName + "." + modelField.getName() +
                        " is " + fieldClassName + ", was expecting " + mft.getJavaType() + "; this may " +
                        "indicate an error in the configuration or in the class, and may result " +
                        "in an SQL-Java data conversion error. Will use the real field type: " +
                        fieldClassName + ", not the definition.", module);
                fieldType = fieldClassName;
            }
        }

        try {
            int typeValue = getType(fieldType);

            switch (typeValue) {
            case 1:
                sqlP.setValue((String) fieldValue);
                break;

            case 2:
                sqlP.setValue((java.sql.Timestamp) fieldValue);
                break;

            case 3:
                sqlP.setValue((java.sql.Time) fieldValue);
                break;

            case 4:
                sqlP.setValue((java.sql.Date) fieldValue);
                break;

            case 5:
                sqlP.setValue((java.lang.Integer) fieldValue);
                break;

            case 6:
                sqlP.setValue((java.lang.Long) fieldValue);
                break;

            case 7:
                sqlP.setValue((java.lang.Float) fieldValue);
                break;

            case 8:
                sqlP.setValue((java.lang.Double) fieldValue);
                break;

            case 9:
                sqlP.setValue((java.math.BigDecimal) fieldValue);
                break;

            case 10:
                sqlP.setValue((java.lang.Boolean) fieldValue);
                break;

            case 11:
                sqlP.setBinaryStream(fieldValue);
                break;

            case 12:
                if (fieldValue instanceof byte[]) {
                    sqlP.setBytes((byte[]) fieldValue);
                } else if (fieldValue instanceof ByteBuffer) {
                    sqlP.setBytes(((ByteBuffer) fieldValue).array());
                } else {
                    sqlP.setValue((java.sql.Blob) fieldValue);
                }
                break;

            case 13:
                sqlP.setValue((java.sql.Clob) fieldValue);
                break;

            case 14:
                if (fieldValue != null) {
                    sqlP.setValue(new java.sql.Date(((java.util.Date) fieldValue).getTime()));
                } else {
                    sqlP.setValue((java.sql.Date) null);
                }
                break;

            case 15:
                sqlP.setValue(UtilGenerics.<Collection<?>>cast(fieldValue));
                break;
            }
        } catch (GenericNotImplementedException e) {
            throw new GenericNotImplementedException("Not Implemented Exception while setting value on field [" + modelField.getName() + "] of entity " + entityName + ": " + e.toString(), e);
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while setting value on field [" + modelField.getName() + "] of entity " + entityName + ": ", sqle);
        }
    }

    protected static Map<String, Integer> fieldTypeMap = FastMap.newInstance();
    static {
        fieldTypeMap.put("java.lang.String", 1);
        fieldTypeMap.put("String", 1);
        fieldTypeMap.put("java.sql.Timestamp", 2);
        fieldTypeMap.put("Timestamp", 2);
        fieldTypeMap.put("java.sql.Time", 3);
        fieldTypeMap.put("Time", 3);
        fieldTypeMap.put("java.sql.Date", 4);
        fieldTypeMap.put("Date", 4);
        fieldTypeMap.put("java.lang.Integer", 5);
        fieldTypeMap.put("Integer", 5);
        fieldTypeMap.put("java.lang.Long", 6);
        fieldTypeMap.put("Long", 6);
        fieldTypeMap.put("java.lang.Float", 7);
        fieldTypeMap.put("Float", 7);
        fieldTypeMap.put("java.lang.Double", 8);
        fieldTypeMap.put("Double", 8);
        fieldTypeMap.put("java.math.BigDecimal", 9);
        fieldTypeMap.put("BigDecimal", 9);
        fieldTypeMap.put("java.lang.Boolean", 10);
        fieldTypeMap.put("Boolean", 10);

        fieldTypeMap.put("java.lang.Object", 11);
        fieldTypeMap.put("Object", 11);

        fieldTypeMap.put("java.sql.Blob", 12);
        fieldTypeMap.put("Blob", 12);
        fieldTypeMap.put("byte[]", 12);
        fieldTypeMap.put("java.nio.ByteBuffer", 12);
        fieldTypeMap.put("java.nio.HeapByteBuffer", 12);

        fieldTypeMap.put("java.sql.Clob", 13);
        fieldTypeMap.put("Clob", 13);

        fieldTypeMap.put("java.util.Date", 14);

        // all of these treated as Collection
        fieldTypeMap.put("java.util.ArrayList", 15);
        fieldTypeMap.put("java.util.HashSet", 15);
        fieldTypeMap.put("java.util.LinkedHashSet", 15);
        fieldTypeMap.put("java.util.LinkedList", 15);
    }

    public static int getType(String fieldType) throws GenericNotImplementedException {
        Integer val = fieldTypeMap.get(fieldType);

        if (val == null) {
            throw new GenericNotImplementedException("Java type " + fieldType + " not currently supported. Sorry.");
        }
        return val.intValue();
    }

    public static void addValueSingle(StringBuffer buffer, Field field, Object value, List<EntityConditionParam> params) {
        StringBuilder sb = new StringBuilder();
        addValueSingle(sb, field, value, params);
        buffer.append(sb);
    }

    public static void addValueSingle(StringBuilder buffer, Field field, Object value, List<EntityConditionParam> params) {
        if (field != null) {
            buffer.append('?');
        } else {
            buffer.append('\'');
            if (value instanceof String) {
                buffer.append(((String) value).replaceAll("'", "''"));
            } else {
                buffer.append(value);
            }
            buffer.append('\'');
        }
        if (field != null && params != null) params.add(new EntityConditionParam(field, value));
    }

    public static void addValue(StringBuffer buffer, Field field, Object value, List<EntityConditionParam> params) {
        StringBuilder sb = new StringBuilder();
        addValue(sb, field, value, params);
        buffer.append(sb);
    }

    public static void addValue(StringBuilder buffer, Field field, Object value, List<EntityConditionParam> params) {
        if (value instanceof Collection<?>) {
            buffer.append("(");
            Iterator<Object> it = UtilGenerics.checkCollection(value).iterator();
            while (it.hasNext()) {
                Object thisValue = it.next();
                addValueSingle(buffer, field, thisValue, params);
                if (it.hasNext()) buffer.append(", ");
            }
            buffer.append(")");
        } else {
            addValueSingle(buffer, field, value, params);
        }
    }
}
