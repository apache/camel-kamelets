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

const QUOTED_CHARS = /[$`"\\]/g

const QUOTE_REPLACEMENTS = {
  '$': '\\$',
  '\`': '\\\`',
  '"': '\\"',
  '\\': '\\\\',
}

const svgb64Prefix = 'data:image/svg+xml;base64,'

module.exports = {
  binding: (binding, apiVersion, kind, metadata_, spec_, refKind, refApiVersion, refName) => {
      const name = metadata_.name
      const metadata = {name: `${name}-binding`}
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
    // return `= image:kamelets/${basename}.svg[] ${title}
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
      .map(([name, value]) => [name, value.example ? value.example : `The ${value.title}`])
  )
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

