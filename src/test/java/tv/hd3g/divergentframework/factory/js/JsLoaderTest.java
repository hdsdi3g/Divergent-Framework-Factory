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
import java.io.InputStream;
import java.util.function.Function;

import junit.framework.TestCase;

public class JsLoaderTest extends TestCase {
	
	public void testSimpleLoad() throws IOException {
		JsLoader js = new JsLoader();
		js.setVerboseErrors(System.out);
		
		js.getBindings().put("test", (Function<String, String>) t -> {
			return t.toUpperCase();
		});
		
		InputStream js_source = JsLoaderTest.class.getResource("simple_load.js").openStream();
		
		Object result = js.eval(js_source, "simple_load.js");
		assertNotNull(result);
		assertEquals(String.class, result.getClass());
		assertEquals("Just a test".toUpperCase(), (String) result);
	}
	
}
