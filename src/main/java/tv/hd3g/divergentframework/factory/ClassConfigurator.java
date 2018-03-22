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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import tv.hd3g.divergentframework.factory.annotations.ConfigurableValidator;
import tv.hd3g.divergentframework.factory.annotations.OnAfterInjectConfiguration;
import tv.hd3g.divergentframework.factory.annotations.OnAfterUpdateConfiguration;
import tv.hd3g.divergentframework.factory.annotations.OnBeforeRemovedInConfiguration;
import tv.hd3g.divergentframework.factory.annotations.OnBeforeUpdateConfiguration;
import tv.hd3g.divergentframework.factory.annotations.TargetGenericClassType;
import tv.hd3g.divergentframework.factory.validation.DefaultValidator;

class ClassConfigurator {
	private static Logger log = Logger.getLogger(ClassConfigurator.class);
	
	private final ConcurrentHashMap<Class<?>, ClassDefinition> class_definitions;
	
	private final GsonKit gson_kit;
	private final Function<Class<?>, Object> instanceNewObjectFromClass;
	
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
	
	class ClassDefinition {
		
		private final Class<?> target_class;
		private final HashMap<String, FieldDefinition> field_definitions;
		
		private final List<Method> allCallbacksOnAfterInjectConfiguration;
		private final List<Method> allCallbacksOnBeforeRemovedInConfiguration;
		private final List<Method> allCallbacksOnAfterUpdateConfiguration;
		private final List<Method> allCallbacksOnBeforeUpdateConfiguration;
		
		private ClassDefinition(Class<?> target_class) {
			this.target_class = target_class;
			if (target_class == null) {
				throw new NullPointerException("\"target_class\" can't to be null");
			}
			
			if (isClassIsBlacklisted(target_class)) {
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
				field_definitions.put(f.getName(), new FieldDefinition(f));
			});
			
			List<Method> all_methods = Stream.concat(Arrays.asList(target_class.getDeclaredMethods()).stream(), Arrays.asList(target_class.getMethods()).stream()).distinct().filter(m -> {
				return Modifier.isStatic(m.getModifiers()) == false;
			}).filter(m -> {
				return Modifier.isNative(m.getModifiers()) == false;
			}).filter(m -> {
				return m.trySetAccessible();
			}).collect(Collectors.toList());
			
			Predicate<Method> annotationOnAfterInjectConfiguration = m -> m.getAnnotation(OnAfterInjectConfiguration.class) != null;
			Predicate<Method> annotationOnBeforeRemovedInConfiguration = m -> m.getAnnotation(OnBeforeRemovedInConfiguration.class) != null;
			Predicate<Method> annotationOnAfterUpdateConfiguration = m -> m.getAnnotation(OnAfterUpdateConfiguration.class) != null;
			Predicate<Method> annotationOnBeforeUpdateConfiguration = m -> m.getAnnotation(OnBeforeUpdateConfiguration.class) != null;
			Predicate<Method> allAnnotations = annotationOnAfterInjectConfiguration.or(annotationOnBeforeRemovedInConfiguration).or(annotationOnAfterUpdateConfiguration).or(annotationOnBeforeUpdateConfiguration);
			
			Predicate<Method> parameterCountNotNull = m -> m.getParameterCount() > 0;
			all_methods.stream().filter(parameterCountNotNull).filter(allAnnotations).forEach(m -> {
				log.error("Can't apply a configuration annotation in a method with some parameter(s), on method " + m.getName() + " in " + target_class);
			});
			if (all_methods.stream().filter(parameterCountNotNull).anyMatch(allAnnotations)) {
				throw new RuntimeException("Invalid method(s) callback annotation definition for " + target_class);
			}
			
			allCallbacksOnAfterInjectConfiguration = all_methods.stream().filter(annotationOnAfterInjectConfiguration).collect(Collectors.toList());
			allCallbacksOnBeforeRemovedInConfiguration = all_methods.stream().filter(annotationOnBeforeRemovedInConfiguration).collect(Collectors.toList());
			allCallbacksOnAfterUpdateConfiguration = all_methods.stream().filter(annotationOnAfterUpdateConfiguration).collect(Collectors.toList());
			allCallbacksOnBeforeUpdateConfiguration = all_methods.stream().filter(annotationOnBeforeUpdateConfiguration).collect(Collectors.toList());
		}
		
