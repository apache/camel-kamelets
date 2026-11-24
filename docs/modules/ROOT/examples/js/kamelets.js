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

const svgb64Prefix = 'data:image/svg+xml;base64,'

module.exports = {
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
