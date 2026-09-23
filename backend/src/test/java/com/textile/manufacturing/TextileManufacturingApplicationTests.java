package com.textile.manufacturing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.textile.manufacturing.support.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;

class TextileManufacturingApplicationTests extends IntegrationTestBase {

    private final ApplicationContext applicationContext;

    TextileManufacturingApplicationTests(@Autowired ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void mainClassIsRegisteredAsAConfigurationBean() {
        assertThat(applicationContext.containsBean("textileManufacturingApplication")).isTrue();
    }
}
