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
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

public class ConfigurationUtility {
	private static Logger log = Logger.getLogger(ConfigurationUtility.class);
	
	// XXX inject conf files + vars + env
	
	private final HashMap<Class<?>, ClassEntry<?>> configured_types;
	private final ArrayList<ConfigurationFile> configuration_files;
	private final ArrayList<File> watched_configuration_files_and_dirs;
	
	public ConfigurationUtility() {// TODO set a Factory here
		configured_types = new HashMap<>();
		configuration_files = new ArrayList<>();
		watched_configuration_files_and_dirs = new ArrayList<>();
	}
	
	/**
	 * @param search all files in list and in dirs (don't search in sub dir).
	 * @return this
	 */
	public ConfigurationUtility addConfigurationFiles(File... conf_file_or_dir) {
		if (conf_file_or_dir == null) {
			return this;
		}
		log.info("Add configuration files: " + conf_file_or_dir);
		
		synchronized (watched_configuration_files_and_dirs) {
			Arrays.asList(conf_file_or_dir).stream().forEach(file -> {
				if (file == null) {
					return;
				}
				try {
					File cn_file = file.getCanonicalFile();
					if (watched_configuration_files_and_dirs.contains(cn_file) == false) {
						log.trace("Add to internal watched_configuration_files_and_dirs list " + cn_file);
						watched_configuration_files_and_dirs.add(cn_file);
					}
				} catch (IOException e) {
					throw new RuntimeException("Can't get file " + file, e);
				}
			});
		}
		
		return this;
	}
	
	/**
	 * With watched_configuration_files_and_dirs, update content and internal content of configuration_files.
	 * @return this
	 */
	public ConfigurationUtility scanAndImportFiles() {
		synchronized (watched_configuration_files_and_dirs) {
			List<File> last_current_founded_files = watched_configuration_files_and_dirs.stream().flatMap(file -> {
				if (file.isFile()) {
					return Stream.of(file);
				} else if (file.isDirectory()) {
					return FileUtils.listFiles(file, ConfigurationFileType.CONFIG_FILE_EXTENTIONS, true).stream();
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
			}).filter(file -> {
				/**
				 * Keep only known extentions.
				 */
				return Arrays.asList(ConfigurationFileType.CONFIG_FILE_EXTENTIONS).stream().map(ext -> ext.substring(1)).anyMatch(ext -> {
					return ext.equalsIgnoreCase(FilenameUtils.getExtension(file.getPath()));
				});
			}).map(file -> {
				try {
					return file.getCanonicalFile();
				} catch (IOException e) {
					log.error("Can't get file " + file, e);
					return null;
				}
			}).filter(file -> {
				return file != null;
			}).distinct().peek(file -> {
				log.trace("Found config file: " + file);
			}).collect(Collectors.toList());
			
			synchronized (configuration_files) {
				List<ConfigurationFile> added_configuration_files = last_current_founded_files.stream().filter(file -> {
					return configuration_files.stream().anyMatch(c_file -> {
						return c_file.linked_file.equals(file) == false;
					});
				}).map(file -> {
					try {
						return new ConfigurationFile(file).parseFile();
					} catch (IOException e) {
						log.error("Can't parse config file " + file, e);
						return null;
					}
				}).filter(file -> {
					return file != null;
				}).peek(c_file -> {
					log.info("Found a new config file: " + c_file);
				}).collect(Collectors.toList());
				
				configuration_files.addAll(added_configuration_files);
				
				configuration_files.removeIf(c_file -> {
					boolean must_remove = last_current_founded_files.stream().noneMatch(file -> {
						return c_file.linked_file.equals(file);
					});
					if (must_remove) {
						log.info("Can't found a config file, remove it from watching: " + c_file);
					}
					return must_remove;
				});
				
				configuration_files.stream().filter(c_file -> {
					return last_current_founded_files.stream().anyMatch(file -> {
						return c_file.linked_file.equals(file);
					});
				}).filter(c_file -> {
					return c_file.isUpdated();
				}).map(c_file -> {
					try {
						return c_file.parseFile();
					} catch (IOException e) {
						log.error("Can't parse config file " + c_file.linked_file, e);
						return null;
					}
				}).filter(file -> {
					return file != null;
				}).forEach(c_file -> {
					log.info("Found an updated config file: " + c_file);
				});
				
			}
		}
		
		return this;
	}
	
	public ConfigurationUtility injectConfiguration() {
		// TODO mergue all configuration_files to configured_types with multiple detection, added, updated and removed classes.
		
		// TODO map for class names <-> mnemonics
		
		return this;
	}
	
	class ConfigurationFile {
		final File linked_file;
		long last_update_date;
		final HashMap<Class<?>, JsonObject> config_tree_by_class;
		
		private ConfigurationFile(File linked_file) {
			this.linked_file = linked_file;
			last_update_date = linked_file.lastModified();
			config_tree_by_class = new HashMap<>();
		}
		
		boolean isUpdated() {
			return last_update_date != linked_file.lastModified();
		}
		
		/**
		 * Don't update/sync main class config list, only reverberate real files content to Json trees.
		 * @return this
		 */
		ConfigurationFile parseFile() throws IOException {
			last_update_date = linked_file.lastModified();
			
			HashMap<String, JsonObject> raw_file_content = ConfigurationFileType.getTypeByFilename(linked_file).getContent(linked_file);
			
			// XXX get classes config
			// XXX found duplicate class in this config
			
			return this;
		}
		
		public String toString() {
			return linked_file.getPath() + ", updated " + Logtoolkit.dateLog(last_update_date) + " (" + config_tree_by_class.size() + " classes)";
		}
	}
	
	boolean isClassIsConfigured(Class<?> reference_class) {
		return configured_types.containsKey(reference_class);
	}
	
	/**
	 * @param target_class do nothing if it's not configured.
	 */
	<T> void addNewInstanceToConfigure(T instance_to_configure, Class<T> target_class) {
		if (isClassIsConfigured(target_class) == false) {
			return;
		}
		
		@SuppressWarnings("unchecked")
		ClassEntry<T> c_e = (ClassEntry<T>) configured_types.get(target_class);
		c_e.setupInstance(instance_to_configure);
	}
	
	class ClassEntry<T> {
		
		private final Class<T> target_class;
		private volatile JsonObject actual_class_configuration;
		
		private ArrayList<T> created_instances;
		
		private ClassEntry(Class<T> target_class) {
			this.target_class = target_class;
			created_instances = new ArrayList<>(1);
			
			actual_class_configuration = configuration_files.stream().filter(c_file -> {
				return c_file.config_tree_by_class.containsKey(target_class);
			}).findFirst().orElseThrow(() -> {
				return new NullPointerException("Can't found configuration file for class " + target_class);
			}).config_tree_by_class.get(target_class);
			
			// TODO snif class and checks some non-sense with actual_class_configuration json
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
