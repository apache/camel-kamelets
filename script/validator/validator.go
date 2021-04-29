package main

import (
	"fmt"
	"io/ioutil"
	"os"
	"path"
	"path/filepath"
	"sort"
	"strings"

	camel "github.com/apache/camel-k/pkg/apis/camel/v1alpha1"
	"github.com/bbalet/stopwords"
	perrors "github.com/pkg/errors"
	yamlv3 "gopkg.in/yaml.v3"
	"k8s.io/apimachinery/pkg/api/equality"
	"k8s.io/apimachinery/pkg/apis/meta/v1/unstructured"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/runtime/serializer"
	"k8s.io/apimachinery/pkg/util/json"
	"k8s.io/apimachinery/pkg/util/yaml"
)

func main() {
	if len(os.Args) != 2 {
		println("usage: validator kamelets-path")
		os.Exit(1)
	}

	dir := os.Args[1]

	kamelets := listKamelets(dir)

	errors := verifyFileNames(kamelets)
	errors = append(errors, verifyKameletType(kamelets)...)
	errors = append(errors, verifyAnnotations(kamelets)...)
	errors = append(errors, verifyParameters(kamelets)...)
	errors = append(errors, verifyInvalidContent(kamelets)...)

	for _, err := range errors {
		fmt.Printf("ERROR: %v\n", err)
	}
	if len(errors) > 0 {
		os.Exit(1)
	}
}

func verifyInvalidContent(kamelets []KameletInfo) (errors []error) {
	for _, kamelet := range kamelets {
		ser, err := json.Marshal(&kamelet.Kamelet)
		if err != nil {
			errors = append(errors, perrors.Wrapf(err, "cannot marshal kamelet %q", kamelet.Name))
			continue
		}
		var unstr unstructured.Unstructured
		err = json.Unmarshal(ser, &unstr)
		if err != nil {
			errors = append(errors, perrors.Wrapf(err, "cannot unmarshal kamelet %q", kamelet.Name))
			continue
		}

		file, err := ioutil.ReadFile(kamelet.FileName)
		if err != nil {
			errors = append(errors, perrors.Wrapf(err, "cannot load kamelet %q", kamelet.Name))
			continue
		}
		var yamlFile map[string]interface{}
		err = yamlv3.Unmarshal(file, &yamlFile)
		if err != nil {
			errors = append(errors, perrors.Wrapf(err, "kamelet %q is not a valid YAML file", kamelet.Name))
			continue
		}
		jsonFile, err := yaml.ToJSON(file)
		if err != nil {
			errors = append(errors, perrors.Wrapf(err, "cannot convert kamelet %q to JSON", kamelet.Name))
			continue
		}
		unstrFile := unstructured.Unstructured{}
		err = json.Unmarshal(jsonFile, &unstrFile)
		if err != nil {
			errors = append(errors, perrors.Wrapf(err, "cannot unmarshal kamelet file %q", kamelet.Name))
			continue
		}

		if !equality.Semantic.DeepDerivative(unstrFile, unstr) {
			errors = append(errors, fmt.Errorf("kamelet %q contains invalid content that is not supported by the Kamelet schema", kamelet.Name))
		}
	}
	return errors
}

func verifyParameters(kamelets []KameletInfo) (errors []error) {
	for _, kamelet := range kamelets {
		if kamelet.Spec.Definition == nil {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain the JSON schema definition", kamelet.Name))
			continue
		}
		if kamelet.Spec.Definition.Title == "" {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain title", kamelet.Name))
		}
		if kamelet.Spec.Definition.Description == "" {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain description", kamelet.Name))
		}
		if kamelet.Spec.Definition.Type != "object" {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain a definition of type \"object\"", kamelet.Name))
		}
		for k, p := range kamelet.Spec.Definition.Properties {
			if p.Type == "" {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q does not contain type", k, kamelet.Name))
			}
			if p.Title == "" {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q does not contain title", k, kamelet.Name))
			} else {
				cleanTitle := stopwords.CleanString(p.Title, "en", false)
				goodWords := strings.Split(cleanTitle, " ")
				check := make(map[string]bool, len(goodWords))
				for _, w := range goodWords {
					check[strings.ToLower(w)] = true
				}
				words := strings.Split(p.Title, " ")
				for _, w := range words {
					if !check[strings.ToLower(w)] {
						continue
					}
					if len(w) > 0 && strings.ToUpper(w[0:1]) != w[0:1] {
						errors = append(errors, fmt.Errorf("property %q in kamelet %q does has non-capitalized word in the title: %q", k, kamelet.Name, w))
					}
				}
			}
			if strings.HasPrefix(p.Title, "The ") {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q has a title starting with \"The \"", k, kamelet.Name))
			}
			if strings.HasPrefix(p.Title, "A ") {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q has a title starting with \"A \"", k, kamelet.Name))
			}
			if strings.HasPrefix(p.Title, "An ") {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q has a title starting with \"An \"", k, kamelet.Name))
			}
			if p.Description == "" {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q does not contain a description", k, kamelet.Name))
			}
		}
		for _, k := range kamelet.Spec.Definition.Required {
			if _, ok := kamelet.Spec.Definition.Properties[k]; !ok {
				errors = append(errors, fmt.Errorf("required property %q in kamelet %q is not defined", k, kamelet.Name))
			}
		}
	}
	return errors
}

