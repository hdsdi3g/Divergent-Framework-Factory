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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import tv.hd3g.divergentframework.factory.Factory;
import tv.hd3g.divergentframework.factory.GsonKit;
import tv.hd3g.divergentframework.factory.GsonKit.KeyValueNullContentMergeBehavior;

public class ConfigurationUtility {
	private static Logger log = Logger.getLogger(ConfigurationUtility.class);
	
	// TODO2 add -regular- watcher, retrieve directory activities and specific files, for specific types (ConfigurationFileType.CONFIG_FILE_EXTENTIONS)
	
	private final Factory factory;
	private final GsonKit gson_kit;
	private final HashMap<String, Class<?>> class_mnemonics;
	private final ClassConfigurator class_configurator;
	
	private final ConcurrentHashMap<Class<?>, ConfiguredClass<?>> configured_types;
	private final ArrayList<ConfigurationFile> configuration_files;
	private final ArrayList<File> watched_configuration_files_and_dirs;
	
	public ConfigurationUtility(Factory factory) {
		this.factory = factory;
		if (factory == null) {
			throw new NullPointerException("\"factory\" can't to be null");
		}
		class_mnemonics = new HashMap<>();
		gson_kit = factory.createGsonKit();
		class_configurator = new ClassConfigurator(gson_kit, c -> {
			try {
				return factory.create(c);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Can't instance class " + c, e);
			}
		});
		
		configured_types = new ConcurrentHashMap<>();
		configuration_files = new ArrayList<>();
		watched_configuration_files_and_dirs = new ArrayList<>();
	}
	
	/**
	 * Before set configuration files.
	 * @param file is a properties file. Syntax: short_name=full_class_name
	 * @return this
	 */
	public ConfigurationUtility loadMnemonicClassNameListFromFile(File conf_file) throws IOException {
		if (conf_file.exists() == false) {
			throw new FileNotFoundException(conf_file + " don't exists");
		} else if (conf_file.canRead() == false) {
			throw new IOException("Can't read " + conf_file);
		} else if (conf_file.isFile() == false) {
			throw new FileNotFoundException(conf_file + " is not a file");
		}
		
		log.info("Load mnemonic conf file " + conf_file);
		
		Properties p = new Properties();
		FileInputStream fis = new FileInputStream(conf_file);
		p.load(fis);
		fis.close();
		
		p.forEach((k, v) -> {
			String mnemonic = (String) k;
			String classname = (String) v;
			Class<?> target = factory.getClassByName(classname);
			if (target != null) {
				class_mnemonics.put(mnemonic, target);
			} else {
				log.debug("Can't found class " + classname + " for mnemonic " + mnemonic);
			}
		});
		
		if (log.isTraceEnabled()) {
			log.trace("Load mnemonic definitions" + class_mnemonics);
		}
		
		return this;
	}
	
