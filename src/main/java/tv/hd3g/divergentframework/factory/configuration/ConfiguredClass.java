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

import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import tv.hd3g.divergentframework.factory.GsonKit;

class ConfiguredClass<T> {
	
	private final ClassConfigurator class_configurator;
	private final ArrayList<T> created_instances;
	private final Class<T> target_class;
	
	private volatile JsonObject actual_class_configuration;
	
	ConfiguredClass(ClassConfigurator class_configurator, Gson gson, Class<T> target_class, JsonObject class_configuration) {
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
		actual_class_configuration = class_configuration.deepCopy();
	}
	
	void setupInstance(T instance) {
		if (target_class.isAssignableFrom(instance.getClass()) == false) {
			throw new ClassCastException(instance.getClass().getName() + " is not assignable from " + target_class.getName());
		}
		// TODO call transformators (and create API)
		
		class_configurator.configureNewObjectWithJson(target_class, instance, actual_class_configuration);
		
		created_instances.add(instance);
	}
	
	void afterRemovedConf() {
		actual_class_configuration = new JsonObject();
		created_instances.forEach(instance -> {
			class_configurator.removeObjectConfiguration(target_class, instance);
		});
	}
	
	void updateInstances(JsonObject new_class_configuration) {
		JsonObject reconfiguration_json = actual_class_configuration.deepCopy();
		
		GsonKit.jsonMerge(reconfiguration_json, new_class_configuration, GsonKit.KeyValueNullContentMergeBehavior.KEEP);
		
		created_instances.forEach(instance -> {
			class_configurator.reconfigureActualObjectWithJson(target_class, instance, reconfiguration_json);
		});
		actual_class_configuration = reconfiguration_json;
	}
	
	public String toString() {
		return target_class + " (for " + created_instances.size() + " instance(s))";
	}
	
}
