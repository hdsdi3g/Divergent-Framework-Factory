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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.script.ScriptException;

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
		JsToolkit js = new JsToolkit();
		DynamicInterface proxy = js.instanceDynamicProxy(DynamicInterface.class, (method, arguments) -> {
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
		}, "nothing", Collections.emptyList());
		
		assertEquals(50l, proxy.biConsumer(5l, 10l));
	}
	
	public void testJsInterface() throws IOException, ScriptException {
		JsToolkit js = new JsToolkit();
		js.setVerboseErrors(System.out);
		
		AtomicInteger test1_trigger_count = new AtomicInteger(0);
		js.getBindings().put("test1", (Runnable) () -> {
			test1_trigger_count.incrementAndGet();
		});
		
		List<Integer> test2_content = Collections.synchronizedList(new ArrayList<>());
		js.getBindings().put("test2", (Consumer<Integer>) t -> {
			test2_content.add(t);
		});
		
		DynamicInterface d_i = js.instanceTypeFromJs(DynamicInterface.class, JsToolkitTest.class.getResource("dynamic_interface.js").openStream(), "dynamic_interface.js", true);
		assertNotNull(d_i);
		
		assertEquals(0, test1_trigger_count.get());
		d_i.simple();
		assertEquals(1, test1_trigger_count.get());
		
		assertEquals(0, test2_content.size());
		d_i.intConsumer(5);
		d_i.intConsumer(10);
		assertEquals(2, test2_content.size());
		assertEquals(5, test2_content.get(0).intValue());
		assertEquals(10, test2_content.get(1).intValue());
		
		assertEquals("Hello world!", d_i.stringSupplier());
		
		assertEquals(5l * 10l, d_i.biConsumer(5l, 10l));
		assertEquals(42, d_i.stringToIntFunction("42"));
		assertEquals(1 + 3 + 5 + 7 + 9, d_i.varArgs(1, 3, 5, 7, 9));
		
		assertFalse(d_i.aDefault());
		
		List<String> list_1 = d_i.stringList(new ArrayList<>(Arrays.asList("az", "é", "5")));
		assertNotNull(list_1);
		assertEquals(3, list_1.size());
		assertEquals("AZ", list_1.get(0));
		assertEquals("É", list_1.get(1));
		assertEquals("5", list_1.get(2));
		
		HashMap<String, Integer> map_1 = d_i.intMap(Map.of("foo", 2, "v1", 5, "baz", 7));
		assertNotNull(map_1);
		assertEquals(1, map_1.size());
		assertEquals((Integer) (5 * 2), map_1.get("v1"));
		
		assertEquals("Static string var", d_i.formStaticJS());
	}
	
}
