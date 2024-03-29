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
package org.etk.entity.engine.core;

import org.etk.entity.base.utils.GeneralException;
/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 6, 2011  
 */
@SuppressWarnings("serial")
public class GenericEntityException extends GeneralException {

    public GenericEntityException() {
        super();
    }

    public GenericEntityException(Throwable nested) {
        super(nested);
    }

    public GenericEntityException(String str) {
        super(str);
    }

    public GenericEntityException(String str, Throwable nested) {
        super(str, nested);
    }
}
