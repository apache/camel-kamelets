module github.com/apache/camel-kamelets/docs/generator

go 1.16

require (
	github.com/apache/camel-k v1.9.2
	github.com/apache/camel-k/pkg/apis/camel v1.9.2
	github.com/bbalet/stopwords v1.0.0
	github.com/pkg/errors v0.9.1
	gopkg.in/yaml.v3 v3.0.1
	k8s.io/apimachinery v0.22.5
)

replace (
	github.com/go-logr/logr => github.com/go-logr/logr v0.4.0
	k8s.io/klog/v2 => k8s.io/klog/v2 v2.9.0
)
