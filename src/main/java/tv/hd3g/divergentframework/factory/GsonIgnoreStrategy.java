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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class GsonIgnoreStrategy implements ExclusionStrategy {
	
	static final boolean DEBUG = false;
	
	private final ArrayList<Class<?>> forbidden_class;
	
	public GsonIgnoreStrategy() {
		forbidden_class = new ArrayList<>();
		forbidden_class.add(InputStream.class);
		forbidden_class.add(OutputStream.class);
		forbidden_class.add(Executor.class);
	}
	
	public boolean shouldSkipField(FieldAttributes f) {
		if (DEBUG) {
			System.out.println(f.getName());
			System.out.println(f.getAnnotation(GsonIgnore.class));
		}
		if (f.getAnnotation(GsonIgnore.class) != null) {
			return true;
		}
		
		Class<?> clazz = f.getDeclaredClass();
		
		return forbidden_class.stream().anyMatch(f_class -> {
			return f_class.isAssignableFrom(clazz);
		});
	}
	
	public boolean shouldSkipClass(Class<?> clazz) {
		if (DEBUG) {
			System.out.println(clazz.getName());
		}
		if (clazz.getAnnotation(GsonIgnore.class) != null) {
			return true;
		}
		
		return forbidden_class.stream().anyMatch(f_class -> {
			return f_class.isAssignableFrom(clazz);
		});
	}
	
}
