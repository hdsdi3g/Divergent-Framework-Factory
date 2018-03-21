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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

public class ConfigurationUtility {
	private static Logger log = Logger.getLogger(ConfigurationUtility.class);
	
	// TODO4 inject conf files + vars + env
	
	private final Factory factory;
	private final GsonKit gson_kit;
	private final HashMap<String, Class<?>> class_mnemonics;
	
	private final HashMap<Class<?>, ConfiguredClassEntry<?>> configured_types;// TODO4 change to sync hash map ?
	private final ArrayList<ConfigurationFile> configuration_files;
	private final ArrayList<File> watched_configuration_files_and_dirs;
	
	public ConfigurationUtility(Factory factory) {
		this.factory = factory;
		if (factory == null) {
			throw new NullPointerException("\"factory\" can't to be null");
		}
		class_mnemonics = new HashMap<>();
		gson_kit = factory.createGsonKit();
		
		configured_types = new HashMap<>();
		configuration_files = new ArrayList<>();
		watched_configuration_files_and_dirs = new ArrayList<>();
	}
	
	/**
	 * @return null if it can't found class
	 */
	private Class<?> getClassByMnemonic(String mnemonic) {
		if (class_mnemonics.containsKey(mnemonic)) {
			return class_mnemonics.get(mnemonic);
		} else {
			return factory.getClassByName(mnemonic);
		}
	}
	
	/**
	 * @param file is properties file syntax.
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
		
		log.debug("Load mnemonic conf file " + conf_file);
		
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
					return new ConfigurationFile(file);
				}).map(file -> {
					try {
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
	
	/*private class InternalConfiguredClassEntry<T> extends ConfiguredClassEntry<T> {
		
		InternalConfiguredClassEntry(Gson gson, Class<T> target_class, JsonObject new_class_configuration) {
			super(gson, target_class, new_class_configuration);
		}
		
		protected Object instanceNewObjectFromClass(Class<?> from_type) throws ReflectiveOperationException {
			return factory.create(from_type);
		}
		
		@Override
		protected void configureNewObjectWithJson(Class<?> from_type, Object new_created_instance, JsonElement configuration) {
			// TODO Auto-generated method stub
		}
		
		@Override
		protected void reconfigureActualObjectWithJson(Class<?> from_type, Object instance_to_update, JsonObject new_configuration) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		protected void callbackUpdateAPIForRemovedObject(Class<?> from_type, Object removed_instance_to_callback) {
			// TODO Auto-generated method stub
			
		}
		
	}*/
	
	public ConfigurationUtility injectConfiguration() {
		synchronized (configuration_files) {
			LinkedHashMap<ConfiguredClassEntry<?>, JsonObject> class_conf_to_update = new LinkedHashMap<>();
			
			synchronized (configured_types) {
				configuration_files.stream().filter(c_file -> {
					return c_file.isUpdated();
				}).forEach(conf -> {
					try {
						conf.parseFile().stream().forEach(set_updated_class_name -> {
							JsonObject new_config_for_class = conf.config_tree_by_class.get(set_updated_class_name);
							
							if (configured_types.containsKey(set_updated_class_name)) {
								ConfiguredClassEntry<?> current_class_entry = configured_types.get(set_updated_class_name);
								if (class_conf_to_update.containsKey(current_class_entry)) {
									GsonKit.jsonMergue(class_conf_to_update.get(current_class_entry), new_config_for_class);
								} else {
									class_conf_to_update.put(current_class_entry, new_config_for_class);
								}
							} else {
								configured_types.put(set_updated_class_name, null);// TODO new InternalConfiguredClassEntry<>(gson_kit.getGson(), set_updated_class_name, new_config_for_class));
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
			}
			
			/**
			 * Update all current configured classes
			 */
			if (class_conf_to_update.isEmpty() == false) {
				List<String> all_class_names = class_conf_to_update.keySet().stream().map(c_e -> {
					return c_e.getTargetClass().getName();
				}).collect(Collectors.toList());
				
				log.info("Update previously configured classes " + all_class_names);
				
				class_conf_to_update.keySet().forEach(c_e -> {
					c_e.beforeUpdateInstances();
				});
				class_conf_to_update.forEach((c_e, new_class_configuration) -> {
					if (log.isDebugEnabled()) {
						if (log.isTraceEnabled()) {
							log.trace("Update all configured instances for " + c_e.getTargetClass() + " with conf " + new_class_configuration);
						} else {
							log.debug("Update all configured instances for " + c_e.getTargetClass());
						}
					}
					c_e.updateInstances(new_class_configuration);
				});
				class_conf_to_update.keySet().forEach(c_e -> {
					c_e.afterUpdateInstances();
				});
			}
		}
		
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
		 * @return set/updated class conf
		 */
		List<Class<?>> parseFile() throws IOException {
			last_update_date = linked_file.lastModified();
			
			log.debug("Open conf file: " + linked_file);
			HashMap<String, JsonObject> raw_file_content = ConfigurationFileType.getTypeByFilename(linked_file).getContent(linked_file);
			
			/**
			 * Remove from current conf list the not present class in last configuration file content.
			 */
			config_tree_by_class.keySet().stream().filter(actual_configured_class -> {
				return raw_file_content.keySet().stream().map(mnemonic -> {
					return getClassByMnemonic(mnemonic);
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
				Class<?> target_class = getClassByMnemonic(mnemonic);
				
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
			return linked_file.getPath() + ", updated " + Logtoolkit.dateLog(last_update_date) + " (" + config_tree_by_class.size() + " classes)";
		}
		
	}
	
	boolean isClassIsConfigured(Class<?> reference_class) {
		return configured_types.containsKey(reference_class);
	}
	
	/**
	 * @param target_class do nothing if it's not configured.
	 */
	<T> void addNewClassInstanceToConfigure(T instance_to_configure, Class<T> target_class) {
		if (isClassIsConfigured(target_class) == false) {
			return;
		}
		
		@SuppressWarnings("unchecked")
		ConfiguredClassEntry<T> c_e = (ConfiguredClassEntry<T>) configured_types.get(target_class);
		c_e.setupInstance(instance_to_configure);
	}
	
}