		/**
		 * Only update instance fields declared in configuration_tree.
		 * Don't callback class annotations.
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
				
				try {
					f_def.setValue(instance, field_conf);
				} catch (JsonSyntaxException | IllegalAccessException e) {
					throw new RuntimeException("Can't configure " + f_def.field.getName() + " in " + target_class.getSimpleName() + " configured with " + field_conf.toString(), e);
				}
			});
			return this;
		}
		
		private ClassDefinition callbackOnAfterInjectConfiguration(Object instance) {
			allCallbacksOnAfterInjectConfiguration.stream().forEach(m -> {
				try {
					m.invoke(instance);
				} catch (Exception e) {
					log.error("Can't callback " + instance.getClass().getName() + "." + m.getName() + "()", e);
				}
			});
			return this;
		}
		
		private ClassDefinition callbackOnBeforeRemovedInConfiguration(Object instance) {
			allCallbacksOnBeforeRemovedInConfiguration.stream().forEach(m -> {
				try {
					m.invoke(instance);
				} catch (Exception e) {
					log.error("Can't callback " + instance.getClass().getName() + "." + m.getName() + "()", e);
				}
			});
			return this;
		}
		
		private ClassDefinition callbackOnAfterUpdateConfiguration(Object instance) {
			allCallbacksOnAfterUpdateConfiguration.stream().forEach(m -> {
				try {
					m.invoke(instance);
				} catch (Exception e) {
					log.error("Can't callback " + instance.getClass().getName() + "." + m.getName() + "()", e);
				}
			});
			return this;
		}
		
		private ClassDefinition callbackOnBeforeUpdateConfiguration(Object instance) {
			allCallbacksOnBeforeUpdateConfiguration.stream().forEach(m -> {
				try {
					m.invoke(instance);
				} catch (Exception e) {
					log.error("Can't callback " + instance.getClass().getName() + "." + m.getName() + "()", e);
				}
			});
			return this;
		}
		
		class FieldDefinition {
			final Field field;
			final Class<?> type;
			Class<?> target_generic_class_type;
			final List<Class<? extends DefaultValidator>> validators;
			
			private FieldDefinition(Field field) {
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
			private void setValue(Object main_object_instance, JsonElement value) throws JsonSyntaxException, IllegalArgumentException, IllegalAccessException {
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
									log.warn("Please don't use null values in a Json array");
									continue;
								}
								Object newer_object = createNewSubItemInGenericObject(ja_value.get(pos));
								
								if (current_list.size() > pos) {
									log.info("INLIST: " + pos + " " + newer_object + " " + current_list.get(pos));
									if (current_list.get(pos).equals(newer_object) == false) {
										callbackUpdateAPIForRemovedObject(target_generic_class_type, current_list.get(pos));
										current_list.set(pos, newer_object);
										log.info("RESET: " + pos + " " + newer_object + " " + current_list.get(pos));
									}
								} else {
									log.info("ADD: " + pos + " " + newer_object + " ");
									current_list.add(newer_object);
								}
							}
							
							for (int pos = ja_value.size() - 1; pos < current_list.size(); pos++) {
								log.info("REMOVE: " + pos);
								current_list.remove(pos);
							}
							
							field.set(main_object_instance, current_list);
						} else if (value.isJsonPrimitive()) {
							Object new_item = createNewSubItemInGenericObject(value);
							
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
										current_map.put(entry.getKey(), gson_kit.getGson().fromJson(sub_item, target_generic_class_type));
									}
								} else {
									current_map.put(entry.getKey(), createNewSubItemInGenericObject(sub_item));
								}
							});
							
							field.set(main_object_instance, current_map);
						}
						return;
					}
				}
				
				if (current_value != null) {
					callbackUpdateAPIForRemovedObject(type, current_value);
				}
				
				field.set(main_object_instance, gson_kit.getGson().fromJson(value, type));
			}
			
			private Object createNewSubItemInGenericObject(JsonElement value) {
				if (value.isJsonArray()) {
					log.warn("Please don't put an json array in a json generic <" + target_generic_class_type + "> " + value + " in " + toString());
					return gson_kit.getGson().fromJson(value, target_generic_class_type);
				} else if (value.isJsonObject()) {
					Object new_item = instanceNewObjectFromClass.apply(target_generic_class_type);
					configureNewObjectWithJson(target_generic_class_type, new_item, value.getAsJsonObject());
					return new_item;
				} else if (value.isJsonPrimitive()) {
					return gson_kit.getGson().fromJson(value, target_generic_class_type);
				}
				throw new NullPointerException("\"value\" can't to be a null json");
			}
			
			public String toString() {
				return "field " + field.getName() + " (" + type.getSimpleName() + ") in " + target_class.getSimpleName();
			}
			
			boolean checkValidators(JsonElement value) {
				return validators.stream().map(v -> {
					return (DefaultValidator) instanceNewObjectFromClass.apply(v);
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
	}
	
	private ClassDefinition getFrom(Class<?> from_type) {
		return class_definitions.computeIfAbsent(from_type, type -> {
			return new ClassDefinition(type);
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
		getFrom(from_type).setObjectConfiguration(new_created_instance, configuration).callbackOnAfterInjectConfiguration(new_created_instance);
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
		getFrom(from_type).callbackOnBeforeUpdateConfiguration(instance_to_update).setObjectConfiguration(instance_to_update, new_configuration).callbackOnAfterUpdateConfiguration(instance_to_update);
	}
	
	private void callbackUpdateAPIForRemovedObject(Class<?> from_type, Object removed_instance_to_callback) {
		if (isClassIsBlacklisted(from_type)) {
			log.debug("Can't configure " + from_type + " (internaly blacklisted)");
			return;
		}
		log.debug("Remove " + from_type);
		getFrom(from_type).callbackOnBeforeRemovedInConfiguration(removed_instance_to_callback);
	}
	
}
