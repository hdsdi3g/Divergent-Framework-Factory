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
package tv.hd3g.divergentframework.factory.js;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.script.ScriptException;

import org.apache.log4j.Logger;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

@Deprecated

public final class JSModuleManager {
	private static Logger log = Logger.getLogger(JSModuleManager.class);
	
	private ArrayList<JSModule> declared_modules;
	
	public JSModuleManager(File configuration_dir) {
		declared_modules = new ArrayList<>();
	}
	
	public class PublishedModuleJSAPI {
		private PublishedModuleJSAPI() {
		}
		
		public void register(Object _module) throws ScriptException {
			if (_module instanceof ScriptObjectMirror == false) {
				throw new ScriptException("module parameter must be a JS object");
			}
			ScriptObjectMirror module = (ScriptObjectMirror) _module;
			
			if (module.isArray()) {
				throw new ScriptException("module parameter can't be a JS Array");
			}
			if (module.getClassName().equalsIgnoreCase("Object") == false) {
				throw new ScriptException("module parameter must be a JS object");
			}
			
			JSModule new_module = new JSModule();
			Arrays.asList(module.getOwnKeys(false)).stream().forEach(key -> {
				try {
					JSModule.class.getDeclaredField(key).set(new_module, module.get(key));
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Can't acces to field \"" + key + "\" in JSModule class", e);
				}
			});
			
			if (new_module.content == null) {
				throw new NullPointerException("Content field is not set... This module (" + new_module.toString() + ") can't to be fonctionnal...");
			}
			
			declared_modules.add(new_module);
		}
		
	}
	
	final class JSModule {
		ScriptObjectMirror content;
	}
	
}
