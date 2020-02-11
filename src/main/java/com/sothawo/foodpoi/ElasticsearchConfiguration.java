/*
 Copyright 2020 Peter-Josef Meisch (pj.meisch@sothawo.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.sothawo.foodpoi;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com)
 */
@Configuration
public class ElasticsearchConfiguration extends AbstractElasticsearchConfiguration {

    private final FoodPOIConfiguration foodPOIConfiguration;

    public ElasticsearchConfiguration(FoodPOIConfiguration foodPOIConfiguration) {
        this.foodPOIConfiguration = foodPOIConfiguration;
    }

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {

        ClientConfiguration.TerminalClientConfigurationBuilder builder = ClientConfiguration.builder()
            .connectedTo(foodPOIConfiguration.getElasticSearchHost());

        if (StringUtils.hasLength(foodPOIConfiguration.getElasticSearchProxy())) {
            builder = builder.withProxy(foodPOIConfiguration.getElasticSearchProxy());
        }

        final ClientConfiguration clientConfiguration = builder
            .withSocketTimeout(Duration.ofSeconds(60)) //
            .build();

        return RestClients.create(clientConfiguration).rest();
    }
}
