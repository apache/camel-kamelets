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

package org.apache.camel.kamelets.utils.format.spi.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Data type annotation defines a data type with its component scheme, a name and optional media types.
 * <p/>
 * The annotation is used by specific classpath scanning data type loaders to automatically add the data types to
 * a registry.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE })
public @interface DataType {

    String DEFAULT_SCHEME = "camel";

    /**
     * Camel component scheme. Specifies whether a data type is component specific.
     * @return the data type scheme.
     */
    String scheme() default DEFAULT_SCHEME;

    /**
     * Data type name. Identifies the data type. Should be unique in combination with scheme.
     * @return the data type name.
     */
    String name();

    /**
     * The media type associated with this data type.
     * @return the media type or empty string as default.
     */
    String mediaType() default "";
}
