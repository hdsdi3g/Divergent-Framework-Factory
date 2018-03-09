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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public final class JSModuleManager {
	private static Logger log = Logger.getLogger(JSModuleManager.class);
	
	public static final String MODULE_JS_OBJECT_NAME = "universalmodule";
	
	private ArrayList<File> js_conf_sources;
	private NashornEngine engine;
	private PublishedModuleJSAPI published_api;
	private ArrayList<JSModule> declared_modules;
	
	public JSModuleManager(File configuration_dir) {
		js_conf_sources = new ArrayList<>(Arrays.asList(configuration_dir.listFiles((file) -> {
			if (file.isDirectory()) {
				return false;
			}
			if (file.isHidden()) {
				return false;
			}
			return FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("js");
		})));
		
		js_conf_sources.sort((l, r) -> {
			return l.compareTo(r);
		});
		
		published_api = new PublishedModuleJSAPI();
		
		engine = new NashornEngine();
		engine.getBindings().put(MODULE_JS_OBJECT_NAME, published_api);
		
		declared_modules = new ArrayList<>();
	}
	
	private static void extractJSNameAnnotation(Class<?> _class, PrintWriter out_js) {
		Annotation[] anno = _class.getAnnotations();
		String comment = getJSComment(anno);
		if (comment != null) {
			out_js.println("    /*");
			out_js.println("     * " + comment);
			out_js.println("     */");
		}
	}
	
	private static void extractJSNameAnnotation(Method method, PrintWriter out_js) {
		ArrayList<String> lines = new ArrayList<>();
		
		Annotation[] anno = method.getAnnotations();
		String comment = getJSComment(anno);
		if (comment != null) {
			lines.add(comment);
		}
		
		lines.addAll(Arrays.asList(method.getParameters()).stream().map(param -> {
			String var_name = getJSVarName(param.getAnnotations());
			if (var_name == null) {
				var_name = "";
			} else {
				var_name = " " + var_name;
			}
			
			String p_comment = getJSComment(param.getAnnotations());
			if (p_comment == null) {
				p_comment = "";
			} else {
				p_comment = " " + p_comment;
			}
			
			String mtd = "";
			if (param.isVarArgs()) {
				mtd = "...";
			} else if (param.getType().isArray()) {
				mtd = "[]";
			} else if (param.getType().isEnum()) {
				mtd = " enum";
			}
			
			return "@param " + param.getType().getSimpleName() + var_name + mtd + p_comment;
		}).collect(Collectors.toList()));
		
		String return_simple_name = method.getReturnType().getSimpleName();
		if (return_simple_name.equalsIgnoreCase("void") == false) {
			lines.add("@return " + return_simple_name);
		}
		
		if (lines.isEmpty() == false) {
			out_js.println("        /*");
			lines.forEach(l -> {
				out_js.println("         * " + l);
			});
			out_js.println("         */");
		}
	}
	
	private static String getJSComment(Annotation[] anno) {
		return Arrays.asList(anno).stream().filter(a -> {
			return a instanceof JSComment;
		}).map(a -> {
			return ((JSComment) a).value();
		}).findFirst().orElseGet(() -> {
			return null;
		});
	}
	
	private static String getJSVarName(Annotation[] anno) {
		return Arrays.asList(anno).stream().filter(a -> {
			return a instanceof JSVarName;
		}).map(a -> {
			return ((JSVarName) a).value();
		}).findFirst().orElseGet(() -> {
			return null;
		});
	}
	
	/**
	 * @return created file
	 */
	public static File createEmptyJSDefinition(File target_directory, Class<?> interface_to_implements, String new_module_name, boolean no_hints) throws IOException {
		File out_js_file = new File(target_directory.getAbsolutePath() + File.separator + new_module_name + "-module.js");
		PrintWriter out_js = new PrintWriter(new FileOutputStream(out_js_file), true);
		if (no_hints == false) {
			out_js.println("/* This file is autogenerated by MyDMAM, but it's a template. Free feel to edit it. */");
			out_js.println();
		}
		
		out_js.println("// This module is mapped from " + interface_to_implements.getName());
		out_js.println(MODULE_JS_OBJECT_NAME + ".register({");
		out_js.println("    name:    \"" + new_module_name + "\",");
		out_js.println("    vendor:  \"MyCompany\",");
		out_js.println("    version: \"1.0\",");
		
		extractJSNameAnnotation(interface_to_implements, out_js);
		out_js.println("    content: {");
		Arrays.asList(interface_to_implements.getMethods()).forEach(method -> {
			AtomicInteger pos = new AtomicInteger(0);
			String return_simple_name = method.getReturnType().getSimpleName();
			
			List<Parameter> params = Arrays.asList(method.getParameters());
			String funct_param = params.stream().map(parameter -> {
				String var_name = getJSVarName(parameter.getAnnotations());
				if (var_name == null) {
					var_name = "arg" + pos.getAndIncrement();
				} else {
					pos.getAndIncrement();
				}
				
				return var_name;
			}).collect(Collectors.joining(", "));
			
			extractJSNameAnnotation(method, out_js);
			
			out_js.println("        " + method.getName() + ": function(" + funct_param + ") {");
			if (return_simple_name.equalsIgnoreCase("void") == false) {
				if (return_simple_name.equalsIgnoreCase("boolean")) {
					out_js.println("            return false;");
				} else {
					out_js.println("            return null;");
				}
			}
			out_js.println("        },");
		});
		out_js.println("    },");
		out_js.println("});");
		
		if (no_hints == false) {
			out_js.println("/*");
			out_js.println(" You can use console.log() or error() or debug() for debugging in Java side.");
			out_js.println(" Content entries can be JS variables or functions.");
			out_js.println(" Content entries names are not case sensitive.");
			out_js.println(" Use Java annotations @JSVarName and @JSComment for display some informations in autogenerated JS code.");
			out_js.println("*/");
		}
		out_js.close();
		return out_js_file;
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
	
	/**
	 * @return null if it can't found a module by name.
	 */
	<T> T moduleBindTo(String module_name, Class<? extends T> interface_reference) {
		JSModule module = declared_modules.stream().filter(m -> {
			return m.name.equalsIgnoreCase(module_name);
		}).findFirst().orElseGet(() -> {
			return null;
		});
		
		return module.bindTo(interface_reference);
	}
	
	final class JSModule {
		// Do not set to final..
		String name;
		String vendor;
		String version;
		ScriptObjectMirror content;
		
		public String toString() {
			return name + " (" + vendor + ") v" + version;
		}
		
		private JSModule() {
		}
		
		private <T> T bindTo(Class<? extends T> interface_reference) {
			return Factory.instanceDynamicProxy(interface_reference, (method_desc, arguments) -> {
				String method = method_desc.getName();
				if (content.containsKey(method) == false) {
					log.warn("Interface " + interface_reference.getName() + " want to call a missing JS method, " + method + "() for module " + toString() + " !");
					return null;
				}
				return NashornEngine.getExtractJavaTypeFromJS(content.get(method), arguments);
			});
		}
		
	}
	
	public void load() {
		js_conf_sources.forEach(js_file -> {
			try {
				log.debug("Load JS file: " + js_file);
				engine.eval(js_file);
			} catch (NullPointerException | IOException | ScriptException e) {
				log.error("Can't load JS file: " + js_file, e);
			}
		});
	}
	
}
