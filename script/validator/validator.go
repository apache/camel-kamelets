package main

import (
	"fmt"
	"github.com/apache/camel-k/v2/pkg/metadata"
	"github.com/apache/camel-k/v2/pkg/util/camel"
	"github.com/apache/camel-k/v2/pkg/util/dsl"
	"io/ioutil"
	"os"
	"path"
	"path/filepath"
	"regexp"
	"sort"
	"strings"

	camelapiv1 "github.com/apache/camel-k/v2/pkg/apis/camel/v1"
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

var (
	paramRegexp = regexp.MustCompile(`{{[?]?([A-Za-z0-9-._]+)(?:[:][^}]*)?}}`)
)

func main() {
	if len(os.Args) != 2 {
		println("usage: validator kamelets-path")
		os.Exit(1)
	}

	dir := os.Args[1]

	kamelets := listKamelets(dir)

	if len(kamelets) <= 0 {
		fmt.Printf("ERROR: directory %s has no Kamelets\n", dir)
		os.Exit(1)
	}

	errors := verifyFileNames(kamelets)
	errors = append(errors, verifyKameletType(kamelets)...)
	errors = append(errors, verifyParameters(kamelets)...)
	errors = append(errors, verifyInvalidContent(kamelets)...)

	// Any failing validation above may result in error in the below methods,
	// let's show the errors if any
	for _, err := range errors {
		fmt.Printf("ERROR: %v\n", err)
	}
	if len(errors) > 0 {
		os.Exit(1)
	}

	errors = append(errors, verifyAnnotations(kamelets)...)
	errors = append(errors, verifyDescriptors(kamelets)...)
	errors = append(errors, verifyDuplicates(kamelets)...)
	errors = append(errors, verifyMissingDependencies(kamelets)...)
	errors = append(errors, verifyUsedParams(kamelets)...)

	for _, err := range errors {
		fmt.Printf("ERROR: %v\n", err)
	}
	if len(errors) > 0 {
		os.Exit(1)
	}
}

func verifyMissingDependencies(kamelets []KameletInfo) (errors []error) {
	for _, kamelet := range kamelets {
		yamlDslTemplate, err := dsl.TemplateToYamlDSL(*kamelet.Kamelet.Spec.Template, kamelet.Kamelet.Name)
		if err != nil {
			panic(err)
		}

		code := camelapiv1.SourceSpec{
			DataSpec: camelapiv1.DataSpec{
				Name:    "source.yaml",
				Content: string(yamlDslTemplate),
			},
			Language: camelapiv1.LanguageYaml,
		}

		catalog, _ := camel.DefaultCatalog()
		meta, _ := metadata.Extract(catalog, code)

		if meta.Metadata.Dependencies != nil {
			meta.Metadata.Dependencies.Each(func(extractedDep string) bool {
				if !containsDependency(kamelet, extractedDep) {
					errors = append(errors, fmt.Errorf("kamelet %q need dependency %q", kamelet.Name, extractedDep))
				}
				return true
			})
		}
	}

	return errors
}

func containsDependency(kamelet KameletInfo, extractedDep string) bool {
	for _, d := range kamelet.Spec.Dependencies {
		if d == extractedDep {
			return true
		}
	}
	return false
}

func verifyDuplicates(kamelets []KameletInfo) (errors []error) {
	usedTitles := make(map[string]bool)
	usedDescriptions := make(map[string]bool)
	for _, kamelet := range kamelets {
		if kamelet.Spec.Definition == nil {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain the JSON schema definition", kamelet.Name))
			continue
		}
		title := kamelet.Kamelet.Spec.Definition.Title
		if _, found := usedTitles[title]; found {
			errors = append(errors, fmt.Errorf("kamelet %q has duplicate title %q", kamelet.Name, title))
		}
		description := kamelet.Kamelet.Spec.Definition.Description
		if _, found := usedDescriptions[description]; found {
			errors = append(errors, fmt.Errorf("kamelet %q has duplicate description %q", kamelet.Name, description))
		}
		usedTitles[title] = true
		usedDescriptions[description] = true
	}
	return errors
}

func verifyDescriptors(kamelets []KameletInfo) (errors []error) {
	for _, kamelet := range kamelets {
		if kamelet.Spec.Definition == nil {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain the JSON schema definition", kamelet.Name))
			continue
		}
		for k, p := range kamelet.Spec.Definition.Properties {
			pwdDescriptor := "urn:alm:descriptor:com.tectonic.ui:password"
			if hasXDescriptor(p, pwdDescriptor) && p.Format != "password" {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q has password descriptor %q but its format is not \"password\"", k, kamelet.Name, pwdDescriptor))
			} else if !hasXDescriptor(p, pwdDescriptor) && p.Format == "password" {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q has \"password\" format but misses descriptor %q (for better compatibility with tectonic UIs)", k, kamelet.Name, pwdDescriptor))
			}
		}
		for k, p := range kamelet.Spec.Definition.Properties {
			credDescriptor := "urn:camel:group:credentials"
			if p.Format == "password" && !hasXDescriptor(p, credDescriptor) {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q has \"password\" format but misses descriptor %q", k, kamelet.Name, credDescriptor))
			}
		}
		for k, p := range kamelet.Spec.Definition.Properties {
			checkboxDescriptor := "urn:alm:descriptor:com.tectonic.ui:checkbox"
			if hasXDescriptor(p, checkboxDescriptor) && p.Type != "boolean" {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q has checkbox descriptor %q but its type is not \"boolean\"", k, kamelet.Name, checkboxDescriptor))
			} else if !hasXDescriptor(p, checkboxDescriptor) && p.Type == "boolean" {
				errors = append(errors, fmt.Errorf("property %q in kamelet %q has \"boolean\" type but misses descriptor %q (for better compatibility with tectonic UIs)", k, kamelet.Name, checkboxDescriptor))
			}
		}
	}
	return errors
}

