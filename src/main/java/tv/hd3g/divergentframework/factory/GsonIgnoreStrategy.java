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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package tv.hd3g.divergentframework.factory;

import org.apache.log4j.Logger;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class GsonIgnoreStrategy implements ExclusionStrategy {
	
	private static Logger log = Logger.getLogger(GsonIgnoreStrategy.class);
	
	// private final ArrayList<Class<?>> forbidden_class;
	
	public GsonIgnoreStrategy() {
		// forbidden_class = new ArrayList<>();
		// forbidden_class.add(InputStream.class);
		// forbidden_class.add(OutputStream.class);
		// forbidden_class.add(Executor.class);
	}
	
	public boolean shouldSkipField(FieldAttributes f) {
		if (log.isTraceEnabled()) {
			log.trace("getName: " + f.getName() + ", getAnnotation: " + f.getAnnotation(GsonIgnore.class));
		}
		if (f.getAnnotation(GsonIgnore.class) != null) {
			return true;
		}
		
		/*Class<?> clazz = f.getDeclaredClass();
		
		return forbidden_class.stream().anyMatch(f_class -> {
			return f_class.isAssignableFrom(clazz);
		});*/
		return false;
	}
	
	public boolean shouldSkipClass(Class<?> clazz) {
		if (log.isTraceEnabled()) {
			log.trace("Class name: " + clazz.getName() + ", getAnnotation: " + clazz.getAnnotation(GsonIgnore.class));
		}
		if (clazz.getAnnotation(GsonIgnore.class) != null) {
			return true;
		}
		
		/*return forbidden_class.stream().anyMatch(f_class -> {
			return f_class.isAssignableFrom(clazz);
		});*/
		return false;
	}
	
}
