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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import tv.hd3g.divergentframework.factory.GsonKit;
import tv.hd3g.divergentframework.factory.configuration.annotations.OnAfterInjectConfiguration;

class ClassDefinition {
	private static Logger log = Logger.getLogger(ClassDefinition.class);
	
	final Class<?> target_class;
	final ClassConfigurator class_configurator;
	final GsonKit gson_kit;
	
	private final HashMap<String, FieldDefinition> field_definitions;
	
	private final List<Method> allCallbacksOnAfterInjectConfiguration;
	
	ClassDefinition(Class<?> target_class, ClassConfigurator configurator) {
		this.target_class = target_class;
		if (target_class == null) {
			throw new NullPointerException("\"target_class\" can't to be null");
		}
		this.class_configurator = configurator;
		if (configurator == null) {
			throw new NullPointerException("\"configurator\" can't to be null");
		}
		this.gson_kit = configurator.gson_kit;
		
		if (configurator.isClassIsBlacklisted(target_class)) {
			throw new ClassCastException("Can't analyst blacklisted class " + target_class);
		}
		
		field_definitions = new HashMap<>();
		
		Arrays.asList(target_class.getDeclaredFields()).stream().filter(f -> {
			return Modifier.isStatic(f.getModifiers()) == false;
		}).filter(f -> {
			return Modifier.isFinal(f.getModifiers()) == false;
		}).filter(m -> {
			return m.trySetAccessible();
		}).forEach(f -> {
			field_definitions.put(f.getName(), new FieldDefinition(this, f));
		});
		
		/**
		 * Class.getDeclaredMethods() gets public, protected, package and private methods, but excludes inherited methods.
		 */
		Stream<Method> declared_methods = Arrays.asList(target_class.getDeclaredMethods()).stream();
		/**
		 * Class.getMethods gets inherited methods, but only the public ones.
		 */
		Stream<Method> methods = Arrays.asList(target_class.getMethods()).stream();
		
		List<Method> all_methods = Stream.concat(declared_methods, methods).distinct().filter(m -> {
			return Modifier.isStatic(m.getModifiers()) == false;
		}).filter(m -> {
			return Modifier.isNative(m.getModifiers()) == false;
		}).filter(m -> {
			return m.trySetAccessible();
		}).collect(Collectors.toList());
		
		Predicate<Method> annotationOnAfterInjectConfiguration = m -> m.getAnnotation(OnAfterInjectConfiguration.class) != null;
		
		Predicate<Method> parameterCountNotNull = m -> m.getParameterCount() > 0;
		all_methods.stream().filter(parameterCountNotNull).filter(annotationOnAfterInjectConfiguration).forEach(m -> {
			log.error("Can't apply a configuration annotation in a method with some parameter(s), on method " + m.getName() + " in " + target_class);
		});
		if (all_methods.stream().filter(parameterCountNotNull).anyMatch(annotationOnAfterInjectConfiguration)) {
			throw new RuntimeException("Invalid method(s) callback annotation definition for " + target_class);
		}
		
		allCallbacksOnAfterInjectConfiguration = all_methods.stream().filter(annotationOnAfterInjectConfiguration).collect(Collectors.toList());
	}
	
	/**
	 * Only update instance fields declared in configuration_tree.
	 * Don't callback class annotations.
	 * @param previous_configuration can be null
	 */
	public ClassDefinition setObjectConfiguration(Object instance, JsonObject configuration_tree) {
		if (target_class.isInstance(instance) == false) {
			throw new ClassCastException("Invalid class type between " + target_class + " and object instance " + instance.getClass());
		}
		
		configuration_tree.entrySet().forEach(entry -> {
			String field_name = entry.getKey();
			JsonElement field_conf = entry.getValue();
			
			if (field_definitions.containsKey(field_name) == false) {
				log.warn("Can't found var name " + field_name + " in " + target_class.getSimpleName() + " configured with " + field_conf.toString());
				return;
			}
			
			FieldDefinition f_def = field_definitions.get(field_name);
			if (f_def.checkValidators(field_conf) == false) {
				throw new IllegalArgumentException("Validation error for var name " + field_name + " in " + target_class.getSimpleName() + " configured with " + field_conf.toString());
			}
			
			if (log.isTraceEnabled()) {
				log.trace("Push conf for a " + target_class + " class, MissingKeyBehavior: " + target_class);
			}
			try {
				f_def.setValue(instance, field_conf);
			} catch (JsonSyntaxException | IllegalAccessException e) {
				throw new RuntimeException("Can't configure " + f_def.field.getName() + " in " + target_class.getSimpleName() + " configured with " + field_conf.toString(), e);
			}
		});
		return this;
	}
	
	ClassDefinition callbackOnAfterInjectConfiguration(Object instance) {
		allCallbacksOnAfterInjectConfiguration.stream().forEach(m -> {
			try {
				m.invoke(instance);
			} catch (Exception e) {
				log.error("Can't callback " + instance.getClass().getName() + "." + m.getName() + "()", e);
			}
		});
		return this;
	}
	
}
