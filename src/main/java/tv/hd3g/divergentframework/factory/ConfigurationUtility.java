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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

public class ConfigurationUtility {
	private static Logger log = Logger.getLogger(ConfigurationUtility.class);
	
	// XXX create backend / inject conf files + vars
	
	private final HashMap<String, ClassEntry> entry_tree;
	
	public ConfigurationUtility() {
		entry_tree = new HashMap<>();
		Yaml y = new Yaml();
	}
	
	class ClassEntry {
		private final Properties content;
		Class<?> hooked_class;// TODO replace with a real callback API
		
		public ClassEntry() {
			content = new Properties();
		}
		
		void putAll(Map<String, Object> entries) {
			content.putAll(entries);
		}
		
	}
	
}
