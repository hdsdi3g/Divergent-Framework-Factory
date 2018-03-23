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
package tv.hd3g.divergentframework.factory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import junit.framework.TestCase;

public class GsonKitTest extends TestCase {
	
	public void testJsonMergue() {
		JsonObject current = new JsonObject();
		current.addProperty("v1", "a");
		current.addProperty("v2", "b");
		
		JsonObject newer = new JsonObject();
		newer.addProperty("v2", "B");
		newer.addProperty("v3", "C");
		
		GsonKit.jsonMergue(current, newer);
		
		assertTrue(current.has("v1"));
		assertTrue(current.has("v2"));
		assertTrue(current.has("v3"));
		
		assertEquals("a", current.get("v1").getAsString());
		assertEquals("B", current.get("v2").getAsString());
		assertEquals("C", current.get("v3").getAsString());
		
		JsonArray sub = new JsonArray();
		sub.add("d1");
		sub.add(1);
		sub.add(true);
		newer = new JsonObject();
		newer.add("v4", sub);
		
		GsonKit.jsonMergue(current, newer);
		
		assertTrue(current.has("v1"));
		assertTrue(current.has("v2"));
		assertTrue(current.has("v3"));
		assertTrue(current.has("v4"));
		
		assertEquals("a", current.get("v1").getAsString());
		assertEquals("B", current.get("v2").getAsString());
		assertEquals("C", current.get("v3").getAsString());
		assertTrue(current.get("v4").isJsonArray());
		assertEquals("d1", current.get("v4").getAsJsonArray().get(0).getAsString());
		assertEquals(1, current.get("v4").getAsJsonArray().get(1).getAsInt());
		assertTrue(current.get("v4").getAsJsonArray().get(2).getAsBoolean());
		
		sub = new JsonArray();
		sub.add("d4");
		newer = new JsonObject();
		newer.add("v1", sub);
		
		GsonKit.jsonMergue(current, newer);
		
		assertTrue(current.has("v1"));
		assertTrue(current.has("v2"));
		assertTrue(current.has("v3"));
		assertTrue(current.has("v4"));
		
		assertEquals("B", current.get("v2").getAsString());
		assertEquals("C", current.get("v3").getAsString());
		assertTrue(current.get("v4").isJsonArray());
		
		assertTrue(current.get("v1").isJsonArray());
		assertEquals("d4", current.get("v1").getAsJsonArray().get(0).getAsString());
	}
	
	// TODO test null behaviors: null value in a map key should do a remove branch action and trigger an remove callback
	
}
