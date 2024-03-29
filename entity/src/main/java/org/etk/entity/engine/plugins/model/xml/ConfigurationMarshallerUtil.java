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

import java.io.IOException;
import java.net.URL;

import org.jibx.runtime.IMarshallingContext;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Aug 30, 2011  
 */
public class ConfigurationMarshallerUtil {

  /**
   * This method adds the given {@link URL} as comment to XML content.
   */
  static void addURLToContent(URL source, IMarshallingContext ictx) {
    try {
      ictx.getXmlWriter().writeComment(" Loaded from '" + source + "' ");
    } catch (IOException e) {
      // log.warn("Could not add the source into the XML document", e);
    }
  }
}
