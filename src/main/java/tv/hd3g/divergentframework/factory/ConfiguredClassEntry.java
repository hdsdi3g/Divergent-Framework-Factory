/*
 * This file is part of Divergent-Framework-Factory.
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import tv.hd3g.divergentframework.factory.annotations.ConfigurableValidator;
import tv.hd3g.divergentframework.factory.annotations.TargetGenericClassType;
import tv.hd3g.divergentframework.factory.validation.DefaultValidator;

abstract class ConfiguredClassEntry<T> {// TODO split ConfiguredClassEntry in 2: Class<->Field with conf (related to Factory), and Global Conf<->Class<->created objects (Related to Conf utility)
	// TODO remove Generic
	
	private static Logger log = Logger.getLogger(ConfiguredClassEntry.class);
	
	private final Gson gson;
	
	private final Class<T> target_class;
	
	@Deprecated
	private volatile JsonObject actual_class_configuration;
	
	@Deprecated
	private final ArrayList<T> created_instances;
	
	private final HashMap<String, FieldDefinition> field_definitions;
	
	private class FieldDefinition {
		final Field field;
		final Class<?> type;
		Class<?> target_generic_class_type;
		final List<Class<? extends DefaultValidator>> validators;
		
		FieldDefinition(Field field) {
			this.field = field;
			if (field == null) {
				throw new NullPointerException("\"field\" can't to be null");
			}
			type = field.getType();
			
			if (type.isAssignableFrom(LinkedHashMap.class) | type.isAssignableFrom(ArrayList.class)) {
				TargetGenericClassType tgct = field.getAnnotation(TargetGenericClassType.class);
				if (tgct == null) {
					log.warn("Missing @" + TargetGenericClassType.class.getSimpleName() + " annotation in class " + target_class.getName() + " for field " + field.getName());
				} else {
					target_generic_class_type = tgct.value();
				}
			}
			
			validators = Arrays.asList(field.getAnnotationsByType(ConfigurableValidator.class)).stream().map(c_v -> {
				return c_v.value();
			}).collect(Collectors.toList());
		}
		
		@SuppressWarnings("unchecked")
		void setValue(Object main_object_instance, JsonElement value) throws JsonSyntaxException, IllegalArgumentException, IllegalAccessException {// TODO callback "after inject/update" ?
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
						
						for (int pos = 0; pos < ja_value.size(); pos++) {
							if (ja_value.get(pos).isJsonNull()) {
								continue;
							}
							Object newer_object = createNewSubItem(ja_value.get(pos));
							
							if (current_list.size() > pos) {
								if (current_list.get(pos).equals(newer_object) == false) {
									callbackUpdateAPIForRemovedObject(target_generic_class_type, current_list.get(pos));
									current_list.set(pos, newer_object);
								}
							} else {
								current_list.add(newer_object);
							}
						}
						field.set(main_object_instance, current_list);
					} else if (value.isJsonPrimitive()) {
						Object new_item = createNewSubItem(value);
						
						current_list.removeIf(item -> {
							if (item.equals(new_item) == false) {
								callbackUpdateAPIForRemovedObject(target_generic_class_type, item);
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
						throw new JsonSyntaxException("Can't mergue list with a json Object for " + toString());
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
						throw new JsonSyntaxException("Can't mergue map with a json Array for " + toString());
					} else if (value.isJsonPrimitive()) {
						throw new JsonSyntaxException("Can't mergue map with a json Primitive for " + toString());
					} else if (value.isJsonNull()) {
						field.set(main_object_instance, null);
					} else if (value.isJsonObject()) {
						JsonObject jo_value = value.getAsJsonObject();
						
						current_map.keySet().removeIf(key -> {
							if (jo_value.has((String) key) == false) {
								callbackUpdateAPIForRemovedObject(target_generic_class_type, current_map.get(key));
								return true;
							}
							return false;
						});
						
						jo_value.entrySet().forEach(entry -> {
							JsonElement sub_item = entry.getValue();
							if (current_map.containsKey(entry.getKey())) {
								if (sub_item.isJsonObject()) {
									reconfigureActualObjectWithJson(target_generic_class_type, current_map.get(entry.getKey()), sub_item.getAsJsonObject());
								} else if (sub_item.isJsonNull()) {
									/**
									 * Don't put null item in map.
									 */
								} else if (sub_item.isJsonPrimitive() | sub_item.isJsonArray()) {
									current_map.put(entry.getKey(), gson.fromJson(sub_item, target_generic_class_type));
								}
							} else {
								current_map.put(entry.getKey(), createNewSubItem(sub_item));
							}
						});
						
						field.set(main_object_instance, current_map);
					}
					return;
				}
			}
			
			if (current_value != null) {
				callbackUpdateAPIForRemovedObject(target_generic_class_type, current_value);
			}
			
			field.set(main_object_instance, gson.fromJson(value, type));
		}
		
		private Object createNewSubItem(JsonElement value) {
			try {
				Object new_item = instanceNewObjectFromClass(target_generic_class_type);
				configureNewObjectWithJson(target_generic_class_type, new_item, value);
				return new_item;
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Can't instantiate from " + target_generic_class_type, e);
			}
		}
		
		public String toString() {
			return "field " + field.getName() + " (" + type.getSimpleName() + ") in " + target_class.getSimpleName();
		}
		
		boolean checkValidators(JsonElement value) {
			return validators.stream().map(v -> {
				try {
					return (DefaultValidator) instanceNewObjectFromClass(v);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException("Can't instance " + v.getName(), e);
				}
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
	
	ConfiguredClassEntry(Gson gson, Class<T> target_class, JsonObject new_class_configuration) {
		this.gson = gson;
		if (gson == null) {
			throw new NullPointerException("\"gson\" can't to be null");
		}
		this.target_class = target_class;
		if (target_class == null) {
			throw new NullPointerException("\"target_class\" can't to be null");
		}
		
		created_instances = new ArrayList<>(1);
		
		field_definitions = new HashMap<>();
		
		Arrays.asList(target_class.getDeclaredFields()).stream().filter(f -> {
			return f.trySetAccessible();
		}).filter(f -> {
			return Modifier.isStatic(f.getModifiers()) == false;
		}).filter(f -> {
			return Modifier.isFinal(f.getModifiers()) == false;
		}).forEach(f -> {
			field_definitions.put(f.getName(), new FieldDefinition(f));
		});
		
		// TODO manage sub Objects: create, inject conf, callbacks
		
		// TODO checks some non-sense with actual_class_configuration json
		actual_class_configuration = new_class_configuration;
		
		/*try {
			String field_name = ;
			
		if (type.isAssignableFrom(LinkedHashMap.class)) {
			System.out.println("Ok, LinkedHashMap " + f.getGenericType());// XXX
		} else if (type.isAssignableFrom(ArrayList.class)) {
			TargetGenericClassType tgct = f.getAnnotation(TargetGenericClassType.class);
			if (tgct == null) {
				// Let Gson to get by
				f.set(car, g.getGson().fromJson(conf_tree.get(field_name), type));
			}
			
			System.out.println("Ok, ArrayList " + f.getGenericType());// XXX
		}
		} catch (JsonSyntaxException | IllegalArgumentException | IllegalAccessException e) {
		throw new RuntimeException(e);
		}*/
	}
	
	void setupInstance(T instance) {
		/*if (target_class.isAssignableFrom(instance.getClass()) == false) {
			throw new ClassCastException(instance.getClass().getName() + " is not assignable from " + target_class.getName());
		}*/
		// TODO4 call transformators (and create API)
		
		// TODO2 check annotations for sub var class
		
		// TODO2 get TargetGenericClassType class, warn if this var is not a generic...
		
		// TODO2 call validators: behavior if not validate ?
		
		// TODO2 instance_to_configure.onAfterInjectConfiguration();
		
		created_instances.add(instance);
	}
	
	void afterRemovedConf() {
		// TODO3 callback all instances with OnAfterRemoveConfiguration
	}
	
	void beforeUpdateInstances() {
		// TODO3 callback all instances
	}
	
	void afterUpdateInstances() {
		// TODO3 callback all instances
	}
	
	void updateInstances(JsonObject new_class_configuration) {
		// TODO3 checks some non-sense with new_class_configuration json
		
		actual_class_configuration = new_class_configuration;// TODO3 compute deltas ?
		
		created_instances.forEach(instance -> {
			// TODO3 configured_instance.onBeforeUpdateConfiguration();
			// TODO3 do update
			// TODO3 configured_instance.onAfterUpdateConfiguration();
			
			// TODO3 call OnBeforeRemovedInConfiguration
		});
	}
	
	Class<T> getTargetClass() {
		return target_class;
	}
	
	protected abstract Object instanceNewObjectFromClass(Class<?> from_type) throws ReflectiveOperationException;
	
	protected abstract void configureNewObjectWithJson(Class<?> from_type, Object new_created_instance, JsonElement configuration);
	
	protected abstract void reconfigureActualObjectWithJson(Class<?> from_type, Object instance_to_update, JsonObject new_configuration);
	
	protected abstract void callbackUpdateAPIForRemovedObject(Class<?> from_type, Object removed_instance_to_callback);
	
}
