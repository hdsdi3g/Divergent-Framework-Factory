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

import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import tv.hd3g.divergentframework.factory.annotations.Configurable;

public class ConfigurationUtility {
	private static Logger log = Logger.getLogger(ConfigurationUtility.class);
	
	// XXX inject conf files + vars + env
	
	private final LinkedHashMap<String, ClassEntry> configured_instances;
	
	public ConfigurationUtility() {// TODO set a Factory here
		configured_instances = new LinkedHashMap<>();
		Yaml y = new Yaml();
	}
	
	void addNewInstanceToConfigure(Configurable instance_to_configure) {
		String configuration_name = instance_to_configure.configurationName();
		if (configuration_name == null) {
			configuration_name = instance_to_configure.getClass().getSimpleName();
		} else if (configuration_name.trim().isEmpty()) {
			log.warn("Invalid configurationName for class " + instance_to_configure.getClass().getName());
			configuration_name = instance_to_configure.getClass().getSimpleName();
		}
		
		configuration_name = configuration_name.trim().toLowerCase();
		
		// TODO get conf tree for name
		// TODO behavior if it can't found conf...
		
		synchronized (configured_instances) {
			if (configured_instances.containsKey(configuration_name)) {
				throw new RuntimeException("Can't add a previously added configured instance: " + configuration_name);
			}
			configured_instances.put(configuration_name, new ClassEntry(instance_to_configure));// TODO inject tree conf in entry
		}
	}
	
	class ClassEntry {
		private final Configurable configured_instance;
		// Class<?> hooked_class;// TODO replace with a real callback API
		
		ClassEntry(Configurable instance_to_configure) {
			// TODO snif class
			
			// XXX call transformators (and create API)
			
			// XXX check ConfigurableVariable presence
			// XXX ConfigurableVariable: warn if this var is a generic...
			
			// XXX get ConfigurableGeneric class
			// XXX ConfigurableGeneric: warn if this var is NOT a generic...
			
			// XXX call validators: behavior if not validate ?
			
			instance_to_configure.onAfterInjectConfiguration();
			configured_instance = instance_to_configure;
		}
		
		void update() {
			if (configured_instance.enableConfigurationUpdate() == false) {
				return;
			}
			configured_instance.onBeforeUpdateConfiguration();
			// XXX do update
			configured_instance.onAfterUpdateConfiguration();
		}
		
	}
	
}
