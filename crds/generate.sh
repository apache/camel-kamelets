#!/bin/bash

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

location=$(dirname $0)

deploy_crd_file() {
  source=$1
  # Make a copy to serve as the base for post-processing
  cp "$source" "${source}.orig"
  # Post-process source
  cat $location/header.txt > "$source"
  echo "" >> "$source"
  cat ${source}.orig >> "$source"

  for dest in "${@:2}"; do
    cp "$source" "$dest"
  done

  # Remove the copy as no longer required
  rm -f "${source}.orig"
}

go install sigs.k8s.io/controller-tools/cmd/controller-gen@v0.17.2
$(go env GOPATH)/bin/controller-gen object paths=./...
$(go env GOPATH)/bin/controller-gen crd paths=./...
# Let's also run some test to validate the project
go test ./...
go build ./...

deploy_crd_file ./config/crd/camel.apache.org_kamelets.yaml
