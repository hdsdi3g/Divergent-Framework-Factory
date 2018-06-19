/*
 * This file is part of factory.
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
package tv.hd3g.divergentframework.factory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.configuration.demo.SingleCar;

public class FactoryTest extends TestCase {
	
	public void testSimpleNew() {
		SingleCar sc = new SingleCar();
		
		assertNull(sc.getColor());
	}
	
	public void testSimpleFactory() throws ReflectiveOperationException, IOException, ScriptException {
		Factory f = new Factory();
		SingleCar sc = f.create(SingleCar.class);
		
		assertNull(sc.getColor());
		assertNull(sc.getPossible_wheel_type());
		assertNull(sc.getPassager_names());
		assertNull(sc.getPoints_by_names());
	}
	
	public void testInterfaceImpl() throws ReflectiveOperationException, IOException, ScriptException {
		Factory f = new Factory();
		f.getJsToolkit().setVerboseErrors(System.err);
		
		f.getBindMap().load(FactoryTest.class.getResource("bind_map.properties").openStream());
		
		SimpleInterface result = f.create(SimpleInterface.class);
		assertNotNull(result);
		assertEquals("AZERTY", result.toUpperCase("aZeRtY"));
		assertEquals("java", result.whoami());
		
		f.getBindMap().put(SimpleInterface.class.getName(), FactoryTest.class.getResource("SimpleInterfaceImpl.js").toString());
		result = f.create(SimpleInterface.class);
		assertNotNull(result);
		assertEquals("AZERTY", result.toUpperCase("aZeRtY"));
		assertEquals("javascript", result.whoami());
		
		System.out.println(FactoryTest.class.getResource("SimpleInterfaceImpl.js"));
		
	}
	
	// T O D O test callbacks (first, before next, after next)
	
	public void testSingleInstance() throws Exception {
		Factory f = new Factory();
		
		Single single = f.create(Single.class);
		assertNotNull(single);
		assertTrue(single.done);
		
		Single single_twice = f.create(Single.class);
		assertNotNull(single_twice);
		assertTrue(single_twice.done);
		assertEquals(single, single_twice);
		assertEquals(single.counter, single_twice.counter);
		
		f.removeSingleInstance(Single.class);
		
		Single single_3rd = f.create(Single.class);
		assertNotNull(single_3rd);
		assertTrue(single_3rd.done);
		assertNotSame(single, single_3rd);
		assertNotSame(single.counter, single_3rd.counter);
	}
	
	public void testInitEmptyFactory() throws Exception {
		new Factory();
		
		File directory_configuration = Files.createTempDirectory(FactoryTest.class.getSimpleName() + "_configuration").toFile();
		File jsbindmap_file = File.createTempFile(FactoryTest.class.getSimpleName(), ".properties");
		File class_mnemonics_file = File.createTempFile(FactoryTest.class.getSimpleName(), ".properties");
		
		new Factory(directory_configuration, jsbindmap_file, class_mnemonics_file);
	}
	
	public void testFactoryWithConf() throws ReflectiveOperationException, IOException, ScriptException {
		Factory f = new Factory();
		
		/**
		 * Create conf file, save it, load it.
		 */
		JsonObject conf_root = new JsonObject();
		JsonObject conf_tree = new JsonObject();
		conf_tree.addProperty("color", "blue");
		conf_tree.addProperty("size", 1);
		conf_root.add(SingleCar.class.getName(), conf_tree);
		
		File temp_conf_file = File.createTempFile(FactoryTest.class.getSimpleName().toLowerCase(), ".json");
		GsonKit gson = f.createGsonKit();
		FileUtils.write(temp_conf_file, gson.getGsonPretty().toJson(conf_root), "UTF-8");
		
		f.getConfigurator().addConfigurationFilesToInternalList(temp_conf_file).scanImportedFiles();
		
		/**
		 * Create object with injected conf
		 */
		SingleCar sc = f.create(SingleCar.class);
		
		assertEquals("blue", sc.getColor());
		assertEquals(1f, sc.getSize());
	}
	
}
