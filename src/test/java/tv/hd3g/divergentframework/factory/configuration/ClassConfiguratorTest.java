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
package tv.hd3g.divergentframework.factory.configuration;

import java.awt.Point;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

import javax.mail.internet.InternetAddress;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.Factory;
import tv.hd3g.divergentframework.factory.GsonKit;
import tv.hd3g.divergentframework.factory.configuration.demo.SingleCar;
import tv.hd3g.divergentframework.factory.configuration.demo.SingleCar.Wheel;
import tv.hd3g.divergentframework.factory.configuration.demo.SingleCar.WheelType;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub.Counters;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub.SubA;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub.SubA.SubB;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub.SubC;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub2;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub2.Sub2A;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub2.Sub2A.Sub2B;
import tv.hd3g.divergentframework.factory.configuration.demo.TMainSub2.Sub2C;

public class ClassConfiguratorTest extends TestCase {
	
	private static final GsonKit gson_kit = new Factory().createGsonKit();
	
	public void testBlackLists() {
		ClassConfigurator cc = new ClassConfigurator(gson_kit, c -> {
			return null;
		});
		
		assertTrue(cc.isClassIsBlacklisted(int.class));
		assertTrue(cc.isClassIsBlacklisted(Integer.class));
		assertTrue(cc.isClassIsBlacklisted(String.class));
		assertTrue(cc.isClassIsBlacklisted(InetSocketAddress.class));
		assertTrue(cc.isClassIsBlacklisted(InternetAddress.class));
	}
	
	public void testSimple() {
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
		
		SingleCar car = new SingleCar();
		cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertEquals("blue", car.getColor());
		assertEquals(1.0f, car.getSize());
		
		conf_tree.addProperty("size", 2);
		assertEquals(1.0f, car.getSize());
	}
	
	public void testValidator() {
		ClassConfigurator cc = new ClassConfigurator(gson_kit, c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		SingleCar car = new SingleCar();
		
		JsonObject conf_tree = new JsonObject();
		conf_tree.addProperty("size", 0);
		
		IllegalArgumentException iae = null;
		try {
			cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
			fail("Can't thown a validator error");
		} catch (IllegalArgumentException e) {
			iae = e;
		}
		assertNotNull(iae);
	}
	
	public void testCallbacks() {
		ClassConfigurator cc = new ClassConfigurator(gson_kit, c -> {
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
		
		cc.configureNewObjectWithJson(SingleCar.class, car, conf_tree);
		
		assertEquals(1, car.counter_AfterInjectConfiguration.get());
	}
	
	public void testSimpleList() {
		ClassConfigurator cc = new ClassConfigurator(gson_kit, c -> {
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
		ClassConfigurator cc = new ClassConfigurator(gson_kit, c -> {
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
		ClassConfigurator cc = new ClassConfigurator(gson_kit, c -> {
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
		ClassConfigurator cc = new ClassConfigurator(gson_kit, c -> {
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
		
		assertEquals(1, car.getPossible_wheel_type().get(0).counter_AfterInjectConfiguration.get());
		assertEquals(1, car.getPossible_wheel_type().get(1).counter_AfterInjectConfiguration.get());
		
		jo_wheel2.remove("size");
		jo_wheel2.addProperty("type", WheelType.suv.name());
		ja.remove(0);
		assertEquals(1, conf_tree.get("possible_wheel_type").getAsJsonArray().size());
	}
	
	public void testComplexMap() throws IOException {
		ClassConfigurator cc = new ClassConfigurator(gson_kit, c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		TMainSub2 main = new TMainSub2();
		
		final UUID uuid = UUID.randomUUID();
		
		/**
		 * Push tree
		 */
		JsonObject conf_tree = ConfigurationFileType.YAML.getContent(pw -> {
			pw.println("test:");
			pw.println("   sub_a:");// var
			pw.println("      a_1:");// map key
			pw.println("         sub_b:");// var
			pw.println("            b_1:");// map key
			pw.println("               sub_c:");// var
			pw.println("                  c_1:");// map key
			pw.println("                     uuid: \"" + uuid + "\"");
		}).get("test");
		
		cc.configureNewObjectWithJson(TMainSub2.class, main, conf_tree);
		
		assertNotNull(main.sub_a);
		assertNotNull(main.sub_a.get("a_1"));
		assertNotNull(main.sub_a.get("a_1").sub_b);
		assertNotNull(main.sub_a.get("a_1").sub_b.get("b_1"));
		assertNotNull(main.sub_a.get("a_1").sub_b.get("b_1").sub_c);
		assertNotNull(main.sub_a.get("a_1").sub_b.get("b_1").sub_c.get("c_1").uuid);
		assertEquals(uuid, main.sub_a.get("a_1").sub_b.get("b_1").sub_c.get("c_1").uuid);
		
		Sub2A actual_a_1 = main.sub_a.get("a_1");
		Sub2B actual_b_1 = main.sub_a.get("a_1").sub_b.get("b_1");
		Sub2C actual_c_1 = main.sub_a.get("a_1").sub_b.get("b_1").sub_c.get("c_1");
		
		checkCountersTMainSub("Pass1", actual_a_1, 1, 0, 0, 0);
		checkCountersTMainSub("Pass1", actual_b_1, 1, 0, 0, 0);
		checkCountersTMainSub("Pass1", actual_c_1, 1, 0, 0, 0);
	}
	
	static void checkCountersTMainSub(String message, Counters to_check, int expected_after_inject, int expected_before_update, int expected_after_update, int expected_before_remove) {
		assertNotNull(to_check);
		assertEquals(to_check.getClass().getSimpleName() + " " + message + " AfterInject", expected_after_inject, to_check.counter_AfterInjectConfiguration.get());
	}
	
	public void testClassRussianDolls() throws IOException {
		ClassConfigurator cc = new ClassConfigurator(gson_kit, c -> {
			try {
				return c.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		
		TMainSub main = new TMainSub();
		
		final UUID uuid = UUID.randomUUID();
		
		/**
		 * Push tree
		 */
		JsonObject conf_tree = ConfigurationFileType.YAML.getContent(pw -> {
			pw.println("test:");
			pw.println("   sub_a:");// var
			pw.println("         sub_b:");// var
			pw.println("               sub_c:");// var
			pw.println("                     uuid: \"" + uuid + "\"");
		}).get("test");
		
		cc.configureNewObjectWithJson(TMainSub.class, main, conf_tree);
		
		assertNotNull(main.sub_a);
		assertNotNull(main.sub_a.sub_b);
		assertNotNull(main.sub_a.sub_b.sub_c);
		assertNotNull(main.sub_a.sub_b.sub_c.uuid);
		assertEquals(uuid, main.sub_a.sub_b.sub_c.uuid);
		
		SubA actual_a_1 = main.sub_a;
		SubB actual_b_1 = main.sub_a.sub_b;
		SubC actual_c_1 = main.sub_a.sub_b.sub_c;
		
		checkCountersTMainSub("Pass1", actual_a_1, 1, 0, 0, 0);
		checkCountersTMainSub("Pass1", actual_b_1, 1, 0, 0, 0);
		checkCountersTMainSub("Pass1", actual_c_1, 1, 0, 0, 0);
	}
	
}
