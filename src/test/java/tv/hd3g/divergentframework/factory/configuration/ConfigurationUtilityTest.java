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
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.Factory;
import tv.hd3g.divergentframework.factory.GsonKit;
import tv.hd3g.divergentframework.factory.configuration.demo.SingleCar;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub;

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
		
		conf_u.scanImportedFilesAndUpdateConfigurations();
		
		assertTrue(conf_u.isClassIsConfigured(SingleCar.class));
		assertFalse(conf_u.isClassIsConfigured(TMainSub.class));
		
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
		
		conf_u.scanImportedFilesAndUpdateConfigurations();
		
		assertEquals("blue", car.getColor());
		assertEquals(2.0f, car.getSize());
		
		temp_conf_file.delete();
	}
	
	public void testMnemonic() throws Exception {
		Factory factory = new Factory();
		ConfigurationUtility conf_u = new ConfigurationUtility(factory);
		
		/**
		 * Create conf file, save it
		 */
		JsonObject conf_root = new JsonObject();
		JsonObject conf_tree = new JsonObject();
		conf_tree.addProperty("color", "red");
		conf_tree.addProperty("size", 55);
		conf_root.add(SingleCar.class.getSimpleName().toLowerCase(), conf_tree);
		
		File temp_conf_file = File.createTempFile(ConfigurationUtilityTest.class.getSimpleName().toLowerCase(), ".json");
		GsonKit gson = factory.createGsonKit();
		FileUtils.write(temp_conf_file, gson.getGsonPretty().toJson(conf_root), "UTF-8");
		
		/**
		 * Create mnemonic file, save it
		 */
		Properties mnemonic_class_name_list = new Properties();
		mnemonic_class_name_list.setProperty(SingleCar.class.getSimpleName().toLowerCase(), SingleCar.class.getName());
		
		File temp_mnemonic_file = File.createTempFile(ConfigurationUtilityTest.class.getSimpleName().toLowerCase(), ".properties");
		FileOutputStream out = new FileOutputStream(temp_mnemonic_file);
		mnemonic_class_name_list.store(out, "");
		out.close();
		
		conf_u.loadMnemonicClassNameListFromFile(temp_mnemonic_file);
		
		conf_u.addConfigurationFilesToInternalList(temp_conf_file);
		
		conf_u.scanImportedFilesAndUpdateConfigurations();
		
		assertTrue(conf_u.isClassIsConfigured(SingleCar.class));
		assertFalse(conf_u.isClassIsConfigured(TMainSub.class));
		
		SingleCar car = new SingleCar();
		
		assertNull(car.getColor());
		assertEquals(0f, car.getSize());
		
		conf_u.addNewClassInstanceToConfigure(car, SingleCar.class);
		
		assertEquals("red", car.getColor());
		assertEquals(55.0f, car.getSize());
		
	}
	
}
