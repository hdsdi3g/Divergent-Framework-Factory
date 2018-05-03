/*
 * This file is part of MyDMAM.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package tv.hd3g.divergentframework.factory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.gson.JsonPrimitive;

import tv.hd3g.divergentframework.factory.js.JsToolkit;

/**
 * Create Objects and search Class
 */
public class Factory {
	private static Logger log = Logger.getLogger(Factory.class);
	
	private final ArrayList<File> classpath;
	private final HashMap<String, Class<?>> class_names;
	private final HashSet<String> absent_class_names;
	private final ConcurrentHashMap<Class<?>, Constructor<?>> class_constructor;
	private final Object lock;
	private final Properties bind_map;
	
	private JsToolkit js_toolkit;
	
	public Factory() {
		ArrayList<String> classpath_string = Lists.newArrayList(System.getProperty("java.class.path").split(System.getProperty("path.separator")));
		
		classpath = new ArrayList<>(classpath_string.size());
		classpath_string.forEach(cp -> {
			File f = new File(cp);
			try {
				classpath.add(f.getCanonicalFile());
			} catch (Exception e) {
				log.error("Can't access to classpath item: " + cp);
			}
		});
		
		class_names = new HashMap<>();
		absent_class_names = new HashSet<>();
		class_constructor = new ConcurrentHashMap<>();
		bind_map = new Properties();
		lock = new Object();
	}
	
	/**
	 * @return CanonicalFile and ExistsCanRead
	 */
	public ArrayList<File> getClasspath() {
		return classpath;
	}
	
	/**
	 * @return CanonicalFile and ExistsCanRead
	 */
	public Stream<File> getClasspathOnlyDirectories() {
		return getClasspath().stream().filter(cp -> {
			return cp.isDirectory();
		});
	}
	
	/**
	 * @return maybe null
	 */
	public Class<?> getClassByName(String class_name) {
		if (class_name == null) {
			return null;
		}
		if (absent_class_names.contains(class_name)) {
			return null;
		}
		if (class_names.containsKey(class_name)) {
			return class_names.get(class_name);
		}
		synchronized (lock) {
			try {
				Class<?> checked = Class.forName(class_name);
				class_names.put(class_name, checked);
				return checked;
			} catch (ClassNotFoundException e) {
				absent_class_names.add(class_name);
				return null;
			}
		}
	}
	
	public boolean isClassExists(String class_name) {
		if (class_name == null) {
			return false;
		}
		if (absent_class_names.contains(class_name)) {
			return false;
		}
		if (class_names.containsKey(class_name)) {
			return true;
		}
		return getClassByName(class_name) != null;
	}
	
	/**
	 * @see getBindMap to put Interface <-> java class/js file (with JsToolkit)
	 */
	public <T> T create(Class<T> from_class_or_interface) throws ReflectiveOperationException {
		// TODO4 get SingleInstance
		
		checkIsAccessibleClass(from_class_or_interface, true);
		
		Class<T> from_class = from_class_or_interface;
		if (from_class_or_interface.isInterface()) {
			String bind_to = bind_map.getProperty(from_class_or_interface.getName());
			
			if (bind_to == null) {
				throw new ClassNotFoundException("Interface " + from_class_or_interface + " is not binded to a class or JS file. Can't instance a simple Interface.");
			}
			
			try {
				File source_file = new File(bind_to);
				if (source_file.exists() && source_file.canRead() && source_file.isFile()) {
					return getJsToolkit().instanceTypeFromJs(from_class_or_interface, source_file, true);
				} else {
					try {
						URL url = new URL(bind_to);
						return getJsToolkit().instanceTypeFromJs(from_class_or_interface, url.openStream(), url.toString(), true);
					} catch (MalformedURLException e) {
					}
				}
			} catch (IOException | ScriptException e) {
				throw new ReflectiveOperationException("Can't instance Interface " + from_class_or_interface.getName(), e);
			}
			
			if (isClassExists(bind_to) == false) {
				throw new ClassNotFoundException("Interface " + from_class_or_interface + " is badly binded to \"" + bind_to + "\". Only JS File, JS from URL and simple class are valid");
			}
			
			Class<?> implementation_candidate = getClassByName(bind_to);
			
			if (from_class_or_interface.isAssignableFrom(implementation_candidate) == false) {
				throw new ReflectiveOperationException("Class " + implementation_candidate + " is not assignable to Interface " + from_class_or_interface);
			}
			
			@SuppressWarnings("unchecked")
			Class<T> checked_class = (Class<T>) implementation_candidate;
			from_class = checked_class;
		}
		
		Constructor<?> constructor = class_constructor.computeIfAbsent(from_class, cl -> {
			Optional<Constructor<?>> o_result = Arrays.asList(cl.getConstructors()).stream().filter(c -> {
				return c.canAccess(null) && c.getParameterCount() == 0 && c.isVarArgs() == false;
			}).findFirst();
			
			if (o_result.isPresent()) {
				return o_result.get();
			}
			
			return null;
		});
		
		if (constructor == null) {
			return from_class.getDeclaredConstructor().newInstance();
		}
		
		@SuppressWarnings("unchecked")
		T result = (T) constructor.newInstance();
		
		if (false) {
			// TODO3 check if this *class* is configured >> ConfigurationUtility
		}
		
		// TODO4 add to SingleInstance if needed
		
		return result;
	}
	
