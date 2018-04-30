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

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.Factory;
import tv.hd3g.divergentframework.factory.GsonKit;
import tv.hd3g.divergentframework.factory.configuration.demo.SingleCar;
import tv.hd3g.divergentframework.factory.configuration.demo.SingleCar.WheelType;

public class ConfiguredClassTest extends TestCase {
	
	private static final GsonKit gson_kit = new Factory().createGsonKit();
	
	public void test() {
		ClassConfigurator cc = new ClassConfigurator(gson_kit, c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		JsonObject conf_tree = new JsonObject();
		conf_tree.addProperty("color", "blue");
		conf_tree.addProperty("size", 1);
		
		ConfiguredClass<SingleCar> c_class = new ConfiguredClass<>(cc, gson_kit.getGson(), SingleCar.class, conf_tree);
		
		SingleCar car1 = new SingleCar();
		SingleCar car2 = new SingleCar();
		
		c_class.setupInstance(car1);
		c_class.setupInstance(car2);
		
		assertEquals("blue", car1.getColor());
		assertEquals(1.0f, car1.getSize());
		
		conf_tree.addProperty("size", 2);
		c_class.updateInstances(conf_tree);
		assertEquals(2.0f, car1.getSize());
		
		conf_tree.addProperty("size", 3);
		c_class.updateInstances(conf_tree);
		assertEquals(3.0f, car2.getSize());
		
		JsonObject jo_wheel = new JsonObject();
		jo_wheel.addProperty("size", 5);
		jo_wheel.addProperty("type", WheelType.formula1.name());
		conf_tree.add("default_wheel", jo_wheel);
		
		c_class.updateInstances(conf_tree);
		assertNotNull(car2.getDefault_wheel());
		assertEquals(5, car2.getDefault_wheel().size);
		
		conf_tree.add("default_wheel", JsonNull.INSTANCE);
		c_class.updateInstances(conf_tree);
		assertNull(car2.getDefault_wheel());
		
		conf_tree.add("default_wheel", jo_wheel);
		c_class.updateInstances(conf_tree);
		assertEquals(5, car1.getDefault_wheel().size);
		
		assertEquals(0, car2.counter_BeforeRemovedInConfiguration.get());
		assertEquals(0, car2.getDefault_wheel().counter_BeforeRemovedInConfiguration.get());
		c_class.afterRemovedConf();
		assertEquals(1, car2.counter_BeforeRemovedInConfiguration.get());
		assertEquals(0, car2.getDefault_wheel().counter_BeforeRemovedInConfiguration.get());
	}
	
}
