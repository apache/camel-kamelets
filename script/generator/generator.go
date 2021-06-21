package main

import (
	"bytes"
	"encoding/base64"
	"errors"
	"fmt"
	"io/ioutil"
	"os"
	"path"
	"path/filepath"
	"sort"
	"strings"
	"text/template"

	camel "github.com/apache/camel-k/pkg/apis/camel/v1alpha1"
	"github.com/iancoleman/strcase"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/runtime/serializer"
	"k8s.io/apimachinery/pkg/util/yaml"
)

func main() {
	if len(os.Args) != 3 {
		println("usage: generator kamelets-path doc-root")
		os.Exit(1)
	}

	dir := os.Args[1]
	out := os.Args[2]

	funcMap := template.FuncMap{
		"ToCamel": strcase.ToCamel,
	}

	templateFile := path.Join(out, "kamelet.adoc.tmpl")
	t, err := template.New("kamelet.adoc.tmpl").Funcs(funcMap).ParseFiles(templateFile)
	handleGeneralError(fmt.Sprintf("cannot load template file from %s", templateFile), err)

	kamelets := listKamelets(dir)

	links := make([]string, 0)
	for _, k := range kamelets {
		img := saveImage(k, out)

		ctx := NewTemplateContext(k, img)

		buffer := new(bytes.Buffer)
		err = t.Execute(buffer, &ctx)
		handleGeneralError("cannot process template", err)

		produceDocFile(k, out, buffer.String())

		links = append(links, fmt.Sprintf("* xref:ROOT:%s.adoc[%s %s]", k.Name, img, k.Spec.Definition.Title))
	}

	saveNav(links, out)
}

type TemplateContext struct {
	Kamelet camel.Kamelet
	Image   string
}

func NewTemplateContext(kamelet camel.Kamelet, image string) TemplateContext {
	return TemplateContext{
		Kamelet: kamelet,
		Image:   image,
	}
}

type Prop struct {
	Name     string
	Title    string
	Required bool
	Default  *string
	Example  *string
}

func (p Prop) GetSampleValue() string {
	if p.Default != nil {
		return *p.Default
	}
	if p.Example != nil {
		return *p.Example
	}
	return fmt.Sprintf(`"The %s"`, p.Title)
}

func getSortedProps(k camel.Kamelet) []Prop {
	required := make(map[string]bool)
	props := make([]Prop, 0, len(k.Spec.Definition.Properties))
	for _, r := range k.Spec.Definition.Required {
		required[r] = true
	}
	for key := range k.Spec.Definition.Properties {
		prop := k.Spec.Definition.Properties[key]
		var def *string
		if prop.Default != nil {
			b, err := prop.Default.MarshalJSON()
			handleGeneralError(fmt.Sprintf("cannot marshal property %q default value in Kamelet %s", key, k.Name), err)
			defVal := string(b)
			def = &defVal
		}
		var ex *string
		if prop.Example != nil {
			b, err := prop.Example.MarshalJSON()
			handleGeneralError(fmt.Sprintf("cannot marshal property %q example value in Kamelet %s", key, k.Name), err)
			exVal := string(b)
			ex = &exVal
		}
		props = append(props, Prop{Name: key, Title: prop.Title, Required: required[key], Default: def, Example: ex})
	}
	sort.Slice(props, func(i, j int) bool {
		ri := props[i].Required
		rj := props[j].Required
		if ri && !rj {
			return true
		} else if !ri && rj {
			return false
		}
		return props[i].Name < props[j].Name
	})
	return props
}

func (ctx *TemplateContext) HasProperties() bool {
	return len(ctx.Kamelet.Spec.Definition.Properties) > 0
}

func (ctx *TemplateContext) Properties() string {
	content := ""
	if len(ctx.Kamelet.Spec.Definition.Properties) > 0 {
		props := getSortedProps(ctx.Kamelet)
		content += `[width="100%",cols="2,^2,3,^2,^2,^3",options="header"]` + "\n"
		content += "|===\n"
		content += tableLine("Property", "Name", "Description", "Type", "Default", "Example")

		for _, propDef := range props {
			key := propDef.Name
			prop := ctx.Kamelet.Spec.Definition.Properties[key]
			name := key
			if propDef.Required {
				name = "*" + name + " {empty}* *"
			}
			var def string
			if propDef.Default != nil {
				def = "`" + strings.ReplaceAll(*propDef.Default, "`", "'") + "`"
			}
			var ex string
			if propDef.Example != nil {
				ex = "`" + strings.ReplaceAll(*propDef.Example, "`", "'") + "`"
			}
			content += tableLine(name, prop.Title, prop.Description, prop.Type, def, ex)
		}

		content += "|===\n"

	}
	return content
}

