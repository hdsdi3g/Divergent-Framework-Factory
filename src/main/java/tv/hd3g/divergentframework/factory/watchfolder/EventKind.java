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
package tv.hd3g.divergentframework.factory.watchfolder;

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

public enum EventKind {
	
	CREATE, MODIFY, DELETE, FIRST_DETECTION;
	
	public static EventKind onWatchEvent(WatchEvent.Kind<?> kind) {
		if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
			return CREATE;
		} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
			return MODIFY;
		} else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
			return DELETE;
		}
		return null;
	}
	
}
