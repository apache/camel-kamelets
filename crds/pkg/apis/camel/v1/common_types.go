/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package v1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// +kubebuilder:object:generate=false

// ResourceCondition is a common type for all conditions.
type ResourceCondition interface {
	GetType() string
	GetStatus() corev1.ConditionStatus
	GetLastUpdateTime() metav1.Time
	GetLastTransitionTime() metav1.Time
	GetReason() string
	GetMessage() string
}

// RawMessage is a raw encoded JSON value.
// It implements Marshaler and Unmarshaler and can
// be used to delay JSON decoding or precompute a JSON encoding.
// +kubebuilder:validation:Type=""
// +kubebuilder:validation:Format=""
// +kubebuilder:pruning:PreserveUnknownFields
type RawMessage []byte

// SourceSpec defines the configuration for one or more routes to be executed in a certain Camel DSL language.
type SourceSpec struct {
	// contains configuration related to the source code
	DataSpec `json:",inline"`
	// specify which is the language (Camel DSL) used to interpret this source code
	Language Language `json:"language,omitempty"`
	// Loader is an optional id of the org.apache.camel.k.RoutesLoader that will
	// interpret this source at runtime
	Loader string `json:"loader,omitempty"`
	// Interceptors are optional identifiers the org.apache.camel.k.RoutesLoader
	// uses to pre/post process sources
	Interceptors []string `json:"interceptors,omitempty"`
	// Type defines the kind of source described by this object
	Type SourceType `json:"type,omitempty"`
	// List of property names defined in the source (e.g. if type is "template")
	PropertyNames []string `json:"property-names,omitempty"`
	// True if the spec is generated from a Kamelet
	FromKamelet bool `json:"from-kamelet,omitempty"`
}

// SourceType represents an available source type.
type SourceType string

const (
	// SourceTypeDefault is used to represent a source code.
	SourceTypeDefault SourceType = ""
	// SourceTypeTemplate is used to represent a template.
	SourceTypeTemplate SourceType = "template"
	// SourceTypeErrorHandler is used to represent an error handler.
	SourceTypeErrorHandler SourceType = "errorHandler"
)

// DataSpec represents the way the source is materialized in the running `Pod`.
type DataSpec struct {
	// the name of the specification
	Name string `json:"name,omitempty"`
	// the path where the file is stored
	Path string `json:"path,omitempty"`
	// the source code (plain text)
	Content string `json:"content,omitempty"`
	// the source code (binary)
	RawContent []byte `json:"rawContent,omitempty"`
	// the confimap reference holding the source content
	ContentRef string `json:"contentRef,omitempty"`
	// the confimap key holding the source content
	ContentKey string `json:"contentKey,omitempty"`
	// the content type (typically text or binary)
	ContentType string `json:"contentType,omitempty"`
	// if the content is compressed (base64 encrypted)
	Compression bool `json:"compression,omitempty"`
}

// Language represents a supported language (Camel DSL).
type Language string

const (
	// LanguageJavaSource used for Java.
	LanguageJavaSource Language = "java"
	// LanguageGroovy used for Groovy.
	LanguageGroovy Language = "groovy"
	// LanguageJavaScript  used for Javascript.
	LanguageJavaScript Language = "js"
	// LanguageXML used for XML.
	LanguageXML Language = "xml"
	// LanguageKotlin used for Kotlin.
	LanguageKotlin Language = "kts"
	// LanguageYaml used for YAML.
	LanguageYaml Language = "yaml"
	// LanguageKamelet used for Kamelets.
	LanguageKamelet Language = "kamelet"
	// LanguageJavaShell used for Java Shell.
	LanguageJavaShell Language = "jsh"
)
