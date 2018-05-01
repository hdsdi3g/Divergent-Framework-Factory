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
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class JsLoader {
	private static Logger log = Logger.getLogger(JsLoader.class);
	
	private final ScriptEngine engine;
	private final SimpleBindings bindings;
	private PrintStream error_out;
	
	public JsLoader() {
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
	
	public synchronized JsLoader setVerboseErrors(PrintStream error_out) {
		this.error_out = error_out;
		return this;
	}
	
	public Object eval(File js_file) throws IOException {
		log.debug("Load and eval JS file: " + js_file);
		return eval(new FileReader(js_file), js_file.getPath());
	}
	
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
	
	public Object eval(InputStream js_source, String source_name) throws IOException {
		return eval(new InputStreamReader(js_source), source_name);
	}
	
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
	
	// TODO2 js file <-> Interface, plugged with Factory
	
	/*
	 * It can instance Interface
	 * @see https://gist.github.com/thomasdarimont/974bf70fe51bbe03b05e
	 */
	/*
	public static <T> T instanceDynamicProxy(Class<? extends T> interface_to_instanciate, SimpleInvocationHandler dynamic_behavior) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		// ClassLoader.getSystemClassLoader()
		Object proxy = Proxy.newProxyInstance(cl, new Class[] { interface_to_instanciate }, (Object _proxy_do_not_use, Method method, Object[] arguments) -> {
			return dynamic_behavior.dynamicInvoke(method, arguments);
		});
		
		@SuppressWarnings("unchecked")
		T result = (T) proxy;
		return result;
	}*/
	
	/*
	 * @see ScriptObjectMirror
	 */
	/*public <T> T getInterfaceDeclaredByJSModule(File configuration_dir, Class<? extends T> interface_reference, String module_name, Supplier<T> default_if_not_declare) {
		synchronized (lock) {
			if (js_module_manager == null) {
				js_module_manager = new JSModuleManager(configuration_dir);
			}
			js_module_manager.load();
		}
		
		T result = js_module_manager.moduleBindTo(module_name, interface_reference);
		if (result == null) {
			return default_if_not_declare.get();
		}
		
		return result;
	}*/
	
}
