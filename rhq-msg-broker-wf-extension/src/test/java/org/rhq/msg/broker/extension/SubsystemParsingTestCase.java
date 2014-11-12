package org.rhq.msg.broker.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceNotFoundException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@Test
public class SubsystemParsingTestCase extends SubsystemBaseParsingTestCase {

    @Override
    @BeforeTest
    public void initializeParser() throws Exception {
        super.initializeParser();
    }

    @Override
    @AfterTest
    public void cleanup() throws Exception {
        super.cleanup();
    }

    /**
     * Tests that the xml is parsed into the correct operations
     */
    public void testParseSubsystem() throws Exception {
        // Parse the subsystem xml into operations
        String subsystemXml = getSubsystemXml();
        List<ModelNode> operations = super.parse(subsystemXml);

        // /Check that we have the expected number of operations
        Assert.assertEquals(operations.size(), 1);

        // Check that each operation has the correct content
        // The add subsystem operation will happen first
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(addSubsystem.get(OP).asString(), ADD);
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(addr.size(), 1);
        PathElement element = addr.getElement(0);
        Assert.assertEquals(element.getKey(), SUBSYSTEM);
        Assert.assertEquals(element.getValue(), BrokerSubsystemExtension.SUBSYSTEM_NAME);
        Assert.assertEquals(addSubsystem.get(BrokerSubsystemExtension.BROKER_ENABLED_ATTR).resolve().asBoolean(), true);
        Assert.assertEquals(addSubsystem.get(BrokerSubsystemExtension.BROKER_CONFIG_FILE_ATTR).resolve().asString(), "foo/bar.xml");
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    public void testInstallIntoController() throws Exception {
        // Parse the subsystem xml and install into the controller
        String subsystemXml = getSubsystemXml();
        KernelServices services = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();
        System.out.println(model);
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(BrokerSubsystemExtension.SUBSYSTEM_NAME));
        Assert.assertTrue(model.get(SUBSYSTEM, BrokerSubsystemExtension.SUBSYSTEM_NAME).hasDefined(BrokerSubsystemExtension.BROKER_ENABLED_ATTR));
        Assert.assertTrue(model.get(SUBSYSTEM, BrokerSubsystemExtension.SUBSYSTEM_NAME, BrokerSubsystemExtension.BROKER_ENABLED_ATTR).resolve().asBoolean());

        // Sanity check to test the service was there
        BrokerService broker = (BrokerService) services.getContainer().getRequiredService(BrokerService.SERVICE_NAME)            .getValue();
        Assert.assertNotNull(broker);
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second controller started with the xml
     * marshalled from the first one results in the same model
     */
    public void testParseAndMarshalModel() throws Exception {
        // Parse the subsystem xml and install into the first controller
        String subsystemXml = getSubsystemXml();
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();

        // Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(null).setSubsystemXml(marshalled).build();
        ModelNode modelB = servicesB.readWholeModel();

        // Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second controller started with the
     * operations from its describe action results in the same model
     */
    public void testDescribeHandler() throws Exception {
        String subsystemXml = getSubsystemXml();
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
            PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, BrokerSubsystemExtension.SUBSYSTEM_NAME))
                .toModelNode());
        ModelNode executeOperation = servicesA.executeOperation(describeOp);
        List<ModelNode> operations = super.checkResultAndGetContents(executeOperation).asList();

        // Install the describe options from the first controller into a second controller
        KernelServices servicesB = createKernelServicesBuilder(null).setBootOperations(operations).build();
        ModelNode modelB = servicesB.readWholeModel();

        // Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);
    }

    /**
     * Tests that the subsystem can be removed
     */
    public void testSubsystemRemoval() throws Exception {
        // Parse the subsystem xml and install into the first controller
        String subsystemXml = getSubsystemXml();
        KernelServices services = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // Sanity check to test the service was there
        BrokerService broker = (BrokerService) services.getContainer().getRequiredService(BrokerService.SERVICE_NAME).getValue();
        Assert.assertNotNull(broker);

        // Checks that the subsystem was removed from the model
        super.assertRemoveSubsystemResources(services);

        // Check that the services that was installed was removed
        try {
            services.getContainer().getRequiredService(BrokerService.SERVICE_NAME);
            assert false : "The service should have been removed along with the subsystem";
        } catch (ServiceNotFoundException expected) {
            // test passed!
        }
    }

    public void testResourceDescription() throws Exception {
        String subsystemXml = getSubsystemXml();
        KernelServices services = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        PathAddress brokerSubsystemPath = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, BrokerSubsystemExtension.SUBSYSTEM_NAME));

        // ask for resource description: /subsystem=broker:read-resource-description
        ModelNode resourceDescriptionOp = new ModelNode();
        resourceDescriptionOp.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        resourceDescriptionOp.get(OP_ADDR).set(brokerSubsystemPath.toModelNode());
        resourceDescriptionOp.get("operations").set(true); // we want to see the operations also
        ModelNode result = services.executeOperation(resourceDescriptionOp);
        ModelNode content = checkResultAndGetContents(result);

        // check the attributes
        Assert.assertTrue(content.get("attributes").isDefined());
        List<Property> attributes = content.get("attributes").asPropertyList();

        List<String> expectedAttributes = Arrays.asList( //
                BrokerSubsystemExtension.CONNECTOR_SOCKET_BINDING_ATTR, //
                BrokerSubsystemExtension.CONNECTOR_NAME_ATTR, //
                BrokerSubsystemExtension.CONNECTOR_PROTOCOL_ATTR, //
                BrokerSubsystemExtension.CUSTOM_CONFIG_ELEMENT, //
                BrokerSubsystemExtension.USE_JMX_ELEMENT, //
                BrokerSubsystemExtension.PERSISTENT_ELEMENT, //
                BrokerSubsystemExtension.BROKER_CONFIG_FILE_ATTR, //
                BrokerSubsystemExtension.BROKER_NAME_ELEMENT, //
                BrokerSubsystemExtension.BROKER_ENABLED_ATTR);
        Assert.assertEquals(attributes.size(), expectedAttributes.size());

        for (int i = 0 ; i < attributes.size(); i++) {
            String attrib = attributes.get(i).getName();
            Assert.assertTrue(expectedAttributes.contains(attrib), "missing attrib: " + attrib);
        }

        // check the operations (there are many other operations that AS adds to our resource, but we only want to check for ours)
        List<String> expectedOperations = Arrays.asList( //
                BrokerSubsystemExtension.BROKER_START_OP, //
                BrokerSubsystemExtension.BROKER_STOP_OP, //
                BrokerSubsystemExtension.BROKER_STATUS_OP);
        Assert.assertTrue(content.get("operations").isDefined());
        List<Property> operations = content.get("operations").asPropertyList();
        List<String> operationNames = new ArrayList<String>();
        for (Property op : operations) {
            operationNames.add(op.getName());
        }
        for (String expectedOperation : expectedOperations) {
            Assert.assertTrue(operationNames.contains(expectedOperation), "Missing: " + expectedOperation);
        }
    }

    public void testExecuteOperations() throws Exception {
        String subsystemXml = getSubsystemXml();
        KernelServices services = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // status check - our service should be available
        BrokerService service = (BrokerService) services.getContainer().getService(BrokerService.SERVICE_NAME).getValue();
        Assert.assertNotNull(service);

        PathAddress brokerSubsystemPath = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, BrokerSubsystemExtension.SUBSYSTEM_NAME));

        // get the startup model from subsystem xml
        ModelNode model = services.readWholeModel();

        // current list of config props
        ModelNode configNode = model.get(SUBSYSTEM, BrokerSubsystemExtension.SUBSYSTEM_NAME).get(BrokerSubsystemExtension.CUSTOM_CONFIG_ELEMENT);

        // Add another
        configNode.add("foo", "true");
        ModelNode addOp = new ModelNode();
        addOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        addOp.get(OP_ADDR).set(brokerSubsystemPath.toModelNode());
        addOp.get(NAME).set(BrokerSubsystemExtension.CUSTOM_CONFIG_ELEMENT);
        addOp.get(VALUE).set(configNode);
        ModelNode result = services.executeOperation(addOp);
        Assert.assertEquals(result.get(OUTCOME).asString(), SUCCESS);

        // now test that things are as they should be
        model = services.readWholeModel();
        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(BrokerSubsystemExtension.SUBSYSTEM_NAME));
        Assert.assertTrue(model.get(SUBSYSTEM, BrokerSubsystemExtension.SUBSYSTEM_NAME).hasDefined(BrokerSubsystemExtension.BROKER_ENABLED_ATTR));
        Assert.assertTrue(model.get(SUBSYSTEM, BrokerSubsystemExtension.SUBSYSTEM_NAME, BrokerSubsystemExtension.BROKER_ENABLED_ATTR).resolve().asBoolean());
        Assert.assertTrue(model.get(SUBSYSTEM, BrokerSubsystemExtension.SUBSYSTEM_NAME).hasDefined(BrokerSubsystemExtension.CUSTOM_CONFIG_ELEMENT));

        List<Property> props = model.get(SUBSYSTEM, BrokerSubsystemExtension.SUBSYSTEM_NAME).get(BrokerSubsystemExtension.CUSTOM_CONFIG_ELEMENT)
                .asPropertyList();
        Assert.assertEquals(props.size(), 3); // there were 2, but we added "foo" above
        Assert.assertEquals(props.get(0).getName(), "custom-prop");
        Assert.assertEquals(props.get(0).getValue().asString(), "custom-prop-val");
        Assert.assertEquals(props.get(1).getName(), "custom-prop2");
        Assert.assertEquals(props.get(1).getValue().asString(), "custom-prop-val2");
        Assert.assertEquals(props.get(2).getName(), "foo");
        Assert.assertEquals(props.get(2).getValue().asString(), "true");

        // Use read-attribute instead of reading the whole model to get an attribute value
        ModelNode readOp = new ModelNode();
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(brokerSubsystemPath.toModelNode().resolve());
        readOp.get(NAME).set(BrokerSubsystemExtension.BROKER_ENABLED_ATTR);
        result = services.executeOperation(readOp);
        Assert.assertTrue(checkResultAndGetContents(result).resolve().asBoolean());

        readOp.get(NAME).set(BrokerSubsystemExtension.CUSTOM_CONFIG_ELEMENT);
        result = services.executeOperation(readOp);
        ModelNode content = checkResultAndGetContents(result);
        props = content.asPropertyList();
        Assert.assertEquals(props.size(), 3); // there were 2, but we added "foo" above
        Assert.assertEquals(props.get(0).getName(), "custom-prop");
        Assert.assertEquals(props.get(0).getValue().asString(), "custom-prop-val");
        Assert.assertEquals(props.get(1).getName(), "custom-prop2");
        Assert.assertEquals(props.get(1).getValue().asString(), "custom-prop-val2");
        Assert.assertEquals(props.get(2).getName(), "foo");
        Assert.assertEquals(props.get(2).getValue().asString(), "true");

        // TODO: I think we need to mock the ServerEnvironmentService dependency before we can do this
        // execute status
        // ModelNode statusOp = new ModelNode();
        // statusOp.get(OP).set(BrokerSubsystemExtension.BROKER_STATUS_OP);
        // statusOp.get(OP_ADDR).set(brokerSubsystemPath.toModelNode().resolve());
        // result = services.executeOperation(statusOp);
        // Assert.assertTrue(checkResultAndGetContents(result).asBoolean());
    }
}