func hasXDescriptor(p camelapiv1.JSONSchemaProp, desc string) bool {
	for _, d := range p.XDescriptors {
		if d == desc {
			return true
		}
	}
	return false
}

func hasXDescriptorPrefix(p camelapiv1.JSONSchemaProp, prefix string) bool {
	for _, d := range p.XDescriptors {
		if strings.HasPrefix(d, prefix) {
			return true
		}
	}
	return false
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
		if kamelet.Spec.Template == nil {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain the Template specification", kamelet.Name))
			continue
		}
		requiredCheck := make(map[string]bool)
		for _, p := range kamelet.Spec.Definition.Required {
			if requiredCheck[p] {
				errors = append(errors, fmt.Errorf("required kamelet property %q is listed twice in kamelet %q", p, kamelet.Name))
			}
			requiredCheck[p] = true
		}
		if kamelet.Spec.Definition.Title == "" {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain title", kamelet.Name))
		} else {
			tp := kamelet.Labels["camel.apache.org/kamelet.type"]
			if len(tp) > 1 {
				expectedSuffix := strings.ToUpper(tp[0:1]) + tp[1:]
				if !strings.HasSuffix(kamelet.Spec.Definition.Title, expectedSuffix) {
					errors = append(errors, fmt.Errorf("kamelet %q title %q does not ends with %q", kamelet.Name, kamelet.Spec.Definition.Title, expectedSuffix))
				}
			}
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
		if catalogVersion := kamelet.Annotations["camel.apache.org/catalog.version"]; catalogVersion == "" {
			errors = append(errors, fmt.Errorf("kamelet %q does not contain the camel.apache.org/catalog.version annotation (should match project version in pom.xml)", kamelet.Name))
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
	err := camelapiv1.AddToScheme(scheme)
	handleGeneralError("cannot to add camel APIs to scheme", err)

	codecs := serializer.NewCodecFactory(scheme)
	gv := camelapiv1.SchemeGroupVersion
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

		kamelet := camelapiv1.Kamelet{}
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

func verifyUsedParams(kamelets []KameletInfo) (errors []error) {
	for _, k := range kamelets {
		if k.FileName != "../../kamelets/azure-storage-blob-source.kamelet.yaml" && k.FileName != "../../kamelets/aws-s3-cdc-source.kamelet.yaml" && k.FileName != "../../kamelets/set-kafka-key-action.kamelet.yaml" && k.FileName != "../../kamelets/azure-storage-blob-cdc-source.kamelet.yaml" && k.FileName != "../../kamelets/google-storage-cdc-source.kamelet.yaml" && k.FileName != "../../kamelets/elasticsearch-search-source.kamelet.yaml" && k.FileName != "../../kamelets/opensearch-search-source.kamelet.yaml" {
			used := getUsedParams(k.Kamelet)
			declared := getDeclaredParams(k.Kamelet)
			for p := range used {
				if _, ok := declared[p]; !ok {
					errors = append(errors, fmt.Errorf("parameter %q is not declared in the definition of kamelet %q", p, k.Kamelet.Name))
				}
			}
			for p := range declared {
				if _, ok := used[p]; !ok {
					errors = append(errors, fmt.Errorf("parameter %q is declared in kamelet %q but never used", p, k.Kamelet.Name))
				}
			}
		}
	}
	return errors
}

func getDeclaredParams(k camelapiv1.Kamelet) map[string]bool {
	res := make(map[string]bool)
	if k.Spec.Definition != nil {
		for p := range k.Spec.Definition.Properties {
			res[p] = true
		}
	}
	// include beans
	var templateData interface{}
	if err := json.Unmarshal(k.Spec.Template.RawMessage, &templateData); err != nil {
		handleGeneralError("cannot unmarshal template", err)
	}
	if fd, ok := templateData.(map[string]interface{}); ok {
		beans := fd["beans"]
		if bl, ok := beans.([]interface{}); ok {
			for _, bdef := range bl {
				if bmap, ok := bdef.(map[string]interface{}); ok {
					beanName := bmap["name"]
					if beanNameStr, ok := beanName.(string); ok {
						res[beanNameStr] = true
					}
				}
			}
		}
	}
	return res
}

func getUsedParams(k camelapiv1.Kamelet) map[string]bool {
	if k.Spec.Template != nil {
		var templateData interface{}
		if err := json.Unmarshal(k.Spec.Template.RawMessage, &templateData); err != nil {
			handleGeneralError("cannot unmarshal template", err)
		}
		params := make(map[string]bool)
		inspectTemplateParams(templateData, params)
		for propName, propVal := range k.Spec.Definition.Properties {
			if hasXDescriptorPrefix(propVal, "urn:keda:") {
				// Assume KEDA parameters may be used by KEDA
				params[propName] = true
			}
		}
		return params
	}
	return nil
}

func inspectTemplateParams(v interface{}, params map[string]bool) {
	switch val := v.(type) {
	case string:
		res := paramRegexp.FindAllStringSubmatch(val, -1)
		for _, m := range res {
			if len(m) > 1 {
				params[m[1]] = true
			}
		}
	case []interface{}:
		for _, c := range val {
			inspectTemplateParams(c, params)
		}
	case map[string]interface{}:
		for _, c := range val {
			inspectTemplateParams(c, params)
		}
	case map[interface{}]interface{}:
		for _, c := range val {
			inspectTemplateParams(c, params)
		}
	}
}

type KameletInfo struct {
	camelapiv1.Kamelet
	FileName string
}

func handleGeneralError(desc string, err error) {
	if err != nil {
		fmt.Printf("%s: %+v\n", desc, err)
		os.Exit(2)
	}
}
