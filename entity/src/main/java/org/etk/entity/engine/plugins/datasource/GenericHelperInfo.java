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
package org.etk.entity.engine.plugins.datasource;

import org.etk.entity.base.utils.UtilValidate;

/**
 * Generic Entity Helper Info Class
 *
 */
public class GenericHelperInfo {
    protected String entityGroupName;
    protected String helperBaseName;
    protected String tenantId = null;
    protected String overrideJdbcUri = null;
    protected String overrideUsername = null;
    protected String overridePassword = null;

    public GenericHelperInfo(String entityGroupName, String helperBaseName) {
        this.entityGroupName = entityGroupName;
        this.helperBaseName = helperBaseName;
    }

    public String getHelperFullName() {
        if (UtilValidate.isNotEmpty(tenantId)) {
            return helperBaseName + "#" + tenantId;
        } else {
            return helperBaseName;
        }
    }

    public String getEntityGroupName() {
        return entityGroupName;
    }

    public String getHelperBaseName() {
        return helperBaseName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOverrideJdbcUri() {
        return overrideJdbcUri;
    }

    public void setOverrideJdbcUri(String overrideJdbcUri) {
        this.overrideJdbcUri = overrideJdbcUri;
    }

    public String getOverrideUsername() {
        return overrideUsername;
    }

    public void setOverrideUsername(String overrideUsername) {
        this.overrideUsername = overrideUsername;
    }

    public String getOverridePassword() {
        return overridePassword;
    }

    public void setOverridePassword(String overridePassword) {
        this.overridePassword = overridePassword;
    }
}