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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import junit.framework.TestCase;

public class ConfigurationFileTypeTest extends TestCase {
	
	public void testYAMLLoading() throws IOException {
		StringWriter strOut = new StringWriter();
		
		PrintWriter pw = new PrintWriter(strOut);
		pw.println("truc:");
		pw.println("   a: 1");
		pw.println("   b: \"toto\"");
		pw.println("---");
		pw.println("truc:");
		pw.println("   a: 2");
		
		assertEquals("{truc={\"a\":2,\"b\":\"toto\"}}", ConfigurationFileType.YAML.getContent(new StringReader(strOut.toString())).toString().trim());
	}
	
	public void testJsonLoading() throws IOException {
		StringWriter strOut = new StringWriter();
		
		/**
		 * {"c1":{"v1":"toto1","v2":"toto2"},"c2":{"v3":"toto3"}}
		 */
		PrintWriter pw = new PrintWriter(strOut);
		pw.println("{");
		pw.println("   c1: {");
		pw.println("      v1: \"toto1\",");
		pw.println("      v2: \"toto2\"");
		pw.println("   },");
		pw.println("   c2: {");
		pw.println("      v3: \"toto3\"");
		pw.println("   }");
		pw.println("}");
		
		assertEquals("{\"c1\":{\"v1\":\"toto1\",\"v2\":\"toto2\"},\"c2\":{\"v3\":\"toto3\"}}", GsonKit.parser.parse(new StringReader(strOut.toString())).toString());
		assertEquals("{c1={\"v1\":\"toto1\",\"v2\":\"toto2\"}, c2={\"v3\":\"toto3\"}}", ConfigurationFileType.JSON.getContent(new StringReader(strOut.toString())).toString().trim());
	}
	
	public void testJsonParser() {
		JsonElement t1 = GsonKit.parser.parse("a");
		JsonElement t2 = GsonKit.parser.parse("2");
		JsonElement t3 = GsonKit.parser.parse("true");
		JsonElement t4 = GsonKit.parser.parse("\"4\"");
		
		assertNotNull(t1);
		assertNotNull(t2);
		assertNotNull(t3);
		assertNotNull(t4);
		
		assertTrue(t1.isJsonPrimitive());
		assertTrue(t2.isJsonPrimitive());
		assertTrue(t3.isJsonPrimitive());
		assertTrue(t4.isJsonPrimitive());
		
		assertTrue(t1.getAsJsonPrimitive().isString());
		assertTrue(t2.getAsJsonPrimitive().isNumber());
		assertTrue(t3.getAsJsonPrimitive().isBoolean());
		assertTrue(t4.getAsJsonPrimitive().isString());
		
		assertEquals("a", t1.getAsJsonPrimitive().getAsString());
		assertEquals(2, t2.getAsJsonPrimitive().getAsInt());
		assertEquals(true, t3.getAsJsonPrimitive().getAsBoolean());
		assertEquals("4", t4.getAsJsonPrimitive().getAsString());
		
		JsonElement t5 = GsonKit.parser.parse("{truc={\"a\":2}}");
		assertNotNull(t5);
		assertTrue(t5.isJsonObject());
		assertTrue(t5.getAsJsonObject().has("truc"));
		assertTrue(t5.getAsJsonObject().get("truc").getAsJsonObject().has("a"));
		assertEquals(2, t5.getAsJsonObject().get("truc").getAsJsonObject().get("a").getAsInt());
		
		JsonElement t6 = GsonKit.parser.parse("[\"a\"]");
		assertNotNull(t6);
		assertTrue(t6.isJsonArray());
		assertEquals(1, t6.getAsJsonArray().size());
		assertEquals("a", t6.getAsJsonArray().get(0).getAsString());
	}
	
	public void testProps() throws IOException {
		StringWriter strOut = new StringWriter();
		
		PrintWriter pw = new PrintWriter(strOut);
		pw.println("value1=a");
		pw.println("value2=2");
		pw.println("value3=true");
		pw.println("value4=\"4\"");
		
		Properties p = new Properties();
		p.load(new StringReader(strOut.toString()));
		
		assertTrue(p.get("value1") instanceof String);
		assertTrue(p.get("value2") instanceof String);
		assertTrue(p.get("value3") instanceof String);
		assertTrue(p.get("value4") instanceof String);
	}
	
