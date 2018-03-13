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
package tv.hd3g.divergentframework.factory;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.JsonObject;

public class ConfigurationUtility {
	private static Logger log = Logger.getLogger(ConfigurationUtility.class);
	
	// XXX inject conf files + vars + env
	
	private final HashMap<Class<?>, ClassEntry<?>> configured_types;
	
	public ConfigurationUtility() {// TODO set a Factory here
		configured_types = new HashMap<>();
		Yaml y = new Yaml();// XXX import conf datas
	}
	
	boolean isClassIsConfigured() {
		return false;// XXX search in conf
	}
	
	<T> void addNewInstanceToConfigure(T instance_to_configure, Class<T> target_class) {
		synchronized (configured_types) {
			if (configured_types.containsKey(target_class) == false) {
				configured_types.put(target_class, new ClassEntry<T>(target_class)/** put conf */
				);
			}
		}
		
		@SuppressWarnings("unchecked")
		ClassEntry<T> c_e = (ClassEntry<T>) configured_types.get(target_class);
		c_e.setupInstance(instance_to_configure);
	}
	
	class ClassEntry<T> {
		
		private final Class<T> target_class;
		private volatile JsonObject actual_class_configuration;
		
		private ArrayList<T> created_instances;
		
		ClassEntry(Class<T> target_class) {// XXX Set just class
			this.target_class = target_class;
			created_instances = new ArrayList<>(1);
			// TODO snif class
		}
		
		private void setupInstance(T instance) {
			/*if (target_class.isAssignableFrom(instance.getClass()) == false) {
				throw new ClassCastException(instance.getClass().getName() + " is not assignable from " + target_class.getName());
			}*/
			// XXX call transformators (and create API)
			
			// XXX check annotations for sub var class
			
			// XXX get TargetGenericClassType class, warn if this var is not a generic...
			
			// XXX call validators: behavior if not validate ?
			
			// XXX instance_to_configure.onAfterInjectConfiguration();
			
			created_instances.add(instance);
		}
		
		void updateInstances(JsonObject new_class_configuration) {// XXX update all instances configured_types in 3 loops (Before/Update/After) ?
			this.actual_class_configuration = new_class_configuration;// XXX compute deltas ?
			
			created_instances.forEach(instance -> {
				// XXX configured_instance.onBeforeUpdateConfiguration();
				// XXX do update
				// XXX configured_instance.onAfterUpdateConfiguration();
				
				// XXX call OnBeforeRemovedInConfiguration
			});
		}
		
	}
	
}
