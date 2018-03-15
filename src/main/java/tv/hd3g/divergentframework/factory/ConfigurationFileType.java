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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

enum ConfigurationFileType {
	
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
			Gson g = new Gson();
			
			JsonObject current_tree = new JsonObject();
			
			for (Object data : y.loadAll(String.join("\r\n", file_lines))) {
				GsonKit.jsonMergue(current_tree, g.toJsonTree(data));
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
	JSON {
		public List<String> getExtentions() {
			return Arrays.asList(".json");
		}
		
		public HashMap<String, JsonObject> getContent(File config_file) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		HashMap<String, JsonObject> getContent(Reader config_file_content) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}
		
	},
	INI {
		public List<String> getExtentions() {
			return Arrays.asList(".ini", ".conf");
		}
		
		public HashMap<String, JsonObject> getContent(File config_file) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		HashMap<String, JsonObject> getContent(Reader config_file_content) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}
		
	},
	PROPERTY {
		public List<String> getExtentions() {
			return Arrays.asList(".properties", ".property");
		}
		
		public HashMap<String, JsonObject> getContent(File config_file) throws IOException {
			// TODO Auto-generated method stub
			Properties p = new Properties();
			return null;
		}
		
		@Override
		HashMap<String, JsonObject> getContent(Reader config_file_content) throws IOException {
			// TODO Auto-generated method stub
			return null;
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
	
	private static Logger log = Logger.getLogger(ConfigurationFileType.class);
	
	static {
		List<String> all_extensions = Arrays.asList(ConfigurationFileType.values()).stream().flatMap(f_type -> {
			return f_type.getExtentions().stream();
		}).collect(Collectors.toList());
		CONFIG_FILE_EXTENTIONS = all_extensions.toArray(new String[0]);
	}
	
}