	public void testPropertiesLoading() throws IOException {
		StringWriter strOut = new StringWriter();
		PrintWriter pw = new PrintWriter(strOut);
		pw.println("mnemonic.v1=a");
		pw.println("mnemonic.v2=2");
		pw.println("mnemonic.v3=true");
		pw.println("mnemonic.v4=\"4\"");
		pw.println("mnemonic.v5=[\"b\"]");
		pw.println("mnemonic.v6={truc={\"c\":6}}");
		
		HashMap<String, JsonObject> datas = ConfigurationFileType.PROPERTY.getContent(new StringReader(strOut.toString()));
		
		assertTrue(datas.containsKey("mnemonic"));
		
		JsonObject root = datas.get("mnemonic");
		
		assertEquals(6, root.size());
		for (int pos = 1; pos <= root.size(); pos++) {
			assertTrue(root.has("v" + pos));
		}
		
		assertEquals("a", root.get("v1").getAsString());
		assertEquals(2, root.get("v2").getAsInt());
		assertEquals(true, root.get("v3").getAsBoolean());
		assertEquals("4", root.get("v4").getAsString());
		
		assertTrue(root.get("v5").isJsonArray());
		assertEquals(1, root.get("v5").getAsJsonArray().size());
		assertEquals("b", root.get("v5").getAsJsonArray().get(0).getAsString());
		
		assertTrue(root.get("v6").isJsonObject());
		assertTrue(root.get("v6").getAsJsonObject().has("truc"));
		assertTrue(root.get("v6").getAsJsonObject().get("truc").getAsJsonObject().has("c"));
		assertEquals(6, root.get("v6").getAsJsonObject().get("truc").getAsJsonObject().get("c").getAsInt());
	}
	
	public void testGetTypeByFilename() throws IOException {
		assertEquals(ConfigurationFileType.JSON, ConfigurationFileType.getTypeByFilename(new File("example.json")));
		assertEquals(ConfigurationFileType.YAML, ConfigurationFileType.getTypeByFilename(new File("/example.YAML")));
		assertEquals(ConfigurationFileType.INI, ConfigurationFileType.getTypeByFilename(new File("c:\\windows\\example.ini")));
		assertEquals(ConfigurationFileType.PROPERTY, ConfigurationFileType.getTypeByFilename(new File("example.property")));
		
		Arrays.asList(ConfigurationFileType.CONFIG_FILE_EXTENTIONS).forEach(ext -> {
			assertNotNull(ConfigurationFileType.getTypeByFilename(new File("example." + ext)));
		});
	}
	
	public void testINILoading() throws IOException {
		StringWriter strOut = new StringWriter();
		PrintWriter pw = new PrintWriter(strOut);
		pw.println("[mnemonic]");
		pw.println("v1=a");
		pw.println("v2 = 2");
		pw.println("v3 = true");
		pw.println("v4 = \"4\"");
		pw.println("v5: [\"b\"]");
		pw.println("v6={truc={\"c\":6}}");
		
		HashMap<String, JsonObject> datas = ConfigurationFileType.INI.getContent(new StringReader(strOut.toString()));
		
		assertTrue(datas.containsKey("mnemonic"));
		
		JsonObject root = datas.get("mnemonic");
		
		assertEquals(6, root.size());
		for (int pos = 1; pos <= root.size(); pos++) {
			assertTrue(root.has("v" + pos));
		}
		
		assertEquals("a", root.get("v1").getAsString());
		assertEquals(2, root.get("v2").getAsInt());
		assertEquals(true, root.get("v3").getAsBoolean());
		assertEquals("4", root.get("v4").getAsString());
		
		assertTrue(root.get("v5").isJsonArray());
		assertEquals(1, root.get("v5").getAsJsonArray().size());
		assertEquals("b", root.get("v5").getAsJsonArray().get(0).getAsString());
		
		assertTrue(root.get("v6").isJsonObject());
		assertTrue(root.get("v6").getAsJsonObject().has("truc"));
		assertTrue(root.get("v6").getAsJsonObject().get("truc").getAsJsonObject().has("c"));
		assertEquals(6, root.get("v6").getAsJsonObject().get("truc").getAsJsonObject().get("c").getAsInt());
	}
	
}
