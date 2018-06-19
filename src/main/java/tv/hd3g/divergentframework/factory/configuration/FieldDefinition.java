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

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import tv.hd3g.divergentframework.factory.GsonKit;
import tv.hd3g.divergentframework.factory.configuration.annotations.ConfigurableValidator;
import tv.hd3g.divergentframework.factory.configuration.annotations.TargetGenericClassType;
import tv.hd3g.divergentframework.factory.configuration.validation.DefaultValidator;

class FieldDefinition {
	private static Logger log = Logger.getLogger(FieldDefinition.class);
	
	private final ClassDefinition class_definition;
	final Field field;
	final GsonKit gson_kit;
	
	final Class<?> type;
	Class<?> target_generic_class_type;
	final List<Class<? extends DefaultValidator>> validators;
	
	FieldDefinition(ClassDefinition class_definition, Field field) {
		this.class_definition = class_definition;
		if (class_definition == null) {
			throw new NullPointerException("\"class_definition\" can't to be null");
		}
		gson_kit = class_definition.gson_kit;
		
		this.field = field;
		if (field == null) {
			throw new NullPointerException("\"field\" can't to be null");
		}
		type = field.getType();
		
		if (type.isAssignableFrom(LinkedHashMap.class) | type.isAssignableFrom(ArrayList.class)) {
			TargetGenericClassType tgct = field.getAnnotation(TargetGenericClassType.class);
			if (tgct == null) {
				log.warn("Missing @" + TargetGenericClassType.class.getSimpleName() + " annotation in class " + class_definition.target_class.getName() + " for field " + field.getName());
			} else {
				target_generic_class_type = tgct.value();
			}
		}
		
		validators = Arrays.asList(field.getAnnotationsByType(ConfigurableValidator.class)).stream().map(c_v -> {
			return c_v.value();
		}).collect(Collectors.toList());
	}
	
