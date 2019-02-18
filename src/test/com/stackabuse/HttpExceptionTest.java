package com.stackabuse;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class HttpExceptionTest extends CamelBlueprintTestSupport {

    Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/blueprint-example.xml";
    }

    @Test
    public void test404() throws InterruptedException {

        MockEndpoint mock = getMockEndpoint("mock:response");

        template.sendBody("direct:start", null);
        mock.expectedMessageCount(1);

        assertMockEndpointsSatisfied();

    }
}
