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
import com.google.gson.JsonObject;

abstract class ConfiguredClassEntry<T> {
	
	private static Logger log = Logger.getLogger(ConfiguredClassEntry.class);
	
	private volatile JsonObject actual_class_configuration;
	
	private final ArrayList<T> created_instances;
	
	ConfiguredClassEntry(Gson gson, Class<T> target_class, JsonObject new_class_configuration) {
		created_instances = new ArrayList<>(1);
		
		// TODO manage sub Objects: create, inject conf, callbacks
		
		// TODO checks some non-sense with actual_class_configuration json
		actual_class_configuration = new_class_configuration;
	}
	
	void setupInstance(T instance) {
		/*if (target_class.isAssignableFrom(instance.getClass()) == false) {
			throw new ClassCastException(instance.getClass().getName() + " is not assignable from " + target_class.getName());
		}*/
		// TODO4 call transformators (and create API)
		
		// TODO2 check annotations for sub var class
		
		// TODO2 get TargetGenericClassType class, warn if this var is not a generic...
		
		// TODO2 call validators: behavior if not validate ?
		
		// TODO2 instance_to_configure.onAfterInjectConfiguration();
		
		created_instances.add(instance);
	}
	
	void afterRemovedConf() {
		// TODO3 callback all instances with OnAfterRemoveConfiguration
	}
	
	void beforeUpdateInstances() {
		// TODO3 callback all instances
	}
	
	void afterUpdateInstances() {
		// TODO3 callback all instances
	}
	
	void updateInstances(JsonObject new_class_configuration) {
		// TODO3 checks some non-sense with new_class_configuration json
		
		actual_class_configuration = new_class_configuration;// TODO3 compute deltas ?
		
		created_instances.forEach(instance -> {
			// TODO3 configured_instance.onBeforeUpdateConfiguration();
			// TODO3 do update
			// TODO3 configured_instance.onAfterUpdateConfiguration();
			
			// TODO3 call OnBeforeRemovedInConfiguration
		});
	}
	
	@Deprecated
	Class<?> getTargetClass() {
		return null;
	}
	
}
