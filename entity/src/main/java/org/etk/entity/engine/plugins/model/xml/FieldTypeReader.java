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
package org.etk.entity.engine.plugins.model.xml;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.etk.common.logging.Logger;
import org.etk.entity.base.utils.UtilValidate;
import org.etk.entity.engine.plugins.config.DatasourceInfo;
import org.etk.entity.engine.plugins.config.EntityConfigUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          thanhvucong.78@google.com
 * Sep 11, 2011  
 */
public class FieldTypeReader implements Serializable {

  private static final Logger logger =Logger.getLogger(FieldTypeReader.class);
  protected static final UtilCache<String, FieldTypeReader> readers = UtilCache.createUtilCache("entity.ModelFieldTypeReader", 0, 0);

  protected static Map<String, FieldType> createFieldTypeCache(Element docElement, String location) {
      docElement.normalize();
      Map<String, FieldType> fieldTypeMap = FastMap.newInstance();
      List<? extends Element> fieldTypeList = UtilXml.childElementList(docElement, "field-type-def");
      for (Element curFieldType: fieldTypeList) {
          String fieldTypeName = curFieldType.getAttribute("type");
          if (UtilValidate.isEmpty(fieldTypeName)) {
              logger.error("Invalid field-type element, type attribute is missing in file " + location);
          } else {
              FieldType fieldType = new FieldType(curFieldType);
              fieldTypeMap.put(fieldTypeName.intern(), fieldType);
          }
      }
      return fieldTypeMap;
  }

  public static FieldTypeReader getModelFieldTypeReader(String helperName) {
      DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);
      if (datasourceInfo == null) {
          throw new IllegalArgumentException("Could not find a datasource/helper with the name " + helperName);
      }
      String tempModelName = datasourceInfo.fieldTypeName;
      //ThanhVC loading the fieldtype from entity/fieldtype
      FieldTypeReader reader = readers.get(tempModelName);
      if (reader == null) {
          synchronized (readers) {
              FieldTypeInfo fieldTypeInfo = EntityConfigUtil.getFieldTypeInfo(tempModelName);
              if (fieldTypeInfo == null) {
                  throw new IllegalArgumentException("Could not find a field-type definition with name \"" + tempModelName + "\"");
              }
              ResourceHandler fieldTypeResourceHandler = new MainResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, fieldTypeInfo.resourceElement);
              UtilTimer utilTimer = new UtilTimer();
              utilTimer.timerString("[ModelFieldTypeReader.getModelFieldTypeReader] Reading field types from " + fieldTypeResourceHandler.getLocation());
              Document document = null;
              try {
                  document = fieldTypeResourceHandler.getDocument();
              } catch (GenericConfigException e) {
                  Debug.logError(e, module);
                  throw new IllegalStateException("Error loading field type file " + fieldTypeResourceHandler.getLocation());
              }
              Map<String, ModelFieldType> fieldTypeMap = createFieldTypeCache(document.getDocumentElement(), fieldTypeResourceHandler.getLocation());
              reader = new ModelFieldTypeReader(fieldTypeMap);
              readers.put(tempModelName, reader);
              utilTimer.timerString("[ModelFieldTypeReader.getModelFieldTypeReader] Read " + fieldTypeMap.size() + " field types");
          }
      }
      return reader;
  }

  protected final Map<String, FieldType> fieldTypeCache;

  public FieldTypeReader(Map<String, FieldType> fieldTypeMap) {
      this.fieldTypeCache = fieldTypeMap;
  }

  /** Creates a Collection with all of the ModelFieldType names
   * @return A Collection of ModelFieldType names
   */
  public Collection<String> getFieldTypeNames() {
      return this.fieldTypeCache.keySet();
  }

  /** Creates a Collection with all of the ModelFieldTypes
   * @return A Collection of ModelFieldTypes
   */
  public Collection<FieldType> getFieldTypes() {
      return this.fieldTypeCache.values();
  }

  /** Gets an FieldType object based on a definition from the specified XML FieldType descriptor file.
   * @param fieldTypeName The fieldTypeName of the FieldType definition to use.
   * @return An FieldType object describing the specified fieldType of the specified descriptor file.
   */
  public FieldType getModelFieldType(String fieldTypeName) {
      return this.fieldTypeCache.get(fieldTypeName);
  }

}