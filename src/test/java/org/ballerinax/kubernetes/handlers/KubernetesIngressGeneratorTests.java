/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinax.kubernetes.handlers;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import org.ballerinax.kubernetes.KubernetesConstants;
import org.ballerinax.kubernetes.exceptions.KubernetesPluginException;
import org.ballerinax.kubernetes.models.IngressModel;
import org.ballerinax.kubernetes.utils.KubernetesUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates kubernetes Service from annotations.
 */
public class KubernetesIngressGeneratorTests {

    private final Logger log = LoggerFactory.getLogger(KubernetesIngressGeneratorTests.class);
    private final String ingressName = "MyIngress";
    private final String hostname = "abc.com";
    private final String path = "/helloworld";
    private final String targetPath = "/";
    private final String ingressClass = "nginx";
    private final String serviceName = "HelloWorldService";
    private final int servicePort = 9090;
    private final String selector = "TestAPP";

    @Test
    public void testIngressGenerator() {
        IngressModel ingressModel = new IngressModel();
        ingressModel.setName(ingressName);
        ingressModel.setHostname(hostname);
        ingressModel.setPath(path);
        ingressModel.setTargetPath(targetPath);
        ingressModel.setServicePort(servicePort);
        ingressModel.setIngressClass(ingressClass);
        ingressModel.setServiceName(serviceName);
        Map<String, String> labels = new HashMap<>();
        labels.put(KubernetesConstants.KUBERNETES_SELECTOR_KEY, selector);
        ingressModel.setLabels(labels);
        try {
            String ingressContent = new IngressHandler(ingressModel).generate();
            Assert.assertNotNull(ingressContent);
            File artifactLocation = new File("target/kubernetes");
            artifactLocation.mkdir();
            File tempFile = File.createTempFile("temp", ingressModel.getName() + ".yaml", artifactLocation);
            KubernetesUtils.writeToFile(ingressContent, tempFile.getPath());
            log.info("Generated YAML: \n" + ingressContent);
            Assert.assertTrue(tempFile.exists());
            assertGeneratedYAML(tempFile);
            tempFile.deleteOnExit();
        } catch (IOException e) {
            Assert.fail("Unable to write to file");
        } catch (KubernetesPluginException e) {
            Assert.fail("Unable to generate yaml from ingress");
        }
    }

    private void assertGeneratedYAML(File yamlFile) throws IOException {
        Ingress ingress = KubernetesHelper.loadYaml(yamlFile);
        Assert.assertEquals(ingressName, ingress.getMetadata().getName());
        Assert.assertEquals(selector, ingress.getMetadata().getLabels().get(KubernetesConstants
                .KUBERNETES_SELECTOR_KEY));
        Assert.assertEquals(hostname, ingress.getSpec().getRules().get(0).getHost());
        Assert.assertEquals(path, ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath());
        Assert.assertEquals(serviceName, ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getBackend()
                .getServiceName());
        Assert.assertEquals(servicePort, ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getBackend()
                .getServicePort().getIntVal().intValue());
        Assert.assertEquals(3, ingress.getMetadata().getAnnotations().size());
    }
}
