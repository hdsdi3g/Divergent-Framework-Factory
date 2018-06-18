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
package tv.hd3g.divergentframework.factory.configuration;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.gson.JsonObject;

import tv.hd3g.divergentframework.factory.GsonKit;

/**
 * Manage created objects lifecycle with linked configuration
 */
class ClassConfigurator {
	private static Logger log = Logger.getLogger(ClassConfigurator.class);
	
	private final ConcurrentHashMap<Class<?>, ClassDefinition> class_definitions;
	
	final GsonKit gson_kit;
	final Function<Class<?>, Object> instanceNewObjectFromClass;
	
	ClassConfigurator(GsonKit gson_kit, Function<Class<?>, Object> instanceNewObjectFromClass) {
		this.gson_kit = gson_kit;
		if (gson_kit == null) {
			throw new NullPointerException("\"gson_kit\" can't to be null");
		}
		this.instanceNewObjectFromClass = instanceNewObjectFromClass;
		if (instanceNewObjectFromClass == null) {
			throw new NullPointerException("\"instanceNewObjectFromClass\" can't to be null");
		}
		
		class_definitions = new ConcurrentHashMap<>();
	}
	
	ClassDefinition getFrom(Class<?> from_type) {
		return class_definitions.computeIfAbsent(from_type, type -> {
			return new ClassDefinition(type, this);
		});
	}
	
	boolean isClassIsBlacklisted(Class<?> from_type) {
		if (from_type.isArray()) {
			throw new ClassCastException("Can't push configuration in an Array");
		} else if (from_type.isInterface()) {
			throw new ClassCastException("Can't push configuration in an Interface");
		} else if (from_type.isAnnotation()) {
			throw new ClassCastException("Can't push configuration in an Annotation");
		} else if (from_type.isAnonymousClass()) {
			throw new ClassCastException("Can't push configuration in an AnonymousClass");
		} else if (from_type.isSynthetic()) {
			throw new ClassCastException("Can't push configuration in a Synthetic");
			// } else if (from_type.isMemberClass()) {
			// throw new ClassCastException("Can't push configuration in a MemberClass");
		} else if (from_type.isLocalClass()) {
			throw new ClassCastException("Can't push configuration in a LocalClass");
		} else if (Number.class.isAssignableFrom(from_type)) {
			return true;
		} else if (String.class.isAssignableFrom(from_type)) {
			return true;
		} else if (from_type.isPrimitive()) {
			return true;
		} else if (from_type.isEnum()) {
			return true;
		}
		
		return gson_kit.getAllSerializedClasses(false).anyMatch(type -> {
			return ((Class<?>) type).isAssignableFrom(from_type);
		});
	}
	
	/**
	 * @param configuration if empty, do nothing
	 */
	void configureNewObjectWithJson(Class<?> from_type, Object new_created_instance, JsonObject configuration) {
		if (isClassIsBlacklisted(from_type)) {
			log.debug("Can't configure " + from_type + " (internaly blacklisted)");
			return;
		} else if (configuration.size() == 0) {
			return;
		}
		log.debug("Configure " + from_type + " with " + configuration);
		getFrom(from_type).setObjectConfiguration(new_created_instance, configuration, MissingKeyBehavior.REMOVE).callbackOnAfterInjectConfiguration(new_created_instance);
	}
	
	/**
	 * @param new_configuration if empty, do nothing
	 */
	void reconfigureActualObjectWithJson(Class<?> from_type, Object instance_to_update, JsonObject new_configuration) {
		if (isClassIsBlacklisted(from_type)) {
			log.debug("Can't configure " + from_type + " (internaly blacklisted)");
			return;
		} else if (new_configuration.size() == 0) {
			return;
		}
		
		log.debug("Reconfigure " + from_type + " with " + new_configuration);
		getFrom(from_type).callbackOnBeforeUpdateConfiguration(instance_to_update).setObjectConfiguration(instance_to_update, new_configuration, MissingKeyBehavior.IGNORE).callbackOnAfterUpdateConfiguration(instance_to_update);
	}
	
	/**
	 * @param configuration if empty, do nothing
	 */
	void removeObjectConfiguration(Class<?> from_type, Object instance_to_callback) {
		if (isClassIsBlacklisted(from_type)) {
			log.debug("Can't configure " + from_type + " (internaly blacklisted)");
			return;
		}
		log.debug("Unconfigure " + from_type);
		getFrom(from_type).callbackOnBeforeRemovedInConfiguration(instance_to_callback);
	}
	
}
