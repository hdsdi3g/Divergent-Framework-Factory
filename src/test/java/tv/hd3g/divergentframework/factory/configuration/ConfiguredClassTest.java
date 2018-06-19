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

import com.google.gson.JsonObject;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.Factory;
import tv.hd3g.divergentframework.factory.GsonKit;
import tv.hd3g.divergentframework.factory.configuration.demo.SingleCar;

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
	}
	
}
