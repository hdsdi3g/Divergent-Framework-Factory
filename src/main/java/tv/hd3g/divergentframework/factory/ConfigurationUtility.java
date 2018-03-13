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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

public class ConfigurationUtility {
	private static Logger log = Logger.getLogger(ConfigurationUtility.class);
	
	// XXX inject conf files + vars + env
	
	private static final String[] CONFIG_FILE_EXT = { ".yml", ".yaml", ".json", ".ini", ".properties" };
	
	private final ArrayList<File> all_configuration_files_and_dir;
	private final LinkedHashMap<File, Long> all_watched_configuration_files_last_dates;
	private final ConcurrentHashMap<File, List<Class<?>>> configured_classes_by_config_file;
	
	private final HashMap<Class<?>, ClassEntry<?>> configured_types;
	
	public ConfigurationUtility() {// TODO set a Factory here
		configured_types = new HashMap<>();
		all_configuration_files_and_dir = new ArrayList<>();
		all_watched_configuration_files_last_dates = new LinkedHashMap<>();
		configured_classes_by_config_file = new ConcurrentHashMap<>();
	}
	
	/**
	 * @param search all files in list and in dirs (don't search in sub dir).
	 * @return this
	 */
	public ConfigurationUtility addConfigurationFiles(File... conf_file_or_dir) {
		synchronized (all_configuration_files_and_dir) {
			all_configuration_files_and_dir.addAll(Arrays.asList(conf_file_or_dir));
			importAndRefreshConfigurationFromFiles();
		}
		
		return this;
	}
	
	private void importAndRefreshConfigurationFromFiles() {
		List<File> all_current_files = all_configuration_files_and_dir.stream().flatMap(source -> {
			if (source.isFile()) {
				return Stream.of(source);
			} else if (source.isDirectory()) {
				return FileUtils.listFiles(source, CONFIG_FILE_EXT, true).stream();
			} else {
				return null;
			}
		}).filter(file -> {
			return file != null;
		}).filter(file -> {
			return file.isHidden() == false;
		}).filter(file -> {
			return file.canRead();
		}).filter(file -> {
			return file.length() > 0;
		}).map(file -> {
			try {
				return file.getCanonicalFile();
			} catch (IOException e) {
				log.error("Can't get file " + file, e);
				return null;
			}
		}).filter(file -> {
			return file != null;
		}).distinct().collect(Collectors.toList());
		
		synchronized (all_watched_configuration_files_last_dates) {
			List<File> new_and_updated_files = all_current_files.stream().map(file -> {
				Long last_presence = all_watched_configuration_files_last_dates.put(file, file.lastModified());
				if (last_presence == null) {
					/**
					 * New file to parse
					 */
					return file;
				} else if (last_presence != file.lastModified()) {
					/**
					 * Updated file
					 */
					return file;
				} else {
					return null;
				}
			}).filter(file -> {
				return file != null;
			}).collect(Collectors.toList());
			
			List<File> removed_files = all_watched_configuration_files_last_dates.keySet().stream().filter(file -> {
				return all_current_files.contains(file) == false;
			}).collect(Collectors.toList());
			
			removed_files.forEach(file -> {
				all_watched_configuration_files_last_dates.remove(file);
				
				configured_classes_by_config_file.getOrDefault(file, Collections.emptyList()).forEach(configured_class -> {
					// XXX remove this class from config
				});
				configured_classes_by_config_file.remove(file);
			});
			
			new_and_updated_files.forEach(file -> {
				// XXX parse and get new_and_updated_files
				
				// XXX push class to file list
				List<Class<?>> current_classes_configured_in_file = configured_classes_by_config_file.computeIfAbsent(file, f -> new ArrayList<>(1));
				/*if (current_classes_configured_in_file.contains(null) ==false) {
					current_classes_configured_in_file.add(null);
				}*/
			});
			
		}
		
		// Yaml y = new Yaml();// XXX import conf datas + ".json", ".ini", ".properties"
		// XXX check tabs in Yaml files before opening
	}
	
	private class ConfFile {
		final File linked_file;
		long last_update_date;
		final HashMap<Class<?>, JsonObject> config_tree_by_class;
		
		ConfFile(File linked_file) {
			this.linked_file = linked_file;
			last_update_date = linked_file.lastModified();
			config_tree_by_class = new HashMap<>();
		}
		
		boolean isUpdated() {
			return last_update_date != linked_file.lastModified();
		}
		
		HashMap<Class<?>, JsonObject> parseFile() {
			// XXX open file
			// XXX get classes config
			
			// XXX get changes
			HashMap<Class<?>, JsonObject> new_config_tree_by_class = new HashMap<>();
			
			return new_config_tree_by_class;
		}
		
	}
	
	boolean isClassIsConfigured() {
		return false;// XXX search in conf
	}
	
	<T> void addNewInstanceToConfigure(T instance_to_configure, Class<T> target_class) {
		synchronized (configured_types) {
			if (configured_types.containsKey(target_class) == false) {
				configured_types.put(target_class, new ClassEntry<>(target_class)/** put conf */
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