func (ctx *TemplateContext) ExampleBinding(apiVersion, kind, name string) string {
	content := ""
	propDefs := getSortedProps(ctx.Kamelet)
	tp := ctx.Kamelet.ObjectMeta.Labels["camel.apache.org/kamelet.type"]
	if tp != "" {
		sampleConfig := make([]string, 0)
		for _, propDef := range propDefs {
			if !propDef.Required {
				continue
			}
			key := propDef.Name
			if propDef.Default == nil {
				ex := propDef.GetSampleValue()
				sampleConfig = append(sampleConfig, fmt.Sprintf("%s: %s", key, ex))
			}
		}
		props := ""
		if len(sampleConfig) > 0 {
			props += "    properties:\n"
			for _, p := range sampleConfig {
				props += fmt.Sprintf("      %s\n", p)
			}
		}

		kameletRef := fmt.Sprintf(`    ref:
      kind: Kamelet
      apiVersion: camel.apache.org/v1alpha1
      name: %s
%s`, ctx.Kamelet.Name, props)

		boundToRef := fmt.Sprintf(`    ref:
      kind: %s
      apiVersion: %s
      name: %s
`, kind, apiVersion, name)
		var sourceRef string
		var sinkRef string
		var steps string

		switch tp {
		case "source":
			sourceRef = kameletRef
			sinkRef = boundToRef
		case "sink":
			sourceRef = boundToRef
			sinkRef = kameletRef
		case "action":
			sourceRef = `    ref:
      kind: Kamelet
      apiVersion: camel.apache.org/v1alpha1
      name: timer-source
    properties:
      message: "Hello"`
			sinkRef = boundToRef
			steps = fmt.Sprintf(`
  steps:
  -%s`, kameletRef[3:])
		}

		binding := fmt.Sprintf(`apiVersion: camel.apache.org/v1alpha1
kind: KameletBinding
metadata:
  name: %s-binding
spec:
  source:
%s%s  sink:
%s
`, ctx.Kamelet.Name, sourceRef, steps, sinkRef)

		content += fmt.Sprintf(".%s-binding.yaml\n", ctx.Kamelet.Name)
		content += "[source,yaml]\n"
		content += "----\n"
		content += binding
		content += "----\n"

	}
	return content
}

func (ctx *TemplateContext) ExampleKamelBindCommand(ref string) string {
	tp := ctx.Kamelet.ObjectMeta.Labels["camel.apache.org/kamelet.type"]
	var prefix string
	switch tp {
	case "source":
		prefix = "source."
	case "sink":
		prefix = "sink."
	case "action":
		prefix = "step-0."
	default:
		handleGeneralError("unknown kamelet type", errors.New(tp))
	}

	cmd := "kamel bind "
	timer := "timer-source?message=Hello"
	kamelet := ctx.Kamelet.Name
	propDefs := getSortedProps(ctx.Kamelet)
	for _, p := range propDefs {
		if p.Required && p.Default == nil {
			val := p.GetSampleValue()
			if strings.HasPrefix(val, `"`) {
				kamelet += fmt.Sprintf(` -p "%s%s=%s`, prefix, p.Name, val[1:])
			} else {
				kamelet += fmt.Sprintf(" -p %s%s=%s", prefix, p.Name, val)
			}
		}
	}

	switch tp {
	case "source":
		return cmd + kamelet + " " + ref
	case "sink":
		return cmd + ref + " " + kamelet
	case "action":
		return cmd + timer + " --step " + kamelet + " " + ref
	default:
		handleGeneralError("unknown kamelet type", errors.New(tp))
	}
	return ""
}

func saveNav(links []string, out string) {
	content := "// THIS FILE IS AUTOMATICALLY GENERATED: DO NOT EDIT\n"
	for _, l := range links {
		content += l + "\n"
	}
	content += "// THIS FILE IS AUTOMATICALLY GENERATED: DO NOT EDIT\n"
	dest := filepath.Join(out, "nav.adoc")
	if _, err := os.Stat(dest); err == nil {
		err = os.Remove(dest)
		handleGeneralError(fmt.Sprintf("cannot remove file %q", dest), err)
	}
	err := ioutil.WriteFile(dest, []byte(content), 0666)
	handleGeneralError(fmt.Sprintf("cannot write file %q", dest), err)
	fmt.Printf("%q written\n", dest)
}

func saveImage(k camel.Kamelet, out string) string {
	if ic, ok := k.ObjectMeta.Annotations["camel.apache.org/kamelet.icon"]; ok {
		svgb64Prefix := "data:image/svg+xml;base64,"
		if strings.HasPrefix(ic, svgb64Prefix) {
			data := ic[len(svgb64Prefix):]
			decoder := base64.NewDecoder(base64.StdEncoding, strings.NewReader(data))
			iconContent, err := ioutil.ReadAll(decoder)
			handleGeneralError(fmt.Sprintf("cannot decode icon from Kamelet %s", k.Name), err)
			dest := filepath.Join(out, "assets", "images", "kamelets", fmt.Sprintf("%s.svg", k.Name))
			if _, err := os.Stat(dest); err == nil {
				err = os.Remove(dest)
				handleGeneralError(fmt.Sprintf("cannot remove file %q", dest), err)
			}
			err = ioutil.WriteFile(dest, iconContent, 0666)
			handleGeneralError(fmt.Sprintf("cannot write file %q", dest), err)
			fmt.Printf("%q written\n", dest)
			return fmt.Sprintf("image:kamelets/%s.svg[]", k.Name)
		}
	}
	return ""
}

func produceDocFile(k camel.Kamelet, out string, content string) {
	docFile := filepath.Join(out, "pages", k.Name+".adoc")
	if _, err := os.Stat(docFile); err == nil {
		err = os.Remove(docFile)
		handleGeneralError(fmt.Sprintf("cannot remove file %q", docFile), err)
	}
	err := ioutil.WriteFile(docFile, []byte(content), 0666)
	handleGeneralError(fmt.Sprintf("cannot write to file %q", docFile), err)
	fmt.Printf("%q written\n", docFile)
}

func tableLine(val ...string) string {
	res := ""
	for _, s := range val {
		clean := strings.ReplaceAll(s, "|", "\\|")
		res += "| " + clean
	}
	return res + "\n"
}

func listKamelets(dir string) []camel.Kamelet {
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

	kamelets := make([]camel.Kamelet, 0)
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
		kamelets = append(kamelets, kamelet)
	}
	return kamelets
}

func handleGeneralError(desc string, err error) {
	if err != nil {
		fmt.Printf("%s: %+v\n", desc, err)
		os.Exit(2)
	}
}