func verifyAnnotations(kamelets []KameletInfo) (errors []error) {
	for _, kamelet := range kamelets {
		if icon := kamelet.Annotations["camel.apache.org/kamelet.icon"]; icon == "" {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain the camel.apache.org/kamelet.icon annotation", kamelet.Name))
		}
		expectedProvider := "Apache Software Foundation"
		if provider := kamelet.Annotations["camel.apache.org/provider"]; provider != expectedProvider {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain the right value for the camel.apache.org/provider annotation: expected %q, found %q", kamelet.Name, expectedProvider, provider))
		}
	}
	return errors
}

func verifyKameletType(kamelets []KameletInfo) (errors []error) {
	for _, kamelet := range kamelets {
		tp := kamelet.Labels["camel.apache.org/kamelet.type"]
		switch tp {
		case "source":
			fallthrough
		case "sink":
			fallthrough
		case "action":
			expectedSuffix := fmt.Sprintf("-%s", tp)
			if !strings.HasSuffix(kamelet.Name, expectedSuffix) {
				errors = append(errors, fmt.Errorf("name of kamelet %q does not end with %q", kamelet.Name, expectedSuffix))
			}
		default:
			errors = append(errors, fmt.Errorf("kamelet %q contains an invalid value for the camel.apache.org/kamelet.type label: %q", kamelet.Name, tp))
		}
	}
	return errors
}

func verifyFileNames(kamelets []KameletInfo) (errors []error) {
	for _, kamelet := range kamelets {
		if kamelet.Name != strings.TrimSuffix(path.Base(kamelet.FileName), ".kamelet.yaml") {
			errors = append(errors, fmt.Errorf("file %q does not match the name of the contained kamelet: %q", kamelet.FileName, kamelet.Name))
		}
	}
	return errors
}

func listKamelets(dir string) []KameletInfo {
	scheme := runtime.NewScheme()
	err := camel.AddToScheme(scheme)
	handleGeneralError("cannot to add camel APIs to scheme", err)

	codecs := serializer.NewCodecFactory(scheme)
	gv := camel.SchemeGroupVersion
	gvk := schema.GroupVersionKind{
		Group:   gv.Group,
		Version: gv.Version,
		Kind:    "Kamelet",
	}
	decoder := codecs.UniversalDecoder(gv)

	kamelets := make([]KameletInfo, 0)
	files, err := ioutil.ReadDir(dir)
	filesSorted := make([]string, 0)
	handleGeneralError(fmt.Sprintf("cannot list dir %q", dir), err)
	for _, fd := range files {
		if !fd.IsDir() && strings.HasSuffix(fd.Name(), ".kamelet.yaml") {
			fullName := filepath.Join(dir, fd.Name())
			filesSorted = append(filesSorted, fullName)
		}
	}
	sort.Strings(filesSorted)
	for _, fileName := range filesSorted {
		content, err := ioutil.ReadFile(fileName)
		handleGeneralError(fmt.Sprintf("cannot read file %q", fileName), err)

		json, err := yaml.ToJSON(content)
		handleGeneralError(fmt.Sprintf("cannot convert file %q to JSON", fileName), err)

		kamelet := camel.Kamelet{}
		_, _, err = decoder.Decode(json, &gvk, &kamelet)
		handleGeneralError(fmt.Sprintf("cannot unmarshal file %q into Kamelet", fileName), err)
		kameletInfo := KameletInfo{
			Kamelet:  kamelet,
			FileName: fileName,
		}
		kamelets = append(kamelets, kameletInfo)
	}
	return kamelets
}

type KameletInfo struct {
	camel.Kamelet
	FileName string
}

func handleGeneralError(desc string, err error) {
	if err != nil {
		fmt.Printf("%s: %+v\n", desc, err)
		os.Exit(2)
	}
}
