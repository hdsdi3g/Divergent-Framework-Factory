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

public class NotNullValidator extends DefaultValidator {
	
	public Predicate<JsonElement> getValidator() {
		return t -> {
			if (t == null) {
				return false;
			}
			return t.isJsonNull() == false;
		};
	}
}
