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

import org.apache.log4j.Logger;

class ClassConfigurationSignature {
	private static Logger log = Logger.getLogger(ClassConfigurationSignature.class);
	
	// TODO get all ConfigurableVar (+ names)
	// TODO + var validation
	/*
	 * There are several methods available in the Reflection API that can be used to retrieve annotations.
	 * The behavior of the methods that return a single annotation, such as AnnotatedElement.getAnnotation(Class<T>),
	 * are unchanged in that they only return a single annotation if one annotation of the requested type is present.
	 * If more than one annotation of the requested type is present, you can obtain them by first getting their container annotation.
	 * In this way, legacy code continues to work. Other methods were introduced in Java SE 8 that scan through the container annotation
	 * to return multiple annotations at once, such as AnnotatedElement.getAnnotationsByType(Class<T>).
	 * See the AnnotatedElement class specification for information on all of the available methods.
	 * */
	
	// TODO + var transformation (raw conf -> raw object)
	
	// TODO @after inject conf
	// TODO @before update conf (and checks if its updatable)
	// TODO @after update conf (and checks if its updatable)
	
	// TODO get updatable
	// TODO get name
	// TODO get SingleInstance
	
	ClassConfigurationSignature() {
		// TODO Auto-generated constructor stub
	}
	
	// TODO create Conf API + blank conf creator
}
