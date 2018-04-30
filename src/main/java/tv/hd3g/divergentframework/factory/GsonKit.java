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

import java.awt.Color;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.google.gson.JsonParser;
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
				this.typeAdapter = (JsonSerializer<T>) (src, typeOfSrc, context) -> {
					JsonElement result = typeAdapter.serialize(src, typeOfSrc, context);
					log.trace("Serialize to " + result.toString());
					return result;
				};
			} else {
				this.typeAdapter = typeAdapter;
			}
		}
		
		private <T> De_Serializator(Type type, JsonDeserializer<T> typeAdapter) {
			log.debug("Declare deserializer " + type.getTypeName());
			
			this.type = type;
			if (log.isTraceEnabled()) {
				this.typeAdapter = (JsonDeserializer<T>) (json, typeOfT, context) -> {
					log.trace("Deserialize from " + json.toString());
					return typeAdapter.deserialize(json, typeOfT, context);
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
	public static final JsonParser parser = new JsonParser();
	
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
		
		/**
		 * Color
		 */
		registerGsonSimpleDeSerializer(Color.class, Color.class, src -> {
			StringBuilder sb = new StringBuilder();
			sb.append("RGBA:");
			toHex(src.getRed(), sb);
			toHex(src.getGreen(), sb);
			toHex(src.getBlue(), sb);
			toHex(src.getAlpha(), sb);
			return new JsonPrimitive(sb.toString());
		}, json -> {
			String hex = json.getAsString().substring(5);
			return new Color(fromHexMax1Byte(hex, 0), fromHexMax1Byte(hex, 2), fromHexMax1Byte(hex, 4), fromHexMax1Byte(hex, 6));
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
	
	private static void toHex(int v, StringBuilder sb) {
		if (v < 16) {
			sb.append(0);
		}
		sb.append(Integer.toString(v, 16).toUpperCase());
	}
	
	/**
	 * @param s from 00 to FF
	 */
	private static final int fromHexMax1Byte(String s, int from) {
		if (s == null) {
			return 0;
		}
		return ((Character.digit(s.charAt(from), 16) << 4) + Character.digit(s.charAt(from + 1), 16));
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
		gson_full_serializator.add(new De_Serializator(type, (JsonDeserializer<T>) (json, typeOfT, context) -> {
			try {
				return deserializer.apply(json);
			} catch (Exception e) {
				log.error("Can't deserialize to " + dest_type.getName(), e);
				return null;
			}
		}));
		gson_full = null;
		gson_full_pretty = null;
		return this;
	}
	
	public synchronized <T> GsonKit registerSerializer(Type type, Class<T> source_type, Function<T, JsonElement> serializer) {
		gson_full_serializator.add(new De_Serializator(type, (JsonSerializer<T>) (src, typeOfSrc, context) -> {
			try {
				return serializer.apply(src);
			} catch (Exception e) {
				log.error("Can't serialize from " + source_type.getName(), e);
				return null;
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
	
	public Stream<Type> getAllSerializedClasses(boolean only_gson_simple) {
		if (only_gson_simple) {
			return gson_simple_serializator.stream().map(s -> {
				return s.type;
			});
		} else {
			return Stream.concat(gson_simple_serializator.stream(), gson_full_serializator.stream()).distinct().map(s -> {
				return s.type;
			});
		}
	}
	
	private <T> GsonDeSerializer<T> makeDeSerializer(Class<T> object_type, Function<T, JsonElement> adapter_serializer, Function<JsonElement, T> adapter_deserializer) {
		return new GsonDeSerializer<>() {
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
	
	@FunctionalInterface
	public static interface CompareTo<R extends JsonElement, K> {
		
		void accept(R relative_to, K key, JsonElement value);
	}
	
	/**
	 * Don't touch current, but It can be updated via events.
	 */
	public static final void jsonCompare(JsonElement original, JsonElement newer, CompareTo<JsonObject, String> onAddNewerToCurrentMap, CompareTo<JsonObject, String> onRemoveInCurrentMap, CompareTo<JsonArray, Void> onAddNewerToCurrentList, CompareTo<JsonArray, Integer> onRemoveInCurrentList) {
		if (original.isJsonNull()) {
			throw new RuntimeException("Can't compare something with a JsonNull");
		} else if (original.isJsonPrimitive()) {
			throw new RuntimeException("Can't compare a JsonPrimitive with something");
		} else if (original.isJsonObject()) {
			JsonObject jo_current = original.getAsJsonObject();
			if (newer.isJsonArray()) {
				throw new RuntimeException("Can't inject a JsonArray in place of a JsonObject");
			} else if (newer.isJsonObject()) {
				JsonObject jo_newer = newer.getAsJsonObject();
				List<String> current_items = jo_current.keySet().stream().collect(Collectors.toList());
				
				current_items.stream().filter(key -> jo_newer.has(key)).forEach(key -> {
					JsonElement current_content_for_key = jo_current.get(key);
					JsonElement newer_content_for_key = jo_newer.get(key);
					
					if (newer_content_for_key == null) {
						/**
						 * Current will not change.
						 */
					} else if (newer_content_for_key.isJsonArray()) {
						if (current_content_for_key.isJsonArray()) {
							jsonCompare(current_content_for_key, newer_content_for_key, onAddNewerToCurrentMap, onRemoveInCurrentMap, onAddNewerToCurrentList, onRemoveInCurrentList);
						} else if (current_content_for_key.isJsonObject()) {
							onAddNewerToCurrentMap.accept(jo_current, key, newer_content_for_key);
						} else if (current_content_for_key.isJsonNull()) {
							onAddNewerToCurrentMap.accept(jo_current, key, newer_content_for_key);
						} else if (current_content_for_key.isJsonPrimitive()) {
							onAddNewerToCurrentMap.accept(jo_current, key, newer_content_for_key);
						}
					} else if (newer_content_for_key.isJsonObject()) {
						if (current_content_for_key.isJsonArray()) {
							onAddNewerToCurrentMap.accept(jo_current, key, newer_content_for_key);
						} else if (current_content_for_key.isJsonObject()) {
							jsonCompare(current_content_for_key, newer_content_for_key, onAddNewerToCurrentMap, onRemoveInCurrentMap, onAddNewerToCurrentList, onRemoveInCurrentList);
						} else if (current_content_for_key.isJsonNull()) {
							onAddNewerToCurrentMap.accept(jo_current, key, newer_content_for_key);
						} else if (current_content_for_key.isJsonPrimitive()) {
							onAddNewerToCurrentMap.accept(jo_current, key, newer_content_for_key);
						}
					} else if (newer_content_for_key.isJsonNull()) {
						onRemoveInCurrentMap.accept(jo_current, key, newer_content_for_key);
					} else if (newer_content_for_key.isJsonPrimitive()) {
						onAddNewerToCurrentMap.accept(jo_current, key, newer_content_for_key);
					}
				});
				
				List<String> newer_items = jo_newer.keySet().stream().collect(Collectors.toList());
				newer_items.stream().filter(key -> jo_current.has(key) == false).forEach(key -> {
					onAddNewerToCurrentMap.accept(jo_current, key, jo_newer.get(key));
				});
			} else if (newer.isJsonPrimitive()) {
				throw new RuntimeException("Can't inject a JsonPrimitive in place of a JsonObject");
			} else if (newer.isJsonNull()) {
				/**
				 * Remove all
				 */
				List<String> current_items = jo_current.keySet().stream().collect(Collectors.toList());
				
				current_items.forEach(key -> {
					onRemoveInCurrentMap.accept(jo_current, key, jo_current.get(key));
				});
			}
		} else if (original.isJsonArray()) {
			JsonArray ja_current = original.getAsJsonArray();
			
			if (newer.isJsonArray()) {
				/**
				 * Replace all by newer content
				 */
				for (int pos = ja_current.size() - 1; pos >= 0; pos--) {
					onRemoveInCurrentList.accept(ja_current, pos, ja_current.get(pos));
				}
				
				newer.getAsJsonArray().forEach(new_item -> {
					onAddNewerToCurrentList.accept(ja_current, null, new_item);
				});
				
			} else if (newer.isJsonObject()) {
				throw new RuntimeException("Can't inject a JsonObject in place of a JsonArray");
			} else if (newer.isJsonPrimitive()) {
				/**
				 * Replace all, and add newer item
				 */
				for (int pos = ja_current.size() - 1; pos >= 0; pos--) {
					onRemoveInCurrentList.accept(ja_current, pos, ja_current.get(pos));
				}
				onAddNewerToCurrentList.accept(ja_current, null, newer);
			} else if (newer.isJsonNull()) {
				/**
				 * Remove all
				 */
				for (int pos = ja_current.size() - 1; pos >= 0; pos--) {
					onRemoveInCurrentList.accept(ja_current, pos, ja_current.get(pos));
				}
			}
		}
	}
	
	public enum KeyValueNullContentMergeBehavior {
		KEEP, REMOVE;
	}
	
	/**
	 * Update current with newer
	 * @param null_behavior use REMOVE for remove all Nulls presence.
	 */
	public static final void jsonMerge(JsonElement current, JsonElement newer, KeyValueNullContentMergeBehavior null_behavior) {
		if (current == null) {
			throw new NullPointerException("\"current\" can't to be null");
		}
		if (newer == null) {
			throw new NullPointerException("\"newer\" can't to be null");
		}
		
		if (current.isJsonArray() | current.isJsonObject()) {
			jsonCompare(current, newer, (relative_to, k_to_add, v) -> {
				if (v.isJsonNull()) {
					if (null_behavior == KeyValueNullContentMergeBehavior.KEEP) {
						relative_to.add(k_to_add, v);
					}
				} else {
					relative_to.add(k_to_add, v);
				}
			}, (relative_to, k_to_remove, v) -> {
				if (null_behavior == KeyValueNullContentMergeBehavior.KEEP) {
					relative_to.add(k_to_remove, v);
				} else if (null_behavior == KeyValueNullContentMergeBehavior.REMOVE) {
					relative_to.remove(k_to_remove);
				}
			}, (relative_to, nothing, content_to_add) -> {
				relative_to.add(content_to_add);
			}, (relative_to, pos, content_to_remove) -> {
				relative_to.remove(pos);
			});
		} else if (current.isJsonPrimitive()) {
			throw new RuntimeException("Can't compare Json primitives");
		} else if (current.isJsonNull()) {
			throw new NullPointerException("Current JsonElement is null");
		} else {
			throw new RuntimeException("Invalid Current JsonElement type " + current.getClass().getName());
		}
	}
	
}
