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
package tv.hd3g.divergentframework.factory.configuration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import tv.hd3g.divergentframework.factory.Logtoolkit;

class ConfigurationFile {
	private static Logger log = Logger.getLogger(ConfigurationFile.class);
	
	final File linked_file;
	private long last_update_date;
	private long last_file_size;
	
	final HashMap<Class<?>, JsonObject> config_tree_by_class;
	private final Function<String, Class<?>> classByMnemonicResolver;
	
	ConfigurationFile(File linked_file, Function<String, Class<?>> classByMnemonicResolver) {
		this.linked_file = linked_file;
		if (linked_file == null) {
			throw new NullPointerException("\"linked_file\" can't to be null");
		}
		this.classByMnemonicResolver = classByMnemonicResolver;
		if (classByMnemonicResolver == null) {
			throw new NullPointerException("\"classByMnemonicResolver\" can't to be null");
		}
		
		last_update_date = 0;
		last_file_size = -1;
		config_tree_by_class = new HashMap<>();
	}
	
	boolean isUpdated() {
		return last_update_date != linked_file.lastModified() | last_file_size != linked_file.length();
	}
	
	void switchUpdateStatus() {
		last_update_date = linked_file.lastModified();
		last_file_size = linked_file.length();
	}
	
	/**
	 * Don't update/sync main class config list, only reverberate real files content to Json trees.
	 * @return set/updated class conf
	 */
	List<Class<?>> parseFile() throws IOException {
		log.debug("Open conf file: " + linked_file);
		HashMap<String, JsonObject> raw_file_content = ConfigurationFileType.getTypeByFilename(linked_file).getContent(linked_file);
		
		/**
		 * Remove from current conf list the not present class in last configuration file content.
		 */
		config_tree_by_class.keySet().stream().filter(actual_configured_class -> {
			return raw_file_content.keySet().stream().map(mnemonic -> {
				return classByMnemonicResolver.apply(mnemonic);
			}).noneMatch(new_configured_class -> {
				return actual_configured_class.equals(new_configured_class);
			});
		}).collect(Collectors.toList()).forEach(class_to_remove -> {
			config_tree_by_class.remove(class_to_remove);
		});
		
		/**
		 * Add/update new configurations for classes
		 */
		return raw_file_content.keySet().stream().map(mnemonic -> {
			Class<?> target_class = classByMnemonicResolver.apply(mnemonic);
			
			if (target_class == null) {
				log.warn("Configuration try to setup a not found class for name (or mnemonic) \"" + mnemonic + "\"");
				return null;
			}
			
			JsonObject class_conf_tree = raw_file_content.get(mnemonic);
			
			if (log.isTraceEnabled()) {
				if (config_tree_by_class.containsKey(target_class)) {
					log.trace("Update configuration for " + target_class + ": " + class_conf_tree.toString() + " in " + linked_file.getName());
				} else {
					log.trace("Found configuration for " + target_class + ": " + class_conf_tree.toString() + " in " + linked_file.getName());
				}
			}
			
			config_tree_by_class.put(target_class, class_conf_tree);
			return target_class;
		}).filter(file -> {
			return file != null;
		}).distinct().collect(Collectors.toList());
		
	}
	
	public String toString() {
		String class_names = config_tree_by_class.keySet().stream().map(c_class -> c_class.getSimpleName()).collect(Collectors.joining(", "));
		
		if (last_update_date == 0) {
			return linked_file.getPath() + ", just detected, for " + class_names;
		} else {
			return linked_file.getPath() + ", updated " + Logtoolkit.dateLog(last_update_date) + "(" + last_file_size + " bytes) for " + class_names;
		}
	}
	
}
