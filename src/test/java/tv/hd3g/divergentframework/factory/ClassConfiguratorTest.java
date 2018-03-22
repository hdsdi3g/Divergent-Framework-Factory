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

import java.net.InetSocketAddress;

import javax.mail.internet.InternetAddress;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.demo.SingleCar;

public class ClassConfiguratorTest extends TestCase {
	
	public void testBlackLists() {
		ClassConfigurator cc = new ClassConfigurator(new GsonKit(), c -> {
			return null;
		});
		
		assertTrue(cc.isClassIsBlacklisted(int.class));
		assertTrue(cc.isClassIsBlacklisted(Integer.class));
		assertTrue(cc.isClassIsBlacklisted(String.class));
		assertTrue(cc.isClassIsBlacklisted(InetSocketAddress.class));
		assertTrue(cc.isClassIsBlacklisted(InternetAddress.class));
	}
	
	public void testSimple() {
		ClassConfigurator cc = new ClassConfigurator(new GsonKit(), c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		JsonObject conf_tree = new JsonObject();
		conf_tree.addProperty("color", "blue");
		conf_tree.addProperty("size", 1);
		
		SingleCar car = new SingleCar();
		
		assertNull(car.getColor());
		assertEquals(0f, car.getSize());
		
		cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertEquals("blue", car.getColor());
		assertEquals(1.0f, car.getSize());
		
		assertNull(car.getPossible_wheel_type());
		assertNull(car.getPassager_names());
		assertNull(car.getPoints_by_names());
	}
	
	public void testReconfigure() {
		ClassConfigurator cc = new ClassConfigurator(new GsonKit(), c -> {
			try {
				return c.getConstructor().newInstance();
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
		
		conf_tree.addProperty("size", 2);
		assertEquals(1.0f, car.getSize());
		
		cc.reconfigureActualObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertEquals(2.0f, car.getSize());
		assertEquals("blue", car.getColor());
	}
	
	public void testValidator() {
		ClassConfigurator cc = new ClassConfigurator(new GsonKit(), c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		JsonObject conf_tree = new JsonObject();
		conf_tree.addProperty("size", 1);
		
		SingleCar car = new SingleCar();
		cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
		
		conf_tree.addProperty("size", 0);
		
		IllegalArgumentException iae = null;
		try {
			cc.reconfigureActualObjectWithJson(SingleCar.class, car, conf_tree);
			fail("Can't thown a validator error");
		} catch (IllegalArgumentException e) {
			iae = e;
		}
		assertNotNull(iae);
	}
	
	public void testCallbacks() {
		ClassConfigurator cc = new ClassConfigurator(new GsonKit(), c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		JsonObject conf_tree = new JsonObject();
		conf_tree.addProperty("color", "blue");
		
		SingleCar car = new SingleCar();
		
		assertEquals(0, car.counter_AfterInjectConfiguration.get());
		assertEquals(0, car.counter_BeforeUpdateConfiguration.get());
		assertEquals(0, car.counter_AfterUpdateConfiguration.get());
		
		cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertEquals(1, car.counter_AfterInjectConfiguration.get());
		assertEquals(0, car.counter_BeforeUpdateConfiguration.get());
		assertEquals(0, car.counter_AfterUpdateConfiguration.get());
		
		conf_tree.addProperty("color", "black");
		
		cc.reconfigureActualObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertEquals(1, car.counter_AfterInjectConfiguration.get());
		assertEquals(1, car.counter_BeforeUpdateConfiguration.get());
		assertEquals(1, car.counter_AfterUpdateConfiguration.get());
		
		/**
		 * Empty conf, no actions
		 */
		conf_tree.remove("color");
		cc.reconfigureActualObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertEquals(1, car.counter_AfterInjectConfiguration.get());
		assertEquals(1, car.counter_BeforeUpdateConfiguration.get());
		assertEquals(1, car.counter_AfterUpdateConfiguration.get());
	}
	
	public void testSimpleArrayList() {
		ClassConfigurator cc = new ClassConfigurator(new GsonKit(), c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		JsonObject conf_tree = new JsonObject();
		JsonArray ja = new JsonArray();
		ja.add("Huey");
		ja.add("Dewey");
		ja.add("Louie");
		conf_tree.add("passager_names", ja);
		
		SingleCar car = new SingleCar();
		
		assertNull(car.getPassager_names());
		
		cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertNotNull(car.getPassager_names());
		assertEquals(3, car.getPassager_names().size());
		
		assertEquals("Huey", car.getPassager_names().get(0));
		assertEquals("Dewey", car.getPassager_names().get(1));
		assertEquals("Louie", car.getPassager_names().get(2));
	}
	
	// XXX test map
	
	// TODO2 test with Wheel.class: assertEquals(0, wheel.counter_BeforeRemovedInConfiguration.get());
}
