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

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

class ConfiguredClass {
	
	private static Logger log = Logger.getLogger(ConfiguredClass.class);
	
	private final ClassConfigurator class_configurator;
	private final ArrayList<Object> created_instances;
	private final Class<?> target_class;
	
	private volatile JsonObject actual_class_configuration;
	
	ConfiguredClass(ClassConfigurator class_configurator, Gson gson, Class<?> target_class, JsonObject new_class_configuration) {
		this.class_configurator = class_configurator;
		if (class_configurator == null) {
			throw new NullPointerException("\"class_configurator\" can't to be null");
		}
		this.target_class = target_class;
		if (target_class == null) {
			throw new NullPointerException("\"target_class\" can't to be null");
		}
		if (class_configurator.isClassIsBlacklisted(target_class)) {
			throw new RuntimeException("Can't configure " + target_class + ", this is blacklisted");
		}
		
		created_instances = new ArrayList<>(1);
		actual_class_configuration = new_class_configuration;
	}
	
	void setupInstance(Object instance) {
		if (target_class.isAssignableFrom(instance.getClass()) == false) {
			throw new ClassCastException(instance.getClass().getName() + " is not assignable from " + target_class.getName());
		}
		// TODO4 call transformators (and create API)
		
		class_configurator.configureNewObjectWithJson(target_class, instance, actual_class_configuration);
		
		created_instances.add(instance);
	}
	
	void afterRemovedConf() {
		actual_class_configuration.keySet().stream().forEach(k -> {
			actual_class_configuration.add(k, JsonNull.INSTANCE);
		});
		created_instances.forEach(instance -> {
			class_configurator.reconfigureActualObjectWithJson(target_class, instance, actual_class_configuration);
		});
	}
	
	void updateInstances(JsonObject new_class_configuration) {
		JsonObject reconfiguration_json = new JsonObject();
		// XXX
		/*
		if (current.isJsonArray() | current.isJsonObject()) {
			jsonCompare(current, newer, (k_to_add, v) -> {
				if (v.isJsonNull()) {
					if (null_behavior == KeyValueNullContentMergeBehavior.KEEP) {
						current.getAsJsonObject().add(k_to_add, v);
					}
				} else {
					current.getAsJsonObject().add(k_to_add, v);
				}
			}, (k_to_remove, v) -> {
				if (null_behavior == KeyValueNullContentMergeBehavior.KEEP) {
					current.getAsJsonObject().add(k_to_remove, v);
				} else if (null_behavior == KeyValueNullContentMergeBehavior.REMOVE) {
					current.getAsJsonObject().remove(k_to_remove);
				}
			}, content_to_add -> {
				current.getAsJsonArray().add(content_to_add);
			}, (pos, content_to_remove) -> {
				current.getAsJsonArray().remove(pos);
			});
		} else if (current.isJsonPrimitive()) {
			throw new RuntimeException("Can't compare Json primitives");
		} else if (current.isJsonNull()) {
			throw new NullPointerException("Current JsonElement is null");
		} else {
			throw new RuntimeException("Invalid Current JsonElement type " + current.getClass().getName());
		}
		}
		 * */
		
		created_instances.forEach(instance -> {
			class_configurator.reconfigureActualObjectWithJson(target_class, instance, reconfiguration_json);
		});
		actual_class_configuration = new_class_configuration;
	}
	
	public String toString() {
		return target_class + " (for " + created_instances.size() + " instance(s))";
	}
	
}
