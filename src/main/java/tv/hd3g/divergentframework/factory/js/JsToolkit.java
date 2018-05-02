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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import jdk.nashorn.api.scripting.NashornException;
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
	
	private static Reader fileToReader(File js_file) throws FileNotFoundException {
		return new FileReader(js_file);
	}
	
	private static ArrayList<String> readerToLines(Reader js_source) throws IOException {
		ArrayList<String> file_lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(js_source)) {
			String line;
			while ((line = br.readLine()) != null) {
				file_lines.add(line);
			}
		}
		js_source.close();
		return file_lines;
	}
	
	private static Reader inputStreamToReader(InputStream js_source) throws FileNotFoundException {
		return new InputStreamReader(js_source);
	}
	
	/**
	 * @return Java primitive type or ScriptObjectMirror for complex results.
	 */
	public Object eval(File js_file) throws IOException {
		log.debug("Load and eval JS file: " + js_file);
		return eval(readerToLines(fileToReader(js_file)), js_file.getPath());
	}
	
	/**
	 * @return Java primitive type or ScriptObjectMirror for complex results.
	 */
	public Object eval(Reader js_source, String source_name) throws IOException {
		return eval(readerToLines(js_source), source_name);
	}
	
	/**
	 * @return Java primitive type or ScriptObjectMirror for complex results.
	 */
	public Object eval(InputStream js_source, String source_name) throws IOException {
		return eval(readerToLines(inputStreamToReader(js_source)), source_name);
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
			showError(file_lines, source_name, e);
			throw new RuntimeException("Trouble with JS source \"" + source_name + "\", line " + e.getLineNumber() + ", col " + e.getColumnNumber() + ": " + e.getMessage(), e);
		}
	}
	
	private void showError(List<String> file_lines, String source_name, ScriptException e) {
		showError(file_lines, source_name, e.getLineNumber(), e.getColumnNumber(), e.getMessage());
	}
	
	private void showError(List<String> file_lines, String source_name, NashornException e) {
		showError(file_lines, source_name, e.getLineNumber(), e.getColumnNumber(), e.getMessage());
	}
	
	private void showError(List<String> file_lines, String source_name, int line_number, int col_number, String message) {
		if (error_out != null) {
			error_out.println("=== Javascript Nashorn error ===");
			error_out.println(source_name + " :: " + message);
			int line = line_number;
			if (line > -1) {
				if (line - 3 > -1) {
					System.out.println((line - 2) + "  | " + file_lines.get(line - 3));
				}
				if (line - 2 > -1) {
					System.out.println((line - 1) + "  | " + file_lines.get(line - 2));
				}
				
				System.out.println(line + "  > " + file_lines.get(line - 1));
				
				if (col_number > 0) {
					System.out.print(StringUtils.repeat(" ", String.valueOf(line).length()) + "  | " + StringUtils.repeat(" ", col_number - 1));
					System.out.println("^");
				}
				
				if (line < file_lines.size()) {
					System.out.println((line + 1) + "  | " + file_lines.get(line));
					if (line + 1 < file_lines.size()) {
						System.out.println((line + 2) + "  | " + file_lines.get(line + 1));
					}
				}
			}
			error_out.println("================================");
		}
	}
	
	/**
	 * It can instance Interface
	 * @see https://gist.github.com/thomasdarimont/974bf70fe51bbe03b05e
	 */
	<T> T instanceDynamicProxy(Class<? extends T> interface_to_instanciate, SimpleInvocationHandler dynamic_behavior, String source_name, List<String> file_lines) {
		if (interface_to_instanciate.isInterface() == false) {
			throw new ClassCastException("Class " + interface_to_instanciate + " is not an Interface");
		}
		
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		
		Object proxy = Proxy.newProxyInstance(cl, new Class[] { interface_to_instanciate }, (Object _proxy_do_not_use, Method method, Object[] arguments) -> {
			try {
				return dynamic_behavior.dynamicInvoke(method, arguments);
			} catch (NashornException e) {
				showError(file_lines, source_name, e);
				throw e;
			}
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
	
	public <T> T instanceTypeFromJs(Class<T> type, ScriptObjectMirror js_content, boolean all_methods_must_be_provisioned, String source_name, List<String> file_lines) throws ScriptException {
		if (js_content == null) {
			throw new NullPointerException("\"js_content\" can't to be null");
		} else if (js_content.isFunction()) {
			throw new ScriptException("js_content parameter can't be a JS function: " + js_content);
		} else if (js_content.isArray()) {
			throw new ScriptException("js_content parameter can't be a JS Array");
		} else if (js_content.getClassName().equalsIgnoreCase("Object") == false) {
			throw new ScriptException("js_content parameter must be a JS object");
		} else if (js_content.size() == 0) {
			throw new ScriptException("js_content parameter don't return items (empty js map)");
		}
		
		final HashMap<String, Object> js_content_map = new HashMap<>(js_content.size());
		js_content.forEach((key, value) -> {
			// System.out.println(value);
			js_content_map.put(key.toLowerCase(), value);
		});
		
		if (all_methods_must_be_provisioned) {
			List<Method> missing_methods = Arrays.asList(type.getMethods()).stream().filter(interface_method -> {
				/**
				 * Missing method declaration
				 */
				return js_content_map.containsKey(interface_method.getName().toLowerCase()) == false;
			}).collect(Collectors.toList());
			
			if (missing_methods.isEmpty() == false) {
				String verbose_view = missing_methods.stream().map(method -> {
					return method.getName();
				}).collect(Collectors.joining(", "));
				
				throw new ScriptException("Missing methods from JS content for " + type.getName() + ": " + verbose_view);
			}
		}
		
		return instanceDynamicProxy(type, (method, arguments) -> {
			String simple_name = method.getName().toLowerCase();
			
			if (js_content_map.containsKey(simple_name) == false) {
				return null;
			}
			
			if (log.isTraceEnabled()) {
				log.trace("Dynamic JS call for " + type.getName() + "." + method.getName());
			}
			
			if (method.getReturnType() != null) {
				
				Class<?> r_type = method.getReturnType();
				if (r_type.isAssignableFrom(Number.class) | r_type == int.class | r_type == long.class | r_type == short.class | r_type == float.class | r_type == double.class | r_type == byte.class | r_type == char.class) {
					/**
					 * Number management: Js return only Double values. Here we try to convert raw double value to method best return type.
					 */
					Object raw_return = getExtractJavaTypeFromJS(js_content_map.get(simple_name), arguments);
					
					if (raw_return == null) {
						return null;
					} else if (raw_return instanceof Double) {
						Double d_return = (Double) raw_return;
						
						switch (r_type.getSimpleName().toLowerCase()) {
						case "integer":
						case "int":
							return d_return.intValue();
						case "long":
							return d_return.longValue();
						case "short":
							return d_return.shortValue();
						case "float":
							return d_return.floatValue();
						case "byte":
							return d_return.byteValue();
						case "char":
							return (char) d_return.intValue();
						}
						
						/**
						 * Double by default.
						 */
						return d_return;
					}
					
					return raw_return;
				}
			}
			
			return getExtractJavaTypeFromJS(js_content_map.get(simple_name), arguments);
		}, source_name, file_lines);
	}
	
	public <T> T instanceTypeFromJs(Class<T> type, File js_file, boolean all_methods_must_be_provisioned) throws IOException, ScriptException {
		log.debug("Load and eval JS file: " + js_file);
		return instanceTypeFromJs(type, readerToLines(fileToReader(js_file)), js_file.getPath(), all_methods_must_be_provisioned);
	}
	
	public <T> T instanceTypeFromJs(Class<T> type, Reader js_source, String source_name, boolean all_methods_must_be_provisioned) throws IOException, ScriptException {
		return instanceTypeFromJs(type, readerToLines(js_source), source_name, all_methods_must_be_provisioned);
	}
	
	public <T> T instanceTypeFromJs(Class<T> type, InputStream js_source, String source_name, boolean all_methods_must_be_provisioned) throws IOException, ScriptException {
		return instanceTypeFromJs(type, readerToLines(inputStreamToReader(js_source)), source_name, all_methods_must_be_provisioned);
	}
	
	public <T> T instanceTypeFromJs(Class<T> type, ArrayList<String> file_lines, String source_name, boolean all_methods_must_be_provisioned) throws ScriptException {
		Object raw_js_interface = eval(file_lines, source_name);
		if (raw_js_interface == null) {
			throw new NullPointerException("Javascript return null content.");
		} else if ((raw_js_interface instanceof ScriptObjectMirror) == false) {
			throw new ScriptException("Javascript don't return a complex item (ScriptObjectMirror)");
		}
		
		try {
			return instanceTypeFromJs(type, (ScriptObjectMirror) raw_js_interface, all_methods_must_be_provisioned, source_name, file_lines);
		} catch (ScriptException e) {
			showError(file_lines, source_name, e);
			throw e;
		}
	}
	
}
