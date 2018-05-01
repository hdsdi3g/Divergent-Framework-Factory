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
package tv.hd3g.divergentframework.factory.js;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JsToolkit {
	private static Logger log = Logger.getLogger(JsToolkit.class);
	
	private final ScriptEngine engine;
	private final SimpleBindings bindings;
	private PrintStream error_out;
	
	public JsToolkit() {
		engine = new NashornScriptEngineFactory().getScriptEngine(classname -> {
			return true;
		});
		bindings = new SimpleBindings(new ConcurrentHashMap<String, Object>());
		bindings.put("console", new JSToolkitConsole(log));
		engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
		error_out = null;
	}
	
	/**
	 * @return synchonized map
	 */
	public SimpleBindings getBindings() {
		return bindings;
	}
	
	public synchronized JsToolkit setVerboseErrors(PrintStream error_out) {
		this.error_out = error_out;
		return this;
	}
	
	/**
	 * @return Java primitive type or ScriptObjectMirror for complex results.
	 */
	public Object eval(File js_file) throws IOException {
		log.debug("Load and eval JS file: " + js_file);
		return eval(new FileReader(js_file), js_file.getPath());
	}
	
	/**
	 * @return Java primitive type or ScriptObjectMirror for complex results.
	 */
	public Object eval(Reader js_source, String source_name) throws IOException {
		ArrayList<String> file_lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(js_source)) {
			String line;
			while ((line = br.readLine()) != null) {
				file_lines.add(line);
			}
		}
		js_source.close();
		
		return eval(file_lines, source_name);
	}
	
	/**
	 * @return Java primitive type or ScriptObjectMirror for complex results.
	 */
	public Object eval(InputStream js_source, String source_name) throws IOException {
		return eval(new InputStreamReader(js_source), source_name);
	}
	
	/**
	 * @return Java primitive type or ScriptObjectMirror for complex results.
	 */
	public Object eval(ArrayList<String> file_lines, String source_name) {
		if (file_lines == null) {
			throw new NullPointerException("\"file_lines\" can't to be null");
		}
		if (source_name == null) {
			throw new NullPointerException("\"source_name\" can't to be null");
		}
		try {
			return engine.eval(file_lines.stream().collect(Collectors.joining("\r\n")));
		} catch (ScriptException e) {
			if (error_out != null) {
				error_out.println("=== Javascript Nashorn error ===");
				error_out.println(source_name + " :: " + e.getMessage());
				int line = e.getLineNumber();
				if (line > -1) {
					if (line - 2 > -1) {
						System.out.println((line - 1) + " | " + file_lines.get(line - 2));
					}
					
					System.out.println(line + " | " + file_lines.get(line - 1));
					
					System.out.print(StringUtils.repeat(" ", String.valueOf(line).length()) + " | " + StringUtils.repeat(" ", e.getColumnNumber() - 1));
					System.out.println("^");
					
					if (line < file_lines.size()) {
						System.out.println((line + 1) + " | " + file_lines.get(line));
					}
				}
				error_out.println("================================");
			}
			
			throw new RuntimeException("Trouble with JS source \"" + source_name + "\", line " + e.getLineNumber() + ", col " + e.getColumnNumber() + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * It can instance Interface
	 * @see https://gist.github.com/thomasdarimont/974bf70fe51bbe03b05e
	 *      TODO move to factory
	 */
	public static <T> T instanceDynamicProxy(Class<? extends T> interface_to_instanciate, SimpleInvocationHandler dynamic_behavior) {
		if (interface_to_instanciate.isInterface() == false) {
			throw new ClassCastException("Class " + interface_to_instanciate + " is not an Interface");
		}
		
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		
		Object proxy = Proxy.newProxyInstance(cl, new Class[] { interface_to_instanciate }, (Object _proxy_do_not_use, Method method, Object[] arguments) -> {
			return dynamic_behavior.dynamicInvoke(method, arguments);
		});
		
		@SuppressWarnings("unchecked")
		T result = (T) proxy;
		return result;
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
	
	public <T> T instanceTypeFromJs(Class<T> type, ScriptObjectMirror js_content) throws ScriptException {
		if (js_content == null) {
			throw new NullPointerException("\"js_content\" can't to be null");
		} else if (js_content.isArray()) {
			throw new ScriptException("js_content parameter can't be a JS Array");
		} else if (js_content.getClassName().equalsIgnoreCase("Object") == false) {
			throw new ScriptException("js_content parameter must be a JS object");
		}
		
		// TODO first pass to check missing declarations ?
		
		/*instanceDynamicProxy(interface_reference, (method_desc, arguments) -> {
		String method = method_desc.getName();
		if (content.containsKey(method) == false) {
		//log.warn("Interface " + interface_reference.getName() + " want to call a missing JS method, " + method + "() for module " + toString() + " !");
		return null;
		}
		return getExtractJavaTypeFromJS(content.get(method), arguments);
		});*/
		
		return null;// XXX
	}
	
	// TODO2 js file <-> Interface, plugged with Factory
	// TODO3 "BindTo"
	
}
