# ---------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ---------------------------------------------------------------------------

Feature: Insert field Kamelet action

  Background:
    Given load Kubernetes resource pod.yaml
    When Kubernetes pod ibm-mq-server is running



  Scenario: Create Kamelet binding
    Given variable input is
    """
    Hello IBM MQ! Nice to reach you.
    """
    When load KameletBinding insert-field-action-binding.yaml
    Then Camel K integration insert-field-action-binding should be running
    And Camel K integration insert-field-action-binding should print Routes startup
    Then sleep 2500 ms

  Scenario: Remove resources
    Given delete KameletBinding insert-field-action-binding
    Then delete Kubernetes resource pod.yaml