	@SuppressWarnings("unchecked")
	void setValue(Object main_object_instance, JsonElement value) throws JsonSyntaxException, IllegalArgumentException, IllegalAccessException {
		Object current_value = field.get(main_object_instance);
		
		if (target_generic_class_type != null) {
			/**
			 * It's a valid generic
			 */
			if (type.isAssignableFrom(ArrayList.class)) {
				/**
				 * It's a list: let's do an intelligent update.
				 * Get current list content
				 */
				@SuppressWarnings("rawtypes")
				ArrayList current_list = new ArrayList();
				if (current_value != null) {
					current_list.addAll((Collection<?>) current_value);
				}
				
				if (value.isJsonArray()) {
					JsonArray ja_value = value.getAsJsonArray();
					
					ArrayList<Object> newer_list = new ArrayList<>();
					newer_list.ensureCapacity(ja_value.size());
					
					/**
					 * Prepare comparison list
					 */
					for (int pos = 0; pos < ja_value.size(); pos++) {
						if (ja_value.get(pos).isJsonNull()) {
							log.warn("Please don't use null values in a Json array");
							continue;
						}
						newer_list.add(createNewSubItemInGenericObject(ja_value.get(pos)));
					}
					
					for (int pos = 0; pos < newer_list.size(); pos++) {
						Object newer_object = newer_list.get(pos);
						
						Object current_object = null;
						if (pos < current_list.size()) {
							current_object = current_list.get(pos);
							
							if (newer_object.equals(current_object) == false) {
								current_list.set(pos, newer_object);
							} else {
								/**
								 * Same Object
								 */
							}
						} else {
							current_list.add(newer_object);
						}
					}
					
					for (int pos = newer_list.size(); pos < current_list.size(); pos++) {
						current_list.remove(pos);
					}
					
					field.set(main_object_instance, current_list);
				} else if (value.isJsonPrimitive()) {
					Object new_item = createNewSubItemInGenericObject(value);
					
					current_list.removeIf(item -> {
						if (item.equals(new_item) == false) {
							return true;
						}
						return false;
					});
					if (current_list.isEmpty()) {
						current_list.add(new_item);
					}
					
					field.set(main_object_instance, current_list);
				} else if (value.isJsonNull()) {
					field.set(main_object_instance, null);
				} else if (value.isJsonObject()) {
					throw new JsonSyntaxException("Can't merge list with a json Object for " + toString());
				}
				return;
			} else if (type.isAssignableFrom(LinkedHashMap.class)) {
				/**
				 * It's a map: let's do an intelligent update.
				 * Get current map content
				 */
				@SuppressWarnings("rawtypes")
				LinkedHashMap current_map = new LinkedHashMap();
				if (current_value != null) {
					current_map.putAll((Map<?, ?>) current_value);
				}
				
				if (value.isJsonArray()) {
					throw new JsonSyntaxException("Can't merge map with a json Array for " + toString());
				} else if (value.isJsonPrimitive()) {
					throw new JsonSyntaxException("Can't merge map with a json Primitive for " + toString());
				} else if (value.isJsonNull()) {
					field.set(main_object_instance, null);
				} else if (value.isJsonObject()) {
					JsonObject jo_value = value.getAsJsonObject();
					
					jo_value.entrySet().forEach(entry -> {
						current_map.put(entry.getKey(), createNewSubItemInGenericObject(entry.getValue()));
					});
					
					field.set(main_object_instance, current_map);
				}
				return;
			}
		}
		
		try {
			String trace_message = null;
			if (log.isTraceEnabled()) {
				/**
				 * Make trace message
				 */
				String[] package_names_tree = class_definition.target_class.getPackageName().split("\\.");
				String package_name = "";
				if (package_names_tree.length == 1) {
					package_name = package_names_tree[0] + ".";
				} else if (package_names_tree.length > 1) {
					package_name = "..." + package_names_tree[package_names_tree.length - 1] + ".";
				}
				String class_name = class_definition.target_class.getSimpleName();
				String value_content = StringUtils.abbreviate(value.toString(), 30);
				trace_message = "Field set " + package_name + class_name + "." + field.getName() + " (" + type.getSimpleName() + ") = " + value_content + " in <" + main_object_instance.toString() + ">";
			}
			
			if (type == JsonObject.class && value.isJsonObject() | type == JsonArray.class && value.isJsonArray() | type == JsonElement.class) {// TODO test json to json...
				/**
				 * Json to json...
				 */
				if (log.isTraceEnabled()) {
					log.trace(trace_message);
				}
				field.set(main_object_instance, value);
			} else if (value.isJsonNull()) {// TODO test json to json...
				if (log.isTraceEnabled()) {
					log.trace(trace_message);
				}
				field.set(main_object_instance, null);
			} else if (value.isJsonPrimitive()) {
				if (log.isTraceEnabled()) {
					log.trace(trace_message);
				}
				field.set(main_object_instance, gson_kit.getGson().fromJson(value, type));
			} else if (value.isJsonObject()) {
				/**
				 * We most to go deeper.
				 */
				Object item = class_definition.class_configurator.instanceNewObjectFromClass.apply(type);
				if (item == null) {
					throw new InaccessibleObjectException("Can't instance object type " + type.getName());
				}
				class_definition.class_configurator.configureNewObjectWithJson(type, item, value.getAsJsonObject());
				if (log.isTraceEnabled()) {
					log.trace(trace_message);
				}
				field.set(main_object_instance, item);
			} else {
				/**
				 * Other cases...
				 */
				if (log.isTraceEnabled()) {
					log.trace(trace_message);
				}
				field.set(main_object_instance, gson_kit.getGson().fromJson(value, type));
			}
		} catch (InaccessibleObjectException e) {
			if (value.isJsonObject()) {
				log.fatal("Can't inject configuration in class " + type.getName() + " with this vars: " + value.getAsJsonObject().keySet().stream().collect(Collectors.toList()));
			} else {
				log.fatal("Can't inject configuration in class " + type.getName() + " with this json: " + value);
			}
			throw e;
		}
	}
	
	private Object createNewSubItemInGenericObject(JsonElement value) {
		if (value.isJsonArray()) {
			log.warn("Please don't put an json array in a json generic <" + target_generic_class_type + "> " + value + " in " + toString());
			return gson_kit.getGson().fromJson(value, target_generic_class_type);
		} else if (value.isJsonObject()) {
			Object new_item = class_definition.class_configurator.instanceNewObjectFromClass.apply(target_generic_class_type);
			class_definition.class_configurator.configureNewObjectWithJson(target_generic_class_type, new_item, value.getAsJsonObject());
			return new_item;
		} else if (value.isJsonPrimitive()) {
			return gson_kit.getGson().fromJson(value, target_generic_class_type);
		}
		throw new NullPointerException("\"value\" can't to be a null json");
	}
	
	public String toString() {
		return "field " + field.getName() + " (" + type.getSimpleName() + ") in " + class_definition.target_class.getSimpleName();
	}
	
	boolean checkValidators(JsonElement value) {
		return validators.stream().map(v -> {
			return (DefaultValidator) class_definition.class_configurator.instanceNewObjectFromClass.apply(v);
		}).allMatch(validator -> {
			if (validator.getValidator().test(value) == false) {
				if (log.isTraceEnabled()) {
					log.trace("Don't pass validator " + validator.getClass().getSimpleName() + " for " + toString() + " with " + value.toString());
				}
				return false;
			}
			return true;
		});
	}
	
}
