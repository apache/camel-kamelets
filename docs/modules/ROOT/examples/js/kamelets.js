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

const util = require('camel-website-util')
const fs = require('fs')
const yaml = require('js-yaml');

const QUOTED_CHARS = /[$`"\\]/g

const QUOTE_REPLACEMENTS = {
  '$': '\\$',
  '\`': '\\\`',
  '"': '\\"',
  '\\': '\\\\',
}

// marker added to the first line of kamelet binding files in templates/bindings/camel-k
// generator will not generate a kamelet binding example and will source this kamelet binding file into the generated doc
const EXAMPLE_KAMELET_DOC_MARKER = "example_for_kamelet_doc"

// regex to replace the sink type
const regex = new RegExp(`(  sink:\\n\\s*ref:\\n)(\\s*kind:)(.*)(\\n\\s*apiVersion:)(.*)(\\n\\s*name:)(.*)`, 'g')

const svgb64Prefix = 'data:image/svg+xml;base64,'

module.exports = {
  binding: (binding, apiVersion, kind, metadata_, spec_, refKind, refApiVersion, refName) => {
      const name = metadata_.name
      const metadata = {name: `${name}-binding`}

      genExample = shouldGenerateKameletBindingExample(metadata.name)
      if (genExample) {
        const kamelet = {
          ref: {
            kind,
            apiVersion,
            name,
          },
          properties: kameletPropertyList(spec_.definition)
        }
        const platform = {
          ref: {
            kind: refKind,
            apiVersion: refApiVersion,
            name: refName,
          },
        }
        const base = {
          apiVersion,
          kind: 'KameletBinding',
          metadata,
        }
        const fn = kameletBindings[binding] || (() => `unrecognized binding ${binding}`)
        return fn(base, kamelet, platform)
      } else {
        content = readKameletBindingExample(metadata.name, refApiVersion, refKind, refName)
        return content
      }
  },

  bindingCommand: (binding, name, definition, topic) => {
    const namePrefix = { action: 'step-0', sink: 'sink', source: 'source' }[binding]
    const quote = (string) => (typeof string === 'String')
      ? string.replace(QUOTED_CHARS, (m) => QUOTE_REPLACEMENTS[m])
      : string
    const properties = Object.entries(kameletPropertyList(definition) || {})
      .map(([name, value]) => `-p "${namePrefix}.${name}=${quote(value)}"`)
      .join(' ')
    return `kamel bind ${name} ${properties} ${topic}`
  },

  sort: function (list) {
    function alphaSort (list) {
      return list.sort((a, b) => a.name < b.name ?  -1: a.name > b.name ? 1: 0)
    }
    const requiredNames = this.data.spec.definition.required || []
    const { required, optional } = list.reduce((accum, item) => {
      const name = item.path[4]
      if (requiredNames.includes(name)) {
        accum.required.push({name, value: Object.assign({ required: true }, item.value)} )
      } else {
        accum.optional.push({ name, value: item.value })
      }
      return accum
    }, { required: [], optional: []})
    return [...alphaSort(required), ...alphaSort(optional)]
  },

  icon: ($) => {
    const b64 = $.metadata.annotations['camel.apache.org/kamelet.icon']
    try {
      if (b64.startsWith(svgb64Prefix)) {
        data = b64.slice(svgb64Prefix.length)
        return Buffer.from(data, 'base64').toString()
      }
    } catch (e) {
      console.log(`icon problem ${b64}`, e)
    }
    return 'generic svg!'
  },

  templateHeader: (basename, $) => {
    const title = $.spec.definition.title
    const name = $.metadata.name
    const provider = $.metadata.annotations["camel.apache.org/provider"]
    const supportLevel = $.metadata.annotations["camel.apache.org/kamelet.support.level"]
    const type = $.metadata.labels["camel.apache.org/kamelet.type"]
    const propertyCount = Object.keys(($.spec.definition.properties || {})).length
    return `= image:kamelets/${basename}.svg[] ${title}
:name: ${name}
:provider: ${provider}
:support-level: ${supportLevel}
:type: ${type}
:propertycount: ${propertyCount}
`
  },

//  Compatibility table support
  branch: (version) =>  version === 'next' ?
    'https://github.com/apache/camel-kamelets[main]' :
    `https://github.com/apache/camel-kamelets/tree/${version}[${version}]`,
}

function kameletPropertyList (definition) {
  return definition.required && definition.properties && Object.fromEntries(
    Object.entries(definition.properties)
      .filter(([name, value]) => definition.required.includes(name))
      .sort(([name1, value1], [name2, value2]) => name1.localeCompare(name2))
      .map(([name, value]) => [name, value.example ? util.escapeAutoLinks(value.example) : `The ${value.title}`])
  )
}

// verify if the existing kamelet binding example should be automatically generated
// by checking if there is a comment marker in the first line
function shouldGenerateKameletBindingExample(file) {
  f = "../camel-kamelets/templates/bindings/camel-k/" + file + ".yaml"
  try {
    bufContent = fs.readFileSync(f)
    content = bufContent.toString()
    line = content.split(/\r?\n/)[0]
    return line.indexOf(EXAMPLE_KAMELET_DOC_MARKER) < 0
  } catch (err) {
    // in case there is no kamelet binding example file, assume the example should be generated
    return true
  }
}

// source the kamelet binding example from the example file
// skip the first line and replace the sink kind when the kind is a knative channel
function readKameletBindingExample(file, apiVersion, kind, name) {
  f = "../camel-kamelets/templates/bindings/camel-k/" + file + ".yaml"
  try {
    bufContent = fs.readFileSync(f)
    content = bufContent.toString()
    lines = content.split(/\r?\n/)
    klbContent = ""
    // skip the first line, as it contains the comment marker
    for (i = 1; i < lines.length; i++) {
      klbContent += lines[i] + "\n"
    }
    // uses a knative channel sink
    klbContent = klbContent.replace(regex, "$1$2 " + kind + "$4 " + apiVersion + "$6 " + name);
    yamlDoc = yaml.load(klbContent);
    return yamlDoc
  } catch (err) {
    console.log("Error reading kamelet binding example file " + file + ": " +  err)
    return err
  }
}


const kameletBindings = {
  action: (base, kamelet, platform) => Object.assign(base, {
      spec: {
        source: {
          ref: {
            kind: 'Kamelet',
            apiVersion: 'camel.apache.org/v1alpha1',
            name: 'timer-source',
            properties: {
              message: 'Hello',
            },
          },
        },
        steps: [
          kamelet,
        ],
        sink: platform,
      },
    }),

  sink: (base, kamelet, platform) => Object.assign(base, {
      spec: {
        source: platform,
        sink: kamelet,
      },
    }),

  source: (base, kamelet, platform) => Object.assign(base, {
      spec: {
        source: kamelet,
        sink: platform,
      },
    }),
}
