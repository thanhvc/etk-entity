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
package org.etk.entity.engine;

import java.net.URL;
import java.util.List;

import org.etk.entity.engine.plugins.model.configuration.ConfigurationUnmarshaller;
import org.etk.entity.engine.plugins.model.xml.Configuration;
import org.etk.entity.engine.plugins.model.xml.Entity;
import org.etk.entity.engine.plugins.model.xml.PKField;

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 5, 2011  
 */
public class EntityConfigurationManagerTest extends TestCase {

  private static String ENTITY_MODEL_XML = "entityconf/entitymodel.xml";
  
  private static String SAMPLE_ENTITY_NAME = "org.etk.entity.engine.sample.SampleEntity";
  private static String SAMPLE1_ENTITY_NAME = "org.etk.entity.engine.sample.SampleEntity1";
  private ConfigurationUnmarshaller unmarshaller;
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    unmarshaller = new ConfigurationUnmarshaller();
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    
  }
  
  public void testLoadEntityModelOneEntity() throws Exception {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    URL entityURL = cl.getResource(ENTITY_MODEL_XML);
    Configuration conf = unmarshaller.unmarshall(entityURL);
    
    assertNotNull("Configuration must not be null.", conf);
    assertEquals("Entity's size must be equals." + 2 , 2, conf.getEntities().size());
    
    Entity sample = conf.getEntity(SAMPLE_ENTITY_NAME);
    assertNotNull("Sample entity must not be null.", sample);
    assertEquals("SampleEntity", sample.getEntityName());
    assertEquals("org.etk.entity.engine.sample", sample.getPackageName());
    assertEquals(5, sample.getFields().size());
    assertEquals(2, sample.getPkgs().size());
    
    Entity sample1 = conf.getEntity(SAMPLE1_ENTITY_NAME);
    assertNotNull("Sample1 entity must not be null.", sample1);
    assertEquals("SampleEntity1", sample1.getEntityName());
    assertEquals("org.etk.entity.engine.sample", sample1.getPackageName());
    assertEquals(4, sample1.getFields().size());
    assertEquals(2, sample1.getPkgs().size());
    
  }
  
  
}
