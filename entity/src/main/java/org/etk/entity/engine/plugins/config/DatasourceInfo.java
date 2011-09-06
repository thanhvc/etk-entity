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
package org.etk.entity.engine.plugins.config;

import java.util.List;

import org.etk.common.logging.Logger;

import com.ibm.icu.util.RangeValueIterator.Element;

/**
 * Misc. utility method for dealing with the entityengine.xml file
 *
 */
public class DatasourceInfo {
    public static final String module = DatasourceInfo.class.getName();

    private static final Logger logger = Logger.getLogger(DatasourceInfo.class);
    public String name;
    public String helperClass;
    public String fieldTypeName;
    public List<? extends Element> sqlLoadPaths;
    public List<? extends Element> readDatas;
    public Element datasourceElement;

    public static final int TYPE_JNDI_JDBC = 1;
    public static final int TYPE_INLINE_JDBC = 2;
    public static final int TYPE_TYREX_DATA_SOURCE = 3;
    public static final int TYPE_OTHER = 4;

    public Element jndiJdbcElement;
    public Element tyrexDataSourceElement;
    public Element inlineJdbcElement;

    public String schemaName = null;
    public boolean useSchemas = true;
    public boolean checkOnStart = true;
    public boolean addMissingOnStart = false;
    public boolean useFks = true;
    public boolean useFkIndices = true;
    public boolean checkPrimaryKeysOnStart = false;
    public boolean checkForeignKeysOnStart = false;
    public boolean checkFkIndicesOnStart = false;
    public boolean usePkConstraintNames = true;
    public int constraintNameClipLength = 30;
    public boolean useProxyCursor = false;
    public String cursorName = "p_cursor";
    public int resultFetchSize = -1;
    public String fkStyle = null;
    public boolean useFkInitiallyDeferred = true;
    public boolean useIndices = true;
    public boolean useIndicesUnique = true;
    public boolean checkIndicesOnStart = false;
    public String joinStyle = null;
    public boolean aliasViews = true;
    public boolean alwaysUseConstraintKeyword = false;
    public boolean dropFkUseForeignKeyKeyword = false;
    public boolean useBinaryTypeForBlob = false;
    public boolean useOrderByNulls = false;
    public String tableType = null;
    public String characterSet = null;
    public String collate = null;
    public int maxWorkerPoolSize = 1;

    public DatasourceInfo(Element element) {
      logger.warn("datasource def not found with name " + this.name + ", using default for schema-name (none)");
      logger.warn("datasource def not found with name " + this.name + ", using default for use-schemas (true)");
      logger.warn("datasource def not found with name " + this.name + ", using default for check-on-start (true)");
      logger.warn("datasource def not found with name " + this.name + ", using default for add-missing-on-start (false)");
      logger.warn("datasource def not found with name " + this.name + ", using default for check-pks-on-start (true)");
      logger.warn("datasource def not found with name " + this.name + ", using default for use-foreign-keys (true)");
      logger.warn("datasource def not found with name " + this.name + ", using default use-foreign-key-indices (true)");
      logger.warn("datasource def not found with name " + this.name + ", using default for check-fks-on-start (false)");
      logger.warn("datasource def not found with name " + this.name + ", using default for check-fk-indices-on-start (false)");
      logger.warn("datasource def not found with name " + this.name + ", using default for use-pk-constraint-names (true)");
      logger.warn("datasource def not found with name " + this.name + ", using default for constraint-name-clip-length (30)");
      logger.warn("datasource def not found with name " + this.name + ", using default for fk-style (name_constraint)");
      logger.warn("datasource def not found with name " + this.name + ", using default for use-fk-initially-deferred (true)");
      logger.warn("datasource def not found with name " + this.name + ", using default for use-indices (true)");
      logger.warn("datasource def not found with name " + this.name + ", using default for use-indices-unique (true)");
      logger.warn("datasource def not found with name " + this.name + ", using default for check-indices-on-start (false)");
      logger.warn("datasource def not found with name " + this.name + ", using default for join-style (ansi)");
      logger.warn("datasource def not found with name " + this.name + ", using default for always-use-constraint-keyword (false)");
      logger.warn("datasource def not found with name " + this.name + ", using default for drop-fk-use-foreign-key-keyword (false)");
      logger.warn("datasource def not found with name " + this.name + ", using default for use-binary-type-for-blob (false)");
      logger.warn("datasource def not found with name " + this.name + ", using default for table-type (none)");
      logger.warn("datasource def not found with name " + this.name + ", using default for character-set (none)");
      logger.warn("datasource def not found with name " + this.name + ", using default for collate (none)");
      logger.warn("datasource def not found with name " + this.name + ", using default for max-worker-pool-size (1)");
    }
}