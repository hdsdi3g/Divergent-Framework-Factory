/*
 * This file is part of Divergent-Framework-Factory.
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

import com.google.gson.JsonObject;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.demo.SingleCar;

public class ClassConfiguratorTest extends TestCase {
	
	public void testSimple() {
		
		Factory f = new Factory();
		ClassConfigurator cc = new ClassConfigurator(f.createGsonKit().getGson(), c -> {
			try {
				return f.create(c);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		JsonObject conf_tree = new JsonObject();
		conf_tree.addProperty("color", "blue");
		conf_tree.addProperty("size", 1);
		
		SingleCar car = new SingleCar();
		cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertEquals("blue", car.getColor());
		assertEquals(1.0f, car.getSize());
		
		assertNull(car.getPossible_wheel_type());
		assertNull(car.getPassager_names());
		assertNull(car.getPoints_by_names());
	}
	
}
