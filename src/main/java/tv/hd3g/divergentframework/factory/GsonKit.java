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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class GsonKit {
	
	private static Logger log = Logger.getLogger(GsonKit.class);
	
	public final static Type type_ArrayList_String = new TypeToken<ArrayList<String>>() {
	}.getType();
	public final static Type type_ArrayList_InetAddr = new TypeToken<ArrayList<InetAddress>>() {
	}.getType();
	public final static Type type_ArrayList_InetSocketAddr = new TypeToken<ArrayList<InetSocketAddress>>() {
	}.getType();
	public final static Type type_HashSet_String = new TypeToken<HashSet<String>>() {
	}.getType();
	public final static Type type_LinkedHashMap_String_String = new TypeToken<LinkedHashMap<String, String>>() {
	}.getType();
	public final static Type type_HashMap_String_String = new TypeToken<HashMap<String, String>>() {
	}.getType();
	public final static Type type_Map_String_Object = new TypeToken<Map<String, Object>>() {
	}.getType();
	
	private class De_Serializator {
		private Type type;
		private Object typeAdapter;
		
		private <T> De_Serializator(Type type, GsonDeSerializer<T> typeAdapter) {
			
			log.debug("Declare de/serializer " + type.getTypeName());
			
			this.type = type;
			if (log.isTraceEnabled()) {
				this.typeAdapter = new GsonDeSerializer<T>() {
					public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
						JsonElement result = typeAdapter.serialize(src, typeOfSrc, context);
						log.trace("Serialize to " + result.toString());
						return result;
					}
					
					public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
						log.trace("Deserialize from " + json.toString());
						return typeAdapter.deserialize(json, typeOfT, context);
					}
				};
			} else {
				this.typeAdapter = typeAdapter;
			}
		}
		
		private <T> De_Serializator(Type type, JsonSerializer<T> typeAdapter) {
			log.debug("Declare serializer " + type.getTypeName());
			
			this.type = type;
			
			if (log.isTraceEnabled()) {
				this.typeAdapter = new JsonSerializer<T>() {
					public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
						JsonElement result = typeAdapter.serialize(src, typeOfSrc, context);
						log.trace("Serialize to " + result.toString());
						return result;
					}
				};
			} else {
				this.typeAdapter = typeAdapter;
			}
		}
		
		private <T> De_Serializator(Type type, JsonDeserializer<T> typeAdapter) {
			log.debug("Declare deserializer " + type.getTypeName());
			
			this.type = type;
			if (log.isTraceEnabled()) {
				this.typeAdapter = new JsonDeserializer<T>() {
					public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
						log.trace("Deserialize from " + json.toString());
						return typeAdapter.deserialize(json, typeOfT, context);
					}
				};
			} else {
				this.typeAdapter = typeAdapter;
			}
		}
	}
	
	private GsonIgnoreStrategy ignore_strategy;
	private Gson gson_simple;
	private ArrayList<De_Serializator> gson_simple_serializator;
	private ArrayList<De_Serializator> gson_full_serializator;
	private boolean full_pretty_printing = false;
	
	/**
	 * @return Gson with GsonIgnoreStrategy, SerializeNulls and BaseSerializers
	 */
	GsonKit() {
		ignore_strategy = new GsonIgnoreStrategy();
		
		gson_simple_serializator = new ArrayList<>();
		gson_full_serializator = new ArrayList<>();
		
		/**
		 * JsonArray
		 */
		registerGsonSimpleDeSerializer(JsonArray.class, JsonArray.class, src -> {
			if (src == null) {
				return null;
			}
			return src;
		}, json -> {
			try {
				return json.getAsJsonArray();
			} catch (Exception e) {
				log.error("Can't deserialize JsonArray", e);
				return null;
			}
		});
		
		/**
		 * JsonObject
		 */
		registerGsonSimpleDeSerializer(JsonObject.class, JsonObject.class, src -> {
			if (src == null) {
				return null;
			}
			return src;
		}, json -> {
			try {
				return json.getAsJsonObject();
			} catch (Exception e) {
				log.error("Can't deserialize JsonObject", e);
				return null;
			}
		});
		
		/**
		 * InetAddress
		 */
		registerGsonSimpleDeSerializer(InetAddress.class, InetAddress.class, src -> {
			return new JsonPrimitive(src.getHostAddress());
		}, json -> {
			try {
				return InetAddress.getByName(json.getAsString());
			} catch (UnknownHostException e) {
				throw new JsonParseException(json.getAsString(), e);
			}
		});
		
		/**
		 * InetSocketAddress
		 */
		registerGsonSimpleDeSerializer(InetSocketAddress.class, InetSocketAddress.class, src -> {
			JsonObject jo = new JsonObject();
			jo.addProperty("addr", src.getHostString());
			jo.addProperty("port", src.getPort());
			return jo;
		}, json -> {
			JsonObject jo = json.getAsJsonObject();
			return new InetSocketAddress(jo.get("addr").getAsString(), jo.get("port").getAsInt());
		});
		
		/**
		 * URL
		 */
		registerGsonSimpleDeSerializer(URL.class, URL.class, src -> {
			return new JsonPrimitive(src.toString());
		}, json -> {
			try {
				return new URL(json.getAsString());
			} catch (MalformedURLException e) {
				throw new JsonParseException(json.getAsString(), e);
			}
		});
		
		/**
		 * Properties
		 */
		registerGsonSimpleDeSerializer(Properties.class, Properties.class, src -> {
			StringWriter pw = new StringWriter();
			try {
				src.store(pw, null);
			} catch (IOException e) {
				log.warn("Can't serialize properties", e);
			}
			pw.flush();
			
			return new JsonPrimitive(pw.toString());
		}, json -> {
			Properties result = new Properties();
			StringReader sr = new StringReader(json.getAsString());
			try {
				result.load(sr);
			} catch (IOException e) {
				log.warn("Can't deserialize properties", e);
			}
			
			return result;
		});
		
		/**
		 * InternetAddress
		 */
		registerGsonSimpleDeSerializer(InternetAddress.class, InternetAddress.class, src -> {
			return new JsonPrimitive(src.toString());
		}, json -> {
			try {
				return new InternetAddress(json.getAsString());
			} catch (AddressException e) {
				throw new JsonParseException(json.getAsString(), e);
			}
		});
		
		/**
		 * Locale
		 */
		registerGsonSimpleDeSerializer(Locale.class, Locale.class, src -> {
			return new JsonPrimitive(src.toLanguageTag());
		}, json -> {
			return Locale.forLanguageTag(json.getAsString());
		});
		
		/*
		 * 	public class Serializer implements JsonSerializer<SelfSerializing> {
		public JsonElement serialize(SelfSerializing src, Type typeOfSrc, JsonSerializationContext context) {
		return selfserializer.serialize(src, MyDMAM.gson_kit.getGson());
		}
		}
		
		public class Deserializer implements JsonDeserializer<SelfSerializing> {
		public SelfSerializing deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		return (SelfSerializing) selfserializer.deserialize(ContainerOperations.getJsonObject(json, false), MyDMAM.gson_kit.getGson());
		}
		}
		
		registerDeSerializer(, .class, src -> {
		}, json -> {
		});
		*/
		
		rebuildGsonSimple();
	}
	
	synchronized void rebuildGsonSimple() {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		
		gson_simple_serializator.forEach(ser -> {
			builder.registerTypeAdapter(ser.type, ser.typeAdapter);
		});
		
		if (full_pretty_printing) {
			builder.setPrettyPrinting();
		}
		
		gson_simple = builder.create();
	}
	
	public Gson getGsonSimple() {
		return gson_simple;
	}
	
	private Gson gson_full;
	private Gson gson_full_pretty;
	
	private synchronized void rebuildGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		
		gson_simple_serializator.forEach(ser -> {
			builder.registerTypeAdapter(ser.type, ser.typeAdapter);
		});
		gson_full_serializator.forEach(ser -> {
			builder.registerTypeAdapter(ser.type, ser.typeAdapter);
		});
		
		if (full_pretty_printing) {
			builder.setPrettyPrinting();
		}
		
		gson_full = builder.create();
		
		builder.setPrettyPrinting();
		gson_full_pretty = builder.create();
	}
	
	/**
	 * @return GsonSimple + all actual registerTypeAdapter()
	 */
	public Gson getGson() {
		if (gson_full == null) {
			rebuildGson();
		}
		return gson_full;
	}
	
	/**
	 * @return GsonSimple + all actual registerTypeAdapter() + pretty printing
	 */
	public Gson getGsonPretty() {
		if (gson_full_pretty == null) {
			rebuildGson();
		}
		return gson_full_pretty;
	}
	
	public synchronized <T> GsonKit registerDeserializer(Type type, Class<T> dest_type, Function<JsonElement, T> deserializer) {
		gson_full_serializator.add(new De_Serializator(type, new JsonDeserializer<T>() {
			public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				try {
					return deserializer.apply(json);
				} catch (Exception e) {
					log.error("Can't deserialize to " + dest_type.getName(), e);
					return null;
				}
			}
		}));
		gson_full = null;
		gson_full_pretty = null;
		return this;
	}
	
	public synchronized <T> GsonKit registerSerializer(Type type, Class<T> source_type, Function<T, JsonElement> serializer) {
		gson_full_serializator.add(new De_Serializator(type, new JsonSerializer<T>() {
			public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
				try {
					return serializer.apply(src);
				} catch (Exception e) {
					log.error("Can't serialize from " + source_type.getName(), e);
					return null;
				}
			}
		}));
		gson_full = null;
		gson_full_pretty = null;
		return this;
	}
	
	public synchronized <T> GsonKit registerDeSerializer(Type type, Class<T> object_type, Function<T, JsonElement> adapter_serializer, Function<JsonElement, T> adapter_deserializer) {
		gson_full_serializator.add(new De_Serializator(type, makeDeSerializer(object_type, adapter_serializer, adapter_deserializer)));
		gson_full = null;
		gson_full_pretty = null;
		return this;
	}
	
	<T> void registerGsonSimpleDeSerializer(Type type, Class<T> object_type, Function<T, JsonElement> adapter_serializer, Function<JsonElement, T> adapter_deserializer) {
		gson_simple_serializator.add(new De_Serializator(type, makeDeSerializer(object_type, adapter_serializer, adapter_deserializer)));
	}
	
	private <T> GsonDeSerializer<T> makeDeSerializer(Class<T> object_type, Function<T, JsonElement> adapter_serializer, Function<JsonElement, T> adapter_deserializer) {
		return new GsonDeSerializer<T>() {
			public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
				try {
					return adapter_serializer.apply(src);
				} catch (Exception e) {
					log.error("Can't serialize from " + object_type.getName(), e);
					return null;
				}
			}
			
			public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				try {
					return adapter_deserializer.apply(json);
				} catch (Exception e) {
					log.error("Can't deserialize from " + object_type.getName(), e);
					return null;
				}
			}
		};
	}
	
	public void setFullPrettyPrinting() {
		if (full_pretty_printing == false) {
			synchronized (this) {
				full_pretty_printing = true;
				gson_full = null;
				gson_full_pretty = null;
			}
			rebuildGsonSimple();
		}
	}
	
}
