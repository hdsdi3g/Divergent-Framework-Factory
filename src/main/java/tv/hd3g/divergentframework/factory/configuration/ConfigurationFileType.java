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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import tv.hd3g.divergentframework.factory.GsonKit;
import tv.hd3g.divergentframework.factory.GsonKit.KeyValueNullContentMergeBehavior;

enum ConfigurationFileType {
	
	/**
	 * Format:
	 * mnemonic:
	 * (spaces) variable: value
	 * ... (you can use --- document separator for overload entries)
	 * - mnemonic can be a class name or a short name.
	 * - value can be a simple json primitive compatible (String, number, boolean) or a complex json data.
	 */
	YAML {
		public List<String> getExtentions() {
			return Arrays.asList(".yml", ".yaml");
		}
		
		public HashMap<String, JsonObject> getContent(File config_file) throws IOException {
			try {
				return getContent(new FileReader(config_file));
			} catch (Exception e) {
				throw new IOException("Invalid YAML file: " + config_file);
			}
		}
		
		HashMap<String, JsonObject> getContent(Reader config_file_content) throws IOException {
			ArrayList<String> file_lines = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(config_file_content)) {
				String line;
				while ((line = br.readLine()) != null) {
					file_lines.add(line);
				}
			}
			config_file_content.close();
			
			/**
			 * Check tabs in Yaml files parsing
			 */
			ArrayList<Integer> error_lines = new ArrayList<>();
			for (int pos = 0; pos < file_lines.size(); pos++) {
				if (file_lines.get(pos).startsWith("\t")) {
					error_lines.add(pos);
				}
			}
			if (error_lines.isEmpty() == false) {
				throw new IOException("Invalid YAML file: detect some line(s) starting with tabs chars in " + error_lines);
			}
			
			HashMap<String, JsonObject> result = new HashMap<>(1);
			if (file_lines.isEmpty()) {
				return result;
			}
			
			Yaml y = new Yaml(new SafeConstructor());
			
			Gson g = new GsonBuilder().serializeNulls().create();
			
			JsonObject current_tree = new JsonObject();
			
			for (Object data : y.loadAll(String.join("\r\n", file_lines))) {
				GsonKit.jsonMerge(current_tree, g.toJsonTree(data), KeyValueNullContentMergeBehavior.KEEP);
			}
			
			for (Map.Entry<String, JsonElement> entry : current_tree.entrySet()) {
				if (entry.getValue().isJsonObject() == false) {
					throw new IOException("Invalid Yaml content (needs a data tree) for key " + entry.getKey() + ": " + entry.getValue().toString());
				}
				result.put(entry.getKey(), entry.getValue().getAsJsonObject());
			}
			
			return result;
		}
		
	},
	
	/**
	 * Format:
	 * {"mnemonic": {"variable": value, ...}, ...}
	 * - mnemonic can be a class name or a short name.
	 * - value can be a simple json primitive compatible (String, number, boolean) or a complex json data.
	 * Only one json document in a json file.
	 */
	JSON {
		public List<String> getExtentions() {
			return Arrays.asList(".json");
		}
		
		public HashMap<String, JsonObject> getContent(File config_file) throws IOException {
			try {
				return getContent(new FileReader(config_file));
			} catch (Exception e) {
				throw new IOException("Invalid Json file: " + config_file);
			}
		}
		
		HashMap<String, JsonObject> getContent(Reader config_file_content) throws IOException {
			JsonElement root = GsonKit.parser.parse(config_file_content);
			config_file_content.close();
			
			HashMap<String, JsonObject> result = new HashMap<>(1);
			
			if (root.isJsonObject() == false) {
				throw new IOException("Invalid Json content: document must be a Json Object");
			}
			
			JsonObject jo_root = root.getAsJsonObject();
			
			for (Map.Entry<String, JsonElement> entry : jo_root.entrySet()) {
				if (entry.getValue().isJsonObject() == false) {
					throw new IOException("Invalid Json content (needs a data tree) for key " + entry.getKey() + ": " + entry.getValue().toString());
				}
				result.put(entry.getKey(), entry.getValue().getAsJsonObject());
			}
			
			return result;
		}
		
	},
	
	/**
	 * Format:
	 * [mnemonic]
	 * variable=value
	 * variable = value
	 * variable: value
	 * ...
	 * - mnemonic can be a class name or a short name.
	 * - value can be a simple json primitive compatible (String, number, boolean) or a complex json data.
	 */
	INI {
		public List<String> getExtentions() {
			return Arrays.asList(".ini", ".conf");
		}
		
		public HashMap<String, JsonObject> getContent(File config_file) throws IOException {
			try {
				return getContent(new FileReader(config_file));
			} catch (Exception e) {
				throw new IOException("Invalid INI file: " + config_file);
			}
		}
		
		HashMap<String, JsonObject> getContent(Reader config_file_content) throws IOException {
			HierarchicalINIConfiguration ini_content = new HierarchicalINIConfiguration();
			try {
				ini_content.load(config_file_content);
			} catch (ConfigurationException e) {
				throw new IOException(e);
			}
			config_file_content.close();
			
			Set<String> sections = ini_content.getSections();
			HashMap<String, JsonObject> result = new HashMap<>(sections.size() + 1);
			
			sections.stream().forEach(section_name -> {
				SubnodeConfiguration section_content = ini_content.getSection(section_name);
				
				if (result.containsKey(section_name) == false) {
					result.put(section_name, new JsonObject());
				}
				
				section_content.getKeys().forEachRemaining(key -> {
					result.get(section_name).add(key, GsonKit.parser.parse(section_content.getString(key)));
				});
			});
			
			return result;
		}
		
	},
	
	/**
	 * Format: mnemonic.variable -> value
	 * - mnemonic can be a class name or a short name.
	 * - value can be a simple json primitive compatible (String, number, boolean) or a complex json data.
	 */
	PROPERTY {
		public List<String> getExtentions() {
			return Arrays.asList(".properties", ".property");
		}
		
		public HashMap<String, JsonObject> getContent(File config_file) throws IOException {
			try {
				return getContent(new FileReader(config_file));
			} catch (Exception e) {
				throw new IOException("Invalid Properties file: " + config_file);
			}
		}
		
		HashMap<String, JsonObject> getContent(Reader config_file_content) throws IOException {
			Properties p = new Properties();
			p.load(config_file_content);
			config_file_content.close();
			
			HashMap<String, JsonObject> result = new HashMap<>(p.size() + 1);
			
			p.forEach((k, v) -> {
				String raw_key = (String) k;
				
				int dot_pos = raw_key.lastIndexOf(".");
				if (dot_pos == -1 | dot_pos == 0 | dot_pos + 1 == raw_key.length()) {
					throw new RuntimeException("Invalid syntax: can't found separate dot in " + raw_key);
				}
				
				String result_key = raw_key.substring(0, dot_pos);
				String variable_name = raw_key.substring(dot_pos + 1);
				
				if (result.containsKey(result_key) == false) {
					result.put(result_key, new JsonObject());
				}
				result.get(result_key).add(variable_name, GsonKit.parser.parse((String) v));
			});
			
			return result;
		}
		
	};
	
	public abstract List<String> getExtentions();
	
	/**
	 * @return class name -> conf tree
	 */
	public abstract HashMap<String, JsonObject> getContent(File config_file) throws IOException;
	
	/**
	 * @return class name -> conf tree
	 */
	abstract HashMap<String, JsonObject> getContent(Reader config_file_content) throws IOException;
	
	public static ConfigurationFileType getTypeByFilename(File conf_file) {
		String ext = "." + FilenameUtils.getExtension(conf_file.getPath()).toLowerCase();
		
		return Arrays.asList(ConfigurationFileType.values()).stream().filter(type -> {
			return type.getExtentions().contains(ext);
		}).findFirst().orElse(null);
	}
	
	static final String[] CONFIG_FILE_EXTENTIONS;
	
	static {
		List<String> all_extensions = Arrays.asList(ConfigurationFileType.values()).stream().flatMap(f_type -> {
			return f_type.getExtentions().stream();
		}).collect(Collectors.toList());
		CONFIG_FILE_EXTENTIONS = all_extensions.toArray(new String[0]);
	}
	
	HashMap<String, JsonObject> getContent(Consumer<PrintWriter> source) throws IOException {
		StringWriter strOut = new StringWriter();
		source.accept(new PrintWriter(strOut));
		return this.getContent(new StringReader(strOut.toString()));
	}
	
}
