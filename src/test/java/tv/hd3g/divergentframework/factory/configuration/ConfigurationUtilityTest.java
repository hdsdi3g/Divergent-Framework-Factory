/*
 * This file is part of Divergent Framework Factory.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 * 
*/
package tv.hd3g.divergentframework.factory.configuration;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.Factory;
import tv.hd3g.divergentframework.factory.GsonKit;
import tv.hd3g.divergentframework.factory.configuration.demo.SingleCar;

public class ConfigurationUtilityTest extends TestCase {
	
	public void test() throws Exception {
		Factory factory = new Factory();
		ConfigurationUtility conf_u = new ConfigurationUtility(factory);
		
		/**
		 * Create conf file, save it, load it.
		 */
		JsonObject conf_root = new JsonObject();
		JsonObject conf_tree = new JsonObject();
		conf_tree.addProperty("color", "blue");
		conf_tree.addProperty("size", 1);
		conf_root.add(SingleCar.class.getName(), conf_tree);
		
		File temp_conf_file = File.createTempFile(ConfigurationUtilityTest.class.getSimpleName().toLowerCase(), ".json");
		GsonKit gson = factory.createGsonKit();
		FileUtils.write(temp_conf_file, gson.getGsonPretty().toJson(conf_root), "UTF-8");
		
		conf_u.addConfigurationFilesToInternalList(temp_conf_file);
		
		conf_u.scanAndImportFiles();
		
		assertTrue(conf_u.isClassIsConfigured(SingleCar.class));
		
		SingleCar car = new SingleCar();
		
		assertNull(car.getColor());
		assertEquals(0f, car.getSize());
		
		conf_u.addNewClassInstanceToConfigure(car, SingleCar.class);
		
		assertEquals("blue", car.getColor());
		assertEquals(1.0f, car.getSize());
		
		/**
		 * Update conf, save it, load it, update instance.
		 */
		conf_tree.addProperty("size", 2);
		
		/**
		 * Wait a long time for the new file date will be different.
		 */
		Thread.sleep(1000);
		
		FileUtils.write(temp_conf_file, gson.getGsonPretty().toJson(conf_root), "UTF-8");
		
		conf_u.scanAndImportFiles();
		
		assertEquals("blue", car.getColor());
		assertEquals(2.0f, car.getSize());
		
		temp_conf_file.delete();
	}
	
	/* TODO test ConfigurationUtility: conf_u.loadMnemonicClassNameListFromFile(conf_file);
	*/
	
}
