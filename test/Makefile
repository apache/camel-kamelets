# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

camel-version := 4.0.0
camel-kamelets-version := 4.1.0-SNAPSHOT
kamelet-api-version := v1alpha1
kamelets-local-dir := ../../../kamelets
test := .

YAKS_RUN_OPTIONS := --timeout=15m --local -e YAKS_CAMELK_MAX_ATTEMPTS=10 -e YAKS_JBANG_CAMEL_VERSION=$(camel-version) -e YAKS_JBANG_KAMELETS_VERSION=$(camel-kamelets-version) -e YAKS_JBANG_KAMELETS_LOCAL_DIR=$(kamelets-local-dir) -e YAKS_KAMELET_API_VERSION=$(kamelet-api-version)

default: all

check-setup:
	@java -version
	@jbang version
	@yaks version
	@jbang camel@apache/camel --version
	@echo "Test setup - OK"

all: check-setup
	@echo "Running YAKS tests"
	@echo #
	yaks run . $(YAKS_RUN_OPTIONS)

yaks: check-setup
	@echo "Running YAKS test"
	@echo #
	yaks run $(test) $(YAKS_RUN_OPTIONS)

.PHONY: all, yaks
