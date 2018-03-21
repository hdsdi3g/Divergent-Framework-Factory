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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.annotations.TargetGenericClassType;
import tv.hd3g.divergentframework.factory.demo.SingleCar;

public class ConfiguredClassEntryTest extends TestCase {
	private static Logger log = Logger.getLogger(ConfiguredClassEntryTest.class);
	
	public void testClassReflexion() {
		GsonKit g = new GsonKit();
		
		/**
		 * field name -> its conf
		 */
		JsonObject conf_tree = new JsonObject();
		conf_tree.addProperty("color", "blue");
		conf_tree.addProperty("size", 1);
		
		ConfiguredClassEntry<SingleCar> cce = null;// new ConfiguredClassEntry<>(g.getGson(), SingleCar.class, conf_tree);
		
		SingleCar car = new SingleCar();
		
		assertNull(car.getColor());
		assertNull(car.getPossible_wheel_type());
		assertNull(car.getPassager_names());
		assertNull(car.getPoints_by_names());
		
		cce.setupInstance(car);
		
		assertEquals("blue", car.getColor());
		assertEquals(1.0f, car.getSize());
		
		assertNull(car.getPossible_wheel_type());
		assertNull(car.getPassager_names());
		assertNull(car.getPoints_by_names());
		// XXX tests
	}
	
	public void testReflection() {
		
		GsonKit g = new GsonKit();
		
		SingleCar car = new SingleCar();
		
		assertNull(car.getColor());
		assertNull(car.getPossible_wheel_type());
		assertNull(car.getPassager_names());
		assertNull(car.getPoints_by_names());
		
		/**
		 * field name -> its conf
		 */
		HashMap<String, JsonElement> conf_tree = new HashMap<>();
		conf_tree.put("color", new JsonPrimitive("blue"));
		conf_tree.put("size", new JsonPrimitive(1));
		
		JsonArray ja_pn = new JsonArray();
		ja_pn.add("Huey");
		ja_pn.add("Dewey");
		ja_pn.add("Louie");
		conf_tree.put("passager_names", ja_pn);
		
		Arrays.asList(SingleCar.class.getDeclaredFields()).stream().filter(f -> {
			return conf_tree.containsKey(f.getName());
		}).filter(f -> {
			return f.trySetAccessible();
		}).filter(f -> {
			return Modifier.isStatic(f.getModifiers()) == false;
		}).filter(f -> {
			return Modifier.isFinal(f.getModifiers()) == false;
		}).forEach(f -> {
			String field_name = f.getName();
			Class<?> type = f.getType();
			
			try {
				if (type.isAssignableFrom(LinkedHashMap.class)) {
					System.out.println("Ok, LinkedHashMap " + f.getGenericType());// XXX
				} else if (type.isAssignableFrom(ArrayList.class)) {
					TargetGenericClassType tgct = f.getAnnotation(TargetGenericClassType.class);
					if (tgct == null) {
						/**
						 * Let Gson to get by
						 */
						f.set(car, g.getGson().fromJson(conf_tree.get(field_name), type));
					}
					
					System.out.println("Ok, ArrayList " + f.getGenericType());// XXX
				} else {
					f.set(car, g.getGson().fromJson(conf_tree.get(field_name), type));
				}
			} catch (JsonSyntaxException | IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		});
		
		assertEquals("blue", car.getColor());
		assertEquals(1.0f, car.getSize());
		
		assertNull(car.getPossible_wheel_type());
		assertNull(car.getPassager_names());
		assertNull(car.getPoints_by_names());
	}
	
}
