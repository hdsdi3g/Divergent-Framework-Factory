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

import java.awt.Point;
import java.net.InetSocketAddress;

import javax.mail.internet.InternetAddress;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.demo.SingleCar;
import tv.hd3g.divergentframework.factory.demo.SingleCar.Wheel;
import tv.hd3g.divergentframework.factory.demo.SingleCar.WheelType;

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
	
	public void testSimpleList() {
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
	
	public void testSimpleMap() {
		ClassConfigurator cc = new ClassConfigurator(new GsonKit(), c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		JsonObject conf_tree = new JsonObject();
		JsonObject jo = new JsonObject();
		
		JsonObject jo_p1 = new JsonObject();
		jo_p1.addProperty("x", 1);
		jo_p1.addProperty("y", 2);
		jo.add("first", jo_p1);
		
		JsonObject jo_p2 = new JsonObject();
		jo_p2.addProperty("x", 3);
		jo_p2.addProperty("y", 4);
		jo.add("second", jo_p2);
		
		conf_tree.add("points_by_names", jo);
		
		SingleCar car = new SingleCar();
		
		assertNull(car.getPoints_by_names());
		
		cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertNotNull(car.getPoints_by_names());
		assertEquals(2, car.getPoints_by_names().size());
		
		assertNotNull(car.getPoints_by_names().containsKey("first"));
		assertNotNull(car.getPoints_by_names().containsKey("second"));
		
		assertEquals(new Point(1, 2), car.getPoints_by_names().get("first"));
		assertEquals(new Point(3, 4), car.getPoints_by_names().get("second"));
		
	}
	
	public void testSubclass() {
		ClassConfigurator cc = new ClassConfigurator(new GsonKit(), c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		JsonObject conf_tree = new JsonObject();
		JsonObject jo = new JsonObject();
		jo.addProperty("size", 5);
		jo.addProperty("type", WheelType.sedan.name());
		conf_tree.add("default_wheel", jo);
		
		SingleCar car = new SingleCar();
		
		assertNull(car.getDefault_wheel());
		
		cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertNotNull(car.getDefault_wheel());
		
		Wheel real_wheel = new Wheel();
		real_wheel.size = 5;
		real_wheel.type = WheelType.sedan;
		assertEquals(real_wheel, car.getDefault_wheel());
	}
	
	public void testComplexList() {
		ClassConfigurator cc = new ClassConfigurator(new GsonKit(), c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		JsonObject jo_wheel1 = new JsonObject();
		jo_wheel1.addProperty("size", 1);
		jo_wheel1.addProperty("type", WheelType.sedan.name());
		
		JsonObject jo_wheel2 = new JsonObject();
		jo_wheel2.addProperty("size", 2);
		jo_wheel2.addProperty("type", WheelType.formula1.name());
		
		JsonArray ja = new JsonArray();
		ja.add(jo_wheel1);
		ja.add(jo_wheel2);
		
		JsonObject conf_tree = new JsonObject();
		conf_tree.add("possible_wheel_type", ja);
		
		SingleCar car = new SingleCar();
		
		assertNull(car.getPossible_wheel_type());
		
		cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertNotNull(car.getPossible_wheel_type());
		assertEquals(2, car.getPossible_wheel_type().size());
		
		Wheel real_wheel1 = new Wheel();
		real_wheel1.size = 1;
		real_wheel1.type = WheelType.sedan;
		
		Wheel real_wheel2 = new Wheel();
		real_wheel2.size = 2;
		real_wheel2.type = WheelType.formula1;
		
		assertEquals(real_wheel1, car.getPossible_wheel_type().get(0));
		assertEquals(real_wheel2, car.getPossible_wheel_type().get(1));
		
		assertEquals(0, car.getPossible_wheel_type().get(0).counter_BeforeRemovedInConfiguration.get());
		assertEquals(0, car.getPossible_wheel_type().get(1).counter_BeforeRemovedInConfiguration.get());
		
		assertEquals(0, car.getPossible_wheel_type().get(0).counter_AfterUpdateConfiguration.get());
		assertEquals(0, car.getPossible_wheel_type().get(1).counter_AfterUpdateConfiguration.get());
		
		jo_wheel2.remove("size");
		jo_wheel2.addProperty("type", WheelType.suv.name());
		ja.remove(0);
		assertEquals(1, conf_tree.get("possible_wheel_type").getAsJsonArray().size());
		
		Wheel normally_removed_from_list = car.getPossible_wheel_type().get(0);
		cc.reconfigureActualObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertNotNull(car.getPossible_wheel_type());
		assertEquals(1, car.getPossible_wheel_type().size());
		assertEquals(WheelType.suv, car.getPossible_wheel_type().get(0).type);
		assertEquals(2, car.getPossible_wheel_type().get(0).size);
		
		assertEquals(0, car.getPossible_wheel_type().get(0).counter_BeforeRemovedInConfiguration.get());
		assertEquals(1, car.getPossible_wheel_type().get(0).counter_AfterUpdateConfiguration.get());
		
		assertEquals(1, normally_removed_from_list.counter_BeforeRemovedInConfiguration.get());
		assertEquals(0, normally_removed_from_list.counter_AfterUpdateConfiguration.get());
	}
	
}