	/**
	 * @param search all files in list and in dirs (don't search in sub dir).
	 * @return this
	 */
	public ConfigurationUtility addConfigurationFilesToInternalList(File... conf_file_or_dir) {
		if (conf_file_or_dir == null) {
			return this;
		}
		
		List<File> conf_file_or_dir_list = Arrays.asList(conf_file_or_dir);
		log.info("Add configuration files: " + conf_file_or_dir_list);
		
		synchronized (watched_configuration_files_and_dirs) {
			conf_file_or_dir_list.stream().forEach(file -> {
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
	 * With watched_configuration_files_and_dirs, update content and internal content of configuration_files. Do regulary this.
	 * @return this
	 */
	public ConfigurationUtility scanImportedFilesAndUpdateConfigurations() {
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
			
			if (last_current_founded_files.isEmpty() == false && log.isDebugEnabled()) {
				log.debug("Current last_current_founded_files: " + last_current_founded_files);
			}
			
			synchronized (configuration_files) {
				List<ConfigurationFile> added_configuration_files = last_current_founded_files.stream().filter(file -> {
					/**
					 * add new added files
					 */
					return configuration_files.stream().anyMatch(c_file -> {
						return c_file.linked_file.equals(file);
					}) == false;
				}).map(file -> {
					return new ConfigurationFile(file, mnemonic -> {
						/**
						 * @return null if it can't found class
						 */
						if (class_mnemonics.containsKey(mnemonic)) {
							return class_mnemonics.get(mnemonic);
						} else {
							return factory.getClassByName(mnemonic);
						}
					});
				}).map(file -> {
					try {
						/**
						 * Test new syntax
						 */
						file.parseFile();
						return file;
					} catch (IOException e) {
						log.error("Can't parse config file " + file, e);
						return null;
					}
				}).filter(file -> {
					return file != null;
				}).peek(c_file -> {
					log.info("Found a new config file: " + c_file);
				}).collect(Collectors.toList());
				
				if (added_configuration_files.isEmpty() == false && log.isDebugEnabled()) {
					log.debug("Current added_configuration_files: " + added_configuration_files);
				}
				
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
				
				/**
				 * Apply newer configurations
				 */
				synchronized (configured_types) {
					LinkedHashMap<ConfiguredClass<?>, JsonObject> class_conf_to_update = new LinkedHashMap<>();
					
					configuration_files.stream().filter(c_file -> {
						return last_current_founded_files.stream().anyMatch(file -> {
							return c_file.linked_file.equals(file);
						});
					}).filter(c_file -> {
						return c_file.isUpdated();
					}).peek(file -> {
						log.trace("!!!!" + file);
					}).forEach(conf -> {
						try {
							log.info("Found an updated config file: " + conf);
							
							conf.switchUpdateStatus();
							conf.parseFile().stream().forEach(set_updated_class_name -> {
								JsonObject new_config_for_class = conf.config_tree_by_class.get(set_updated_class_name);
								
								if (configured_types.containsKey(set_updated_class_name)) {
									ConfiguredClass<?> current_class_entry = configured_types.get(set_updated_class_name);
									if (class_conf_to_update.containsKey(current_class_entry)) {
										GsonKit.jsonMerge(class_conf_to_update.get(current_class_entry), new_config_for_class, KeyValueNullContentMergeBehavior.KEEP);
									} else {
										class_conf_to_update.put(current_class_entry, new_config_for_class);
									}
								} else {
									configured_types.put(set_updated_class_name, new ConfiguredClass<>(class_configurator, gson_kit.getGson(), set_updated_class_name, new_config_for_class));
								}
							});
						} catch (IOException e) {
							log.error("Can't read/parse file " + conf.linked_file, e);
						}
					});
					
					/**
					 * Do a reverse search for removed classes
					 */
					List<Class<?>> all_actual_configured_classes = configuration_files.stream().flatMap(c_f -> {
						return c_f.config_tree_by_class.keySet().stream();
					}).collect(Collectors.toList());
					
					configured_types.keySet().stream().filter(current_configured_class -> {
						return all_actual_configured_classes.contains(current_configured_class) == false;
					}).collect(Collectors.toList()).forEach(class_not_actually_configured -> {
						log.debug("Remove configuration tree for " + class_not_actually_configured);
						configured_types.remove(class_not_actually_configured).afterRemovedConf();
					});
					
					/**
					 * Update all current configured classes
					 */
					if (class_conf_to_update.isEmpty() == false) {
						if (log.isTraceEnabled()) {
							log.trace("Update previously configured classes " + class_conf_to_update);
						} else if (log.isDebugEnabled()) {
							log.debug("Update previously configured classes " + class_conf_to_update.keySet());
						}
						
						class_conf_to_update.forEach((c_e, new_class_configuration) -> {
							if (log.isDebugEnabled()) {
								if (log.isTraceEnabled()) {
									log.trace("Update all configured instances for " + c_e + " with conf " + new_class_configuration);
								} else {
									log.debug("Update all configured instances for " + c_e);
								}
							}
							c_e.updateInstances(new_class_configuration);
						});
					}
				}
			}
		}
		
		return this;
	}
	
	public boolean isClassIsConfigured(Class<?> reference_class) {
		return configured_types.containsKey(reference_class);
	}
	
	/**
	 * @param target_class do nothing if it's not configured.
	 */
	public <T> void addNewClassInstanceToConfigure(T instance_to_configure, Class<T> target_class) {
		if (isClassIsConfigured(target_class) == false) {
			return;
		}
		
		@SuppressWarnings("unchecked")
		ConfiguredClass<T> c_e = (ConfiguredClass<T>) configured_types.get(target_class);
		c_e.setupInstance(instance_to_configure);
	}
	
}
