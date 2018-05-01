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

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

import junit.framework.TestCase;

public class JsToolkitTest extends TestCase {
	
	public void testSimpleLoad() throws IOException {
		JsToolkit js = new JsToolkit();
		js.setVerboseErrors(System.out);
		
		js.getBindings().put("test", (Function<String, String>) t -> {
			return t.toUpperCase();
		});
		
		Object result = js.eval(JsToolkitTest.class.getResource("simple_load.js").openStream(), "simple_load.js");
		assertNotNull(result);
		assertEquals(String.class, result.getClass());
		assertEquals("Just a test".toUpperCase(), (String) result);
	}
	
	public void testDynamicProxy() throws IOException {
		DynamicInterface proxy = JsToolkit.instanceDynamicProxy(DynamicInterface.class, (method, arguments) -> {
			assertEquals("biConsumer", method.getName());
			assertNotNull(arguments);
			assertEquals(2, arguments.length);
			
			assertNotNull(arguments[0]);
			assertNotNull(arguments[1]);
			assertEquals(Long.class, arguments[0].getClass());
			assertEquals(Long.class, arguments[1].getClass());
			
			assertEquals(5l, (long) arguments[0]);
			assertEquals(10l, (long) arguments[1]);
			
			return 5l * 10l;
		});
		
		assertEquals(50l, proxy.biConsumer(5l, 10l));
	}
	
	public void testJsInterface() throws IOException {
		JsToolkit js = new JsToolkit();
		js.setVerboseErrors(System.out);
		
		js.getBindings().put("test1", (Runnable) () -> {
			// XXX capture
		});
		js.getBindings().put("test2", (Consumer<Integer>) t -> {
			// XXX capture
		});
		
		Object raw_js_interface = js.eval(JsToolkitTest.class.getResource("dynamic_interface.js").openStream(), "dynamic_interface.js");
		System.out.println(raw_js_interface.getClass());
	}
	// ScriptObjectMirror
}
