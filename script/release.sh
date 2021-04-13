#!/bin/sh

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
rootdir=$location/../

if [ "$#" -ne 2 ]; then
  echo "usage: $0 version remote"
  exit 1
fi

version=$1
remote=$2

cd $rootdir
echo "Releasing $version..."

for file in *.kamelet.yaml; do
  if ! grep -q "camel.apache.org/catalog.version" "$file"
  then
    echo "Adding release information to $file..."
    sed -i "s/^  annotations:.*$/  annotations:\n    camel.apache.org\/catalog.version: \"$version\"/" "${file}"
  fi
done

staging=staging-$version
git branch -D ${staging} || true
git checkout -b ${staging}
git add * || true
git commit -a -m "Release ${version}"
git tag --force ${version} ${staging}
git push --force ${remote} ${version}
echo "Tag ${version} pushed to ${remote}"
