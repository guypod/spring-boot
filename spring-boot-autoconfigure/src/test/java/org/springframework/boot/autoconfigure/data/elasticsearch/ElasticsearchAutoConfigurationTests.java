/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.data.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.node.NodeClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class ElasticsearchAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@Before
	public void preventElasticsearchFromConfiguringNetty() {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
		System.clearProperty("es.set.netty.runtime.available.processors");
	}

	@Test
	public void createNodeClientWithDefaults() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.data.elasticsearch.properties.monitor.process.refresh_interval:2s",
						"spring.data.elasticsearch.properties.path.home:target")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				ElasticsearchAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(Client.class).length).isEqualTo(1);
		NodeClient client = (NodeClient) this.context.getBean(Client.class);
		assertThat(client.settings().get("monitor.process.refresh_interval"))
				.isEqualTo("2s");
		assertThat(client.settings().get("transport.type")).isEqualTo("local");
		assertThat(client.settings().get("http.enabled")).isEqualTo("false");
	}

	@Test
	public void createNodeClientWithOverrides() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.data.elasticsearch.properties.monitor.process.refresh_interval:2s",
						"spring.data.elasticsearch.properties.path.home:target",
						"spring.data.elasticsearch.properties.transport.type:local",
						"spring.data.elasticsearch.properties.node.data:true",
						"spring.data.elasticsearch.properties.http.enabled:true")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				ElasticsearchAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(Client.class).length).isEqualTo(1);
		NodeClient client = (NodeClient) this.context.getBean(Client.class);
		assertThat(client.settings().get("monitor.process.refresh_interval"))
				.isEqualTo("2s");
		assertThat(client.settings().get("transport.type")).isEqualTo("local");
		assertThat(client.settings().get("node.data")).isEqualTo("true");
		assertThat(client.settings().get("http.enabled")).isEqualTo("true");
	}

	@Test
	public void useExistingClient() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(CustomConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				ElasticsearchAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(Client.class).length).isEqualTo(1);
		assertThat(this.context.getBean("myClient"))
				.isSameAs(this.context.getBean(Client.class));
	}

	@Test
	public void createTransportClient() throws Exception {
		// We don't have a local elasticsearch server so use an address that's missing
		// a port and check the exception
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.data.elasticsearch.cluster-nodes:localhost",
						"spring.data.elasticsearch.properties.path.home:target")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				ElasticsearchAutoConfiguration.class);
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("port");
		this.context.refresh();
	}

	@Configuration
	static class CustomConfiguration {

		@Bean
		public Client myClient() {
			return mock(Client.class);
		}

	}

}
