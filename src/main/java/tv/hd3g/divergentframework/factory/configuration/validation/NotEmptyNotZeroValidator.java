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

import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NotEmptyNotZeroValidator extends NotNullValidator {
	
	public Predicate<JsonElement> getValidator() {
		return super.getValidator().and(t -> {
			if (t.isJsonPrimitive()) {
				JsonPrimitive jp = t.getAsJsonPrimitive();
				if (jp.isString()) {
					return jp.getAsString().equals("") == false;
				} else if (jp.isNumber()) {
					return jp.getAsNumber().intValue() != 0;
				}
			} else if (t.isJsonArray()) {
				return t.getAsJsonArray().size() > 0;
			} else if (t.isJsonObject()) {
				return t.getAsJsonObject().size() > 0;
			}
			return false;
		});
	}
}
