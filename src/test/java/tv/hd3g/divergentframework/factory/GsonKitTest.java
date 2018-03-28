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

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import junit.framework.TestCase;
import tv.hd3g.divergentframework.factory.GsonKit.KeyValueNullContentMergeBehavior;

public class GsonKitTest extends TestCase {
	
	public void testjsonMerge() {
		JsonObject current = new JsonObject();
		current.addProperty("v1", "a");
		current.addProperty("v2", "b");
		
		JsonObject newer = new JsonObject();
		newer.addProperty("v2", "B");
		newer.addProperty("v3", "C");
		
		GsonKit.jsonMerge(current, newer, KeyValueNullContentMergeBehavior.KEEP);
		
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
		
		GsonKit.jsonMerge(current, newer, KeyValueNullContentMergeBehavior.KEEP);
		
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
		
		GsonKit.jsonMerge(current, newer, KeyValueNullContentMergeBehavior.KEEP);
		
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
	
	public void testJsonKeepNullMap() {
		JsonObject current = new JsonObject();
		current.addProperty("v1", "a");
		current.addProperty("v2", "b");
		
		JsonObject newer = new JsonObject();
		newer.add("v2", JsonNull.INSTANCE);
		newer.addProperty("v3", "C");
		
		GsonKit.jsonMerge(current, newer, KeyValueNullContentMergeBehavior.KEEP);
		
		assertEquals(3, current.size());
		
		assertTrue(current.has("v1"));
		assertTrue(current.has("v2"));
		assertTrue(current.has("v3"));
		
		assertEquals("a", current.get("v1").getAsString());
		assertEquals(JsonNull.INSTANCE, current.get("v2"));
		assertEquals("C", current.get("v3").getAsString());
	}
	
	public void testJsonRemoveNullMap() {
		JsonObject current = new JsonObject();
		current.addProperty("v1", "a");
		current.addProperty("v2", "b");
		
		JsonObject newer = new JsonObject();
		newer.add("v2", JsonNull.INSTANCE);
		newer.addProperty("v3", "C");
		
		GsonKit.jsonMerge(current, newer, KeyValueNullContentMergeBehavior.REMOVE);
		
		assertEquals(2, current.size());
		
		assertTrue(current.has("v1"));
		assertFalse(current.has("v2"));
		assertTrue(current.has("v3"));
		
		assertEquals("a", current.get("v1").getAsString());
		assertEquals("C", current.get("v3").getAsString());
	}
	
	public void testJsonKeepNullList() {
		JsonArray current = new JsonArray();
		current.add("a");
		current.add("b");
		current.add("c");
		
		JsonArray newer = new JsonArray();
		newer.add("a");
		newer.add(JsonNull.INSTANCE);
		newer.add("C");
		
		GsonKit.jsonMerge(current, newer, KeyValueNullContentMergeBehavior.KEEP);
		
		assertEquals(3, current.size());
		
		assertEquals("a", current.get(0).getAsString());
		assertEquals(JsonNull.INSTANCE, current.get(1));
		assertEquals("C", current.get(2).getAsString());
		
	}
	
	public void testJsonCompareMapNull() {
		JsonObject current = new JsonObject();
		current.addProperty("v1", "a");
		current.addProperty("v2", "b");
		current.addProperty("v4", "c");
		current.addProperty("v5", "d");
		
		JsonObject newer = new JsonObject();
		newer.add("v2", JsonNull.INSTANCE);
		newer.add("v5", JsonNull.INSTANCE);
		newer.addProperty("v3", "C");
		newer.add("v4", JsonNull.INSTANCE);
		
		ArrayList<String> newers = new ArrayList<>();
		ArrayList<String> removed = new ArrayList<>();
		
		GsonKit.jsonCompare(current, newer, (k, v) -> newers.add(k), (k, v) -> removed.add(k), null, null);
		
		assertEquals(1, newers.size());
		assertEquals(3, removed.size());
		
		assertEquals("v3", newers.get(0));
		
		assertEquals("v2", removed.get(0));
		assertEquals("v4", removed.get(1));
		assertEquals("v5", removed.get(2));
	}
	
}
