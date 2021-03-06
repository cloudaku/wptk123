/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.cf.manifest.v2.utils;

public class ManifestConstants {

	/* Manifest error messages */
	public static String UNEXPECTED_INPUT_ERROR = "Unexpected token around line {0}.";

	public static String UNSUPPORTED_TOKEN_ERROR = "Unsupported token around line {0}.";

	public static String ILLEGAL_ITEM_TOKEN = "Unexpected token \"{1}\" around line {0}. Instead, expected a string literal.";

	public static String ILLEGAL_ITEM_TOKEN_MIX = "Unexpected item token \"{1}\" around line {0}. Instead, expected a string literal mapping, i.e. \"property : value\".";

	public static String ILLEGAL_MAPPING_TOKEN_MIX = "Unexpected string literal mapping \"{1}\" around line {0}. Instead, expected an item token, i.e. \" - property: value\".";

	public static String ILLEGAL_MAPPING_TOKEN = "Unexpected token \"{1}\" around line {0}. Instead, expected \":\".";

	public static String DUPLICATE_MAPPING_TOKEN = "Unexpected token \"{1}\" around line {0}. Instead, expected a string literal or item symbol \"- \".";

	public static String MISSING_ITEM_ACCESS = "Expected {0} to have at least {1} item members.";

	public static String MISSING_MEMBER_ACCESS = "Expected {0} to have a member \"{1}\".";

	public static String MISSING_MAPPING_ACCESS = "Expected {0} to have a value.";

	/* Manifest constants */
	public static final String MANIFEST_FILE_NAME = "manifest.yml"; //$NON-NLS-1$

	public static String MANIFEST_APPLICATIONS = "applications";

	public static String MANIFEST_APPLICATION_NAME = "name";
}
