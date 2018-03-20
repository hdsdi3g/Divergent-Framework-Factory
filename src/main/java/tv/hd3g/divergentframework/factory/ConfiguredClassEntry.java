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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import tv.hd3g.divergentframework.factory.annotations.ConfigurableValidator;
import tv.hd3g.divergentframework.factory.annotations.TargetGenericClassType;
import tv.hd3g.divergentframework.factory.validation.DefaultValidator;

class ConfiguredClassEntry<T> {
	private static Logger log = Logger.getLogger(ConfiguredClassEntry.class);
	
	private final Class<T> target_class;
	private volatile JsonObject actual_class_configuration;
	
	@Deprecated
	private final ArrayList<T> created_instances;// TODO need this here ?
	
	private final HashMap<String, FieldDefinition> field_definitions;
	
	private class FieldDefinition {
		final Field field;
		final Class<?> type;
		Class<?> target_generic_class_type;
		final List<Class<? extends DefaultValidator>> validators;
		
		public FieldDefinition(Field field) {
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
		
		/*boolean isMap() {
			return type.isAssignableFrom(LinkedHashMap.class) && target_generic_class_type != null;
		}
		
		boolean isList() {
			return type.isAssignableFrom(ArrayList.class) && target_generic_class_type != null;
		}
		
		boolean isEnum() {
			return false;
		}
		
		boolean isNumber() {
			return false;
		}
		
		boolean isBoolean() {
			return false;
		}
		
		boolean isString() {
			return false;
		}
		
		boolean isOtherObject() {
			return isMap() == false && isList() == false && isEnum() == false && isNumber() == false && isBoolean() == false && isString() == false;
		}*/
		
	}
	
	ConfiguredClassEntry(Class<T> target_class, JsonObject new_class_configuration) {
		this.target_class = target_class;
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
		} else {
			f.set(car, g.getGson().fromJson(conf_tree.get(field_name), type));
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
}
