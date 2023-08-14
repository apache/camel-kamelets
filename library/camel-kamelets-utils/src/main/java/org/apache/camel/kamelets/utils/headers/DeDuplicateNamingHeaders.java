/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.kamelets.utils.headers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Processor;

public class DeDuplicateNamingHeaders implements Processor {

	String prefix;
	String renamingPrefix;
	String selectedHeaders;
	String mode = "all";

	/**
	 * Default constructor.
	 */
	public DeDuplicateNamingHeaders() {
	}

	/**
	 * Constructor using fields.
	 * 
	 * @param prefix         a prefix to find all the headers to rename.
	 * @param renamingPrefix the renaming prefix to use on all the matching headers
	 */
	public DeDuplicateNamingHeaders(String prefix, String renamingPrefix, String selectedHeaders, String mode) {
		this.prefix = prefix;
		this.renamingPrefix = renamingPrefix;
		this.selectedHeaders = selectedHeaders;
		this.mode = mode;
	}

	public void process(Exchange ex) throws InvalidPayloadException {
		Map<String, Object> originalHeaders = ex.getMessage().getHeaders();
		Map<String, Object> newHeaders = new HashMap<>();
	    Iterator<Map.Entry<String, Object>> iterator = originalHeaders.entrySet().iterator();
	    while (iterator.hasNext()) {
	        Map.Entry<String, Object> entry = iterator.next();
			String key = entry.getKey();
			Object val = entry.getValue();
			if (prefix != null && mode.equalsIgnoreCase("all")) {
				if (key.startsWith(prefix)) {
					String newKey = key.replaceFirst(prefix, renamingPrefix);
					String subKey = newKey.substring(renamingPrefix.length());
					String suffix = subKey.toUpperCase();
					newHeaders.put(renamingPrefix + suffix, val);
					iterator.remove();
				}
			} else {
				if (selectedHeaders != null && mode.equalsIgnoreCase("filtering")) {
					List<String> headerList = Arrays.asList(selectedHeaders.split(","));
					for (Iterator<String> iteratorHeader = headerList.iterator(); iteratorHeader.hasNext();) {
						String header = iteratorHeader.next();
						if (key.equalsIgnoreCase(header)) {
							String newKey = key.replaceFirst(prefix, renamingPrefix);
							String subKey = newKey.substring(renamingPrefix.length());
							String suffix = subKey.toUpperCase();
							newHeaders.put(renamingPrefix + suffix, val);
							iterator.remove();
						} 
					}

				}
	       }
	    }
		originalHeaders.putAll(newHeaders);
		ex.getMessage().setHeaders(originalHeaders);
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setRenamingPrefix(String renamingPrefix) {
		this.renamingPrefix = renamingPrefix;
	}

	public void setSelectedHeaders(String selectedHeaders) {
		this.selectedHeaders = selectedHeaders;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}	

}
