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

package org.apache.camel.kamelets.utils.format.converter.utils;

import java.util.Objects;

import org.apache.camel.Exchange;

public final class PojoHelper {
    private PojoHelper() {
    }

    public static boolean isString(Class<?> type) {
        return String.class.isAssignableFrom(type);
    }

    public static boolean isNumber(Class<?> type) {
        return Number.class.isAssignableFrom(type)
                || int.class.isAssignableFrom(type)
                || long.class.isAssignableFrom(type)
                || short.class.isAssignableFrom(type)
                || char.class.isAssignableFrom(type)
                || float.class.isAssignableFrom(type)
                || double.class.isAssignableFrom(type);
    }

    public static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || (type.isArray() && type.getComponentType().isPrimitive())
                || char.class.isAssignableFrom(type) || Character.class.isAssignableFrom(type)
                || byte.class.isAssignableFrom(type) || Byte.class.isAssignableFrom(type)
                || boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type);
    }

    public static boolean isPojo(Class<?> type) {
        Package pkg = type.getPackage();
        if (pkg != null) {
            if (pkg.getName().startsWith("java")
                    || pkg.getName().startsWith("javax")
                    || pkg.getName().startsWith("com.sun")
                    || pkg.getName().startsWith("com.oracle")) {
                return false;
            }
        }

        if (isNumber(type)) {
            return false;
        }
        if (isPrimitive(type)) {
            return false;
        }
        if (isString(type)) {
            return false;
        }

        return true;
    }

    public static boolean hasProperty(Exchange exchange, String name) {
        return exchange.getProperties().containsKey(name);
    }

    public static boolean hasProperty(Exchange exchange, String name, Object value) {
        return Objects.equals(
                value,
                exchange.getProperty(name, value.getClass()));
    }

    public static boolean hasHeader(Exchange exchange, String name, Object value) {
        return Objects.equals(
                value,
                exchange.getMessage().getHeader(name, value.getClass()));
    }
}
