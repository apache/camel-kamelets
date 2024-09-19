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

package camel

import (
	"io/fs"
	"os"
	"path"
	"path/filepath"
	"strings"
	"testing"

	v1 "github.com/apache/camel-kamelets/pkg/apis/camel/v1"
	"github.com/stretchr/testify/assert"
	"gopkg.in/yaml.v3"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestBasicKameletSpec(t *testing.T) {
	k := v1.Kamelet{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "my-kamelet",
			Namespace: "my-ns",
		},
		Spec: v1.KameletSpec{KameletSpecBase: v1.KameletSpecBase{Sources: []v1.SourceSpec{{DataSpec: v1.DataSpec{}}}}},
	}
	cloned := k.DeepCopy()
	assert.Equal(t, cloned, &k)
}

func TestLibraryKamelets(t *testing.T) {
	dir := "../../../../kamelets"
	// We can use all the Kamelets in the catalog in order to validate the spec and viceversa, validate the Kamelets against the existing spec
	err := filepath.WalkDir(dir, func(p string, f fs.DirEntry, err error) error {
		assert.NoError(t, err)
		if !(strings.HasSuffix(f.Name(), ".yaml") || strings.HasSuffix(f.Name(), ".yml")) {
			return nil
		}
		var kamelet *v1.Kamelet
		yamlContent, err := os.ReadFile(path.Join(dir, f.Name()))
		assert.NotEmpty(t, yamlContent)
		assert.NoError(t, err)
		err = yaml.Unmarshal(yamlContent, &kamelet)
		assert.NoError(t, err)
		assert.NotNil(t, kamelet, "Could not marshal Kamelet %s", f.Name())
		return nil
	})
	assert.NoError(t, err)
}