	// TODO4 set ConfigurationUtility
	
	public <T> T create(String class_name, Class<T> type) throws ReflectiveOperationException {
		if (class_name == null) {
			throw new NullPointerException("\"class_name\" can't to be null");
		}
		if (type == null) {
			throw new NullPointerException("\"type\" can't to be null");
		}
		
		Class<?> candidate = getClassByName(class_name);
		if (candidate == null) {
			return null;
		}
		try {
			@SuppressWarnings("unchecked")
			T result = (T) create(candidate);
			return result;
		} catch (LinkageError le) {
			throw new ReflectiveOperationException("Invalid class " + type.getName(), le);
		} catch (ClassCastException cce) {
			throw new ReflectiveOperationException("Try to cast to " + type.getName(), cce);
		}
	}
	
	/**
	 * @throws ClassNotFoundException if null, anonymous, local, member (or static if can_to_be_static).
	 */
	public void checkIsAccessibleClass(Class<?> context, boolean can_to_be_static) throws ClassNotFoundException {
		if (context == null) {
			throw new ClassNotFoundException("\"context\" can't to be null");
		}
		if (context.getClass().isAnonymousClass()) {
			throw new ClassNotFoundException("\"context\" can't to be an anonymous class");
		}
		if (context.getClass().isLocalClass()) {
			throw new ClassNotFoundException("\"context\" can't to be a local class");
		}
		if (context.getClass().isMemberClass()) {
			throw new ClassNotFoundException("\"context\" can't to be a member class");
		}
		if (can_to_be_static == false) {
			if (Modifier.isStatic(context.getClass().getModifiers())) {
				throw new ClassNotFoundException("\"context\" can't to be a static class");
			}
		}
	}
	
	public List<Class<?>> getAllClassesFromPackage(String package_name) throws ClassNotFoundException {
		try {
			ClassPath cp = ClassPath.from(ClassLoader.getSystemClassLoader());
			ImmutableSet<ClassInfo> packg = cp.getTopLevelClasses(package_name);
			return packg.stream().map(cl -> {
				try {
					return Class.forName(cl.getName());
				} catch (Exception e) {
					log.error("Can't load class " + cl.getName(), e);
					return null;
				}
			}).filter(cl -> {
				return cl != null;
			}).collect(Collectors.toList());
		} catch (IOException e) {
			throw new ClassNotFoundException("Can't search in classpath", e);
		}
	}
	
	public GsonKit createGsonKit() {
		GsonKit g_kit = new GsonKit();
		
		g_kit.registerGsonSimpleDeSerializer(Class.class, Class.class, src -> {
			if (src == null) {
				return null;
			}
			return new JsonPrimitive(src.getName());
		}, json -> {
			try {
				return getClassByName(json.getAsString());
			} catch (Exception e) {
				return null;
			}
		});
		g_kit.rebuildGsonSimple();
		
		return g_kit;
	}
	
	/**
	 * @return js_toolkit common on this Factory instance.
	 */
	public JsToolkit getJsToolkit() {
		if (js_toolkit == null) {
			synchronized (lock) {
				js_toolkit = new JsToolkit();
			}
		}
		return js_toolkit;
	}
	
	/**
	 * Formats:
	 * - package.Interface=package.ClassImplementInterface (simple class)
	 * - package.Interface=/Path/To/file.js (file)
	 * - package.Interface=file:/Path/To/file.js (URL)
	 */
	public Properties getBindMap() {
		return bind_map;
	}
	
	// TODO4 During the creation with Factory, if a field has a class type @SingleInstance, do a dynamic Injection & configure it, before current class conf.
	
}
