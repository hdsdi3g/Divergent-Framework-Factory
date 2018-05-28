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
package tv.hd3g.divergentframework.factory.configuration.validation;

import java.io.File;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class FileIsDirectoryAndExistsValidator extends NotNullValidator {
	
	public Predicate<JsonElement> getValidator() {
		return super.getValidator().and(t -> {
			if (t.isJsonPrimitive() == false) {
				return false;
			}
			
			JsonPrimitive jp = t.getAsJsonPrimitive();
			if (jp.isString() == false) {
				return false;
			}
			
			File f = new File(jp.getAsString());
			
			return f.exists() && f.isDirectory() && f.canRead();
		});
	}
}
