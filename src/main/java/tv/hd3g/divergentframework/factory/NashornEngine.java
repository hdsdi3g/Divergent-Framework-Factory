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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.log4j.Logger;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class NashornEngine {
	
	private static Logger log = Logger.getLogger(Factory.class);
	
	private ScriptEngine engine;
	private HashMap<String, Object> bindings;
	
	public NashornEngine() {
		// engine = new ScriptEngineManager().getEngineByName("nashorn");
		engine = new NashornScriptEngineFactory().getScriptEngine(classname -> {
			return false;
		});
		bindings = new HashMap<>();
		bindings.put("console", new JSToolkitConsole(log));
		
		engine.setBindings(new SimpleBindings(bindings), ScriptContext.ENGINE_SCOPE);
	}
	
	/*
	    binds.put("gson_simple", MyDMAM.gson_kit.getGsonSimple());
		binds.put("content", "Hello world !");
		binds.put("uu", "3");
	*/
	public HashMap<String, Object> getBindings() {
		return bindings;
	}
	
	// ScriptObjectMirror eval = (ScriptObjectMirror) engine.eval("uu=\"5\"; gson_simple.toJson(content); [uu, \"6\"]");
	// System.out.println(eval.values().stream().findFirst().get() + " " + engine.get("uu"));
	public Object eval(File js_file) throws NullPointerException, IOException, ScriptException {
		if (js_file.exists() == false) {
			throw new FileNotFoundException("\"" + js_file.getPath() + "\" in filesytem");
		}
		if (js_file.canRead() == false) {
			throw new IOException("Can't read element \"" + js_file.getPath() + "\"");
		}
		if (js_file.isFile() == false) {
			throw new FileNotFoundException("\"" + js_file.getPath() + "\" is not a file");
		}
		
		log.debug("Load and eval JS file: " + js_file);
		
		InputStreamReader isr = new InputStreamReader(new FileInputStream(js_file), StandardCharsets.UTF_8);
		try {
			Object result = engine.eval(isr);
			isr.close();
			return result;
		} catch (ScriptException e) {
			isr.close();
			throw e;
		}
	}
	
	/**
	 * @return java native, emptyList, ArrayList, emptyMap, LinkedHashMap
	 */
	public static Object getExtractJavaTypeFromJS(Object raw_js_attribute, Object... arguments_if_function_to_call) {
		if (raw_js_attribute == null) {
			return null;
		}
		if ((raw_js_attribute instanceof ScriptObjectMirror) == false) {
			return raw_js_attribute;
		}
		ScriptObjectMirror js_attribute = (ScriptObjectMirror) raw_js_attribute;
		
		if (js_attribute.isArray()) {
			if (js_attribute.size() == 0) {
				return Collections.emptyList();
			} else {
				ArrayList<Object> items = new ArrayList<>(js_attribute.size());
				js_attribute.forEach((pos, value) -> {
					items.add(getExtractJavaTypeFromJS(value, arguments_if_function_to_call));
				});
				return items;
			}
		} else if (js_attribute.isFunction()) {
			return getExtractJavaTypeFromJS(js_attribute.call(null, arguments_if_function_to_call), arguments_if_function_to_call);
		} else {
			if (js_attribute.size() == 0) {
				return Collections.emptyMap();
			} else {
				LinkedHashMap<String, Object> items = new LinkedHashMap<>(js_attribute.size());
				js_attribute.forEach((key, value) -> {
					items.put(key, getExtractJavaTypeFromJS(value, arguments_if_function_to_call));
				});
				return items;
			}
		}
	}
	
}
