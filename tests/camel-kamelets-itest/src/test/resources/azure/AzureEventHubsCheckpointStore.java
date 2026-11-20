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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.models.Checkpoint;
import com.azure.messaging.eventhubs.models.PartitionOwnership;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.eventhubs.EventHubsComponent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Keeps the positions of the Event Hubs consumers in memory. The consumers save them in Blob storage of an Azure
 * account by default, which a test does not have, so this sets an in-memory store on the azure-eventhubs component,
 * which the consumers take instead.
 */
public class AzureEventHubsCheckpointStore extends RouteBuilder {

    @Override
    public void configure() {
        getContext().getComponent("azure-eventhubs", EventHubsComponent.class).getConfiguration()
                .setCheckpointStore(new InMemoryCheckpointStore());
    }

    private static final class InMemoryCheckpointStore implements CheckpointStore {

        private final Map<String, PartitionOwnership> ownerships = new ConcurrentHashMap<>();
        private final Map<String, Checkpoint> checkpoints = new ConcurrentHashMap<>();

        @Override
        public Flux<PartitionOwnership> listOwnership(String namespace, String eventHubName, String consumerGroup) {
            return Flux.fromIterable(ownerships.values());
        }

        @Override
        public Flux<PartitionOwnership> claimOwnership(List<PartitionOwnership> requested) {
            return Flux.fromIterable(requested).map(ownership -> {
                ownership.setETag(UUID.randomUUID().toString()).setLastModifiedTime(System.currentTimeMillis());
                ownerships.put(ownership.getPartitionId(), ownership);
                return ownership;
            });
        }

        @Override
        public Flux<Checkpoint> listCheckpoints(String namespace, String eventHubName, String consumerGroup) {
            return Flux.fromIterable(checkpoints.values());
        }

        @Override
        public Mono<Void> updateCheckpoint(Checkpoint checkpoint) {
            checkpoints.put(checkpoint.getPartitionId(), checkpoint);
            return Mono.empty();
        }
    }
}
