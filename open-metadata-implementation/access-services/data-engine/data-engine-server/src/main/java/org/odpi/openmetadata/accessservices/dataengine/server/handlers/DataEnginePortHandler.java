/* SPDX-License-Identifier: Apache 2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.accessservices.dataengine.server.handlers;

import org.odpi.openmetadata.accessservices.dataengine.ffdc.DataEngineErrorCode;
import org.odpi.openmetadata.accessservices.dataengine.model.Port;
import org.odpi.openmetadata.accessservices.dataengine.model.PortAlias;
import org.odpi.openmetadata.accessservices.dataengine.model.PortImplementation;
import org.odpi.openmetadata.accessservices.dataengine.model.PortType;
import org.odpi.openmetadata.accessservices.dataengine.server.mappers.CommonMapper;
import org.odpi.openmetadata.accessservices.dataengine.server.mappers.ProcessPropertiesMapper;
import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
import org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper;
import org.odpi.openmetadata.commonservices.generichandlers.PortBuilder;
import org.odpi.openmetadata.commonservices.generichandlers.PortHandler;
import org.odpi.openmetadata.commonservices.repositoryhandler.RepositoryHandler;
import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetailDifferences;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EnumPropertyValue;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;

import java.util.Optional;

/**
 * PortHandler manages Port objects from the property server. It runs server-side in the DataEngine OMAS
 * and creates port entities with wire relationships through the OMRSRepositoryConnector.
 */
public class DataEnginePortHandler {
    private final String serviceName;
    private final String serverName;
    private final RepositoryHandler repositoryHandler;
    private final OMRSRepositoryHelper repositoryHelper;
    private final InvalidParameterHandler invalidParameterHandler;
    private final DataEngineCommonHandler dataEngineCommonHandler;
    private final PortHandler<Port> portHandler;
    private final DataEngineRegistrationHandler registrationHandler;

    private final static String processGUIDParameterName = "processGUID";
    private final static String portGUIDParameterName = "portGUID";
    private final static String schemaTypeGUIDParameterName = "schemaTypeGUID";

    /**
     * Construct the handler information needed to interact with the repository services
     *
     * @param serviceName             name of this service
     * @param serverName              name of the local server
     * @param invalidParameterHandler handler for managing parameter errors
     * @param repositoryHandler       manages calls to the repository services
     * @param repositoryHelper        provides utilities for manipulating the repository services objects
     * @param dataEngineCommonHandler provides utilities for manipulating entities
     * @param portHandler             provides utilities for manipulating the repository services ports
     * @param registrationHandler     provides utilities for manipulating software server capability entities
     */
    public DataEnginePortHandler(String serviceName, String serverName, InvalidParameterHandler invalidParameterHandler,
                                 RepositoryHandler repositoryHandler, OMRSRepositoryHelper repositoryHelper,
                                 DataEngineCommonHandler dataEngineCommonHandler, PortHandler<Port> portHandler,
                                 DataEngineRegistrationHandler registrationHandler) {
        this.serviceName = serviceName;
        this.serverName = serverName;
        this.invalidParameterHandler = invalidParameterHandler;
        this.repositoryHelper = repositoryHelper;
        this.repositoryHandler = repositoryHandler;
        this.dataEngineCommonHandler = dataEngineCommonHandler;
        this.portHandler = portHandler;
        this.registrationHandler = registrationHandler;
    }

    /**
     * Create the port implementation attached to a process.
     *
     * @param userId             the name of the calling user
     * @param portImplementation the port implementation values
     * @param processGUID        the unique identifier of the process
     * @param externalSourceName the unique name of the external source
     *
     * @return unique identifier of the port implementation in the repository
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public String createPortImplementation(String userId, PortImplementation portImplementation, String processGUID, String externalSourceName) throws
                                                                                                                                                InvalidParameterException,
                                                                                                                                                UserNotAuthorizedException,
                                                                                                                                                PropertyServerException {
        return createPort(userId, portImplementation, OpenMetadataAPIMapper.PORT_IMPLEMENTATION_TYPE_NAME, processGUID, externalSourceName);
    }

    /**
     * Create the port alias and attach it to the process.
     *
     * @param userId             the name of the calling user
     * @param portAlias          the port alias values
     * @param processGUID        the unique identifier of the process
     * @param externalSourceName the unique name of the external source
     *
     * @return unique identifier of the port alias in the repository
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public String createPortAlias(String userId, PortAlias portAlias, String processGUID, String externalSourceName) throws InvalidParameterException,
                                                                                                                            UserNotAuthorizedException,
                                                                                                                            PropertyServerException {
        return createPort(userId, portAlias, OpenMetadataAPIMapper.PORT_ALIAS_TYPE_NAME, processGUID, externalSourceName);
    }

    /**
     * Update the port implementation
     *
     * @param userId             the name of the calling user
     * @param originalPortEntity the created port entity
     * @param portImplementation the port implementation new values
     * @param externalSourceName the external data engine
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public void updatePortImplementation(String userId, EntityDetail originalPortEntity, PortImplementation portImplementation,
                                         String externalSourceName) throws InvalidParameterException, UserNotAuthorizedException,
                                                                           PropertyServerException {
        updatePort(userId, originalPortEntity, portImplementation, OpenMetadataAPIMapper.PORT_IMPLEMENTATION_TYPE_NAME, externalSourceName);
    }

    /**
     * Update the port alias
     *
     * @param userId             the name of the calling user
     * @param originalPortEntity the created port entity
     * @param portAlias          the port alias new values
     * @param externalSourceName the external data engine
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public void updatePortAlias(String userId, EntityDetail originalPortEntity, PortAlias portAlias, String externalSourceName) throws
                                                                                                                                InvalidParameterException,
                                                                                                                                UserNotAuthorizedException,
                                                                                                                                PropertyServerException {
        updatePort(userId, originalPortEntity, portAlias, OpenMetadataAPIMapper.PORT_ALIAS_TYPE_NAME, externalSourceName);
    }

    /**
     * Create the port and attach it to the process.
     *
     * @param userId             the name of the calling user
     * @param port               the port values
     * @param entityTpeName      the type name
     * @param externalSourceName the unique name of the external source
     * @param processGUID        the unique identifier of the process containing the port
     *
     * @return unique identifier of the port in the repository
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    private String createPort(String userId, Port port, String entityTpeName, String processGUID, String externalSourceName) throws
                                                                                                                             InvalidParameterException,
                                                                                                                             UserNotAuthorizedException,
                                                                                                                             PropertyServerException {
        final String methodName = "createPort";
        validatePortParameters(userId, port.getQualifiedName(), port.getDisplayName(), methodName);
        invalidParameterHandler.validateGUID(processGUID, CommonMapper.GUID_PROPERTY_NAME, methodName);

        String externalSourceGUID = registrationHandler.getExternalDataEngineByQualifiedName(userId, externalSourceName);
        return portHandler.createPort(userId, externalSourceGUID, externalSourceName, processGUID, processGUIDParameterName, port.getQualifiedName(),
                port.getDisplayName(), port.getPortType().getOrdinal(), port.getAdditionalProperties(), entityTpeName, null, methodName);
    }

    /**
     * Update the port
     *
     * @param userId             the name of the calling user
     * @param originalPortEntity the created port entity
     * @param port               the port values
     * @param entityTypeName     the type name
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    private void updatePort(String userId, EntityDetail originalPortEntity, Port port, String entityTypeName, String externalSourceName) throws
                                                                                                                                         InvalidParameterException,
                                                                                                                                         UserNotAuthorizedException,
                                                                                                                                         PropertyServerException {
        final String methodName = "updatePort";
        validatePortParameters(userId, port.getQualifiedName(), port.getDisplayName(), methodName);
        String portGUID = originalPortEntity.getGUID();

        String externalSourceGUID = registrationHandler.getExternalDataEngineByQualifiedName(userId, externalSourceName);
        PortBuilder updatedPortBuilder = new PortBuilder(port.getQualifiedName(), port.getDisplayName(), port.getPortType().getOrdinal(),
                port.getAdditionalProperties(), externalSourceGUID, externalSourceName, null, repositoryHelper, serviceName, serverName);
        EntityDetail updatedPortEntity = dataEngineCommonHandler.buildEntityDetail(portGUID, updatedPortBuilder.getInstanceProperties(methodName));
        EntityDetailDifferences entityDetailDifferences = repositoryHelper.getEntityDetailDifferences(originalPortEntity, updatedPortEntity, true);
        if (!entityDetailDifferences.hasInstancePropertiesDifferences()) {
            return;
        }

        portHandler.updatePort(userId, externalSourceGUID, externalSourceName, portGUID, portGUIDParameterName, port.getQualifiedName(),
                port.getDisplayName(), port.getPortType().getOrdinal(), port.getAdditionalProperties(), entityTypeName, null, methodName);
    }

    /**
     * Create a PortSchema relationship between a Port and the corresponding SchemaType. Verifies that the
     * relationship is not present before creating it
     *
     * @param userId             the name of the calling user
     * @param portGUID           the unique identifier of the port
     * @param schemaTypeGUID     the unique identifier of the schema type
     * @param externalSourceName the unique name of the external source
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public void addPortSchemaRelationship(String userId, String portGUID, String schemaTypeGUID, String externalSourceName) throws
                                                                                                                            InvalidParameterException,
                                                                                                                            UserNotAuthorizedException,
                                                                                                                            PropertyServerException {
        final String methodName = "addPortSchemaRelationship";
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(portGUID, CommonMapper.GUID_PROPERTY_NAME, methodName);
        invalidParameterHandler.validateGUID(schemaTypeGUID, CommonMapper.GUID_PROPERTY_NAME, methodName);

        if (!dataEngineCommonHandler.findRelationship(userId, portGUID, schemaTypeGUID, OpenMetadataAPIMapper.PORT_TYPE_NAME,
                OpenMetadataAPIMapper.PORT_SCHEMA_RELATIONSHIP_TYPE_NAME).isPresent()) {
            String externalSourceGUID = registrationHandler.getExternalDataEngineByQualifiedName(userId, externalSourceName);
            portHandler.setupPortSchemaType(userId, externalSourceGUID, externalSourceName, portGUID, portGUIDParameterName,
                    schemaTypeGUID, schemaTypeGUIDParameterName, methodName);
        }
    }

    /**
     * Retrieve the schema type that is linked to the port
     *
     * @param userId   the name of the calling user
     * @param portGUID the unique identifier of the port
     *
     * @return retrieved entity or an empty optional
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public Optional<EntityDetail> findSchemaTypeForPort(String userId, String portGUID) throws InvalidParameterException,
                                                                                               UserNotAuthorizedException,
                                                                                               PropertyServerException {
        final String methodName = "findSchemaTypeForPort";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(portGUID, ProcessPropertiesMapper.GUID_PROPERTY_NAME, methodName);

        TypeDef relationshipTypeDef = repositoryHelper.getTypeDefByName(userId, OpenMetadataAPIMapper.PORT_SCHEMA_RELATIONSHIP_TYPE_NAME);

        return Optional.of(repositoryHandler.getEntityForRelationshipType(userId, portGUID, OpenMetadataAPIMapper.PORT_TYPE_NAME,
                relationshipTypeDef.getGUID(), relationshipTypeDef.getName(), methodName));
    }

    /**
     * Create a PortDelegation relationship between two ports. Verifies that the relationship is not present
     * before creating it
     *
     * @param userId                   the name of the calling user
     * @param portGUID                 the unique identifier of the source port
     * @param portType                 the type of the source port
     * @param delegatesToQualifiedName the unique identifier of the target port
     * @param externalSourceName       the unique name of the external source
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public void addPortDelegationRelationship(String userId, String portGUID, PortType portType, String delegatesToQualifiedName,
                                              String externalSourceName) throws InvalidParameterException,
                                                                                UserNotAuthorizedException,
                                                                                PropertyServerException {
        final String methodName = "addPortDelegationRelationship";
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateName(delegatesToQualifiedName, OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME, methodName);

        Optional<EntityDetail> delegatedPortEntity = findPortEntity(userId, delegatesToQualifiedName);
        if (!delegatedPortEntity.isPresent()) {
            dataEngineCommonHandler.throwInvalidParameterException(DataEngineErrorCode.PORT_NOT_FOUND, methodName, delegatesToQualifiedName);
        }

        String delegatedPortType = getPortType(delegatedPortEntity.get());
        if (!portType.getName().equalsIgnoreCase(delegatedPortType)) {
            dataEngineCommonHandler.throwInvalidParameterException(DataEngineErrorCode.INVALID_PORT_TYPE, methodName, delegatesToQualifiedName,
                    delegatedPortType);
        }

        String delegatedPortGUID = delegatedPortEntity.get().getGUID();
        if (!dataEngineCommonHandler.findRelationship(userId, portGUID, delegatedPortGUID, OpenMetadataAPIMapper.PORT_TYPE_NAME,
                OpenMetadataAPIMapper.PORT_DELEGATION_TYPE_NAME).isPresent()) {
            String externalSourceGUID = registrationHandler.getExternalDataEngineByQualifiedName(userId, externalSourceName);
            portHandler.setupPortDelegation(userId, externalSourceGUID, externalSourceName, portGUID, portGUIDParameterName, delegatedPortGUID,
                    portGUIDParameterName, methodName);
        }
    }

    /**
     * Remove the port
     *
     * @param userId             the name of the calling user
     * @param qualifiedName      qualified name of the port to be removed
     * @param externalSourceName the external data engine
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public void removePort(String userId, String qualifiedName, String externalSourceName) throws InvalidParameterException,
                                                                                                  PropertyServerException,
                                                                                                  UserNotAuthorizedException {
        final String methodName = "removePort";
        Optional<EntityDetail> portOptional = findPortEntity(userId, qualifiedName);
        if (portOptional.isPresent()) {
            String externalSourceGUID = registrationHandler.getExternalDataEngineByQualifiedName(userId, externalSourceName);
            portHandler.removePort(userId, externalSourceGUID, externalSourceName, portOptional.get().getGUID(), portGUIDParameterName, methodName);
        }
    }

    /**
     * Find out if the PortImplementation object is already stored in the repository. It uses the fully qualified name to retrieve the entity
     *
     * @param userId        the name of the calling user
     * @param qualifiedName the qualifiedName name of the process to be searched
     *
     * @return optional with entity details if found, empty optional if not found
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public Optional<EntityDetail> findPortImplementationEntity(String userId, String qualifiedName) throws InvalidParameterException,
                                                                                                           UserNotAuthorizedException,
                                                                                                           PropertyServerException {
        return dataEngineCommonHandler.findEntity(userId, qualifiedName, OpenMetadataAPIMapper.PORT_IMPLEMENTATION_TYPE_NAME);
    }

    /**
     * Find out if the PortAlias object is already stored in the repository. It uses the fully qualified name to retrieve the entity
     *
     * @param userId        the name of the calling user
     * @param qualifiedName the qualifiedName name of the process to be searched
     *
     * @return optional with entity details if found, empty optional if not found
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public Optional<EntityDetail> findPortAliasEntity(String userId, String qualifiedName) throws InvalidParameterException,
                                                                                                  UserNotAuthorizedException,
                                                                                                  PropertyServerException {
        return dataEngineCommonHandler.findEntity(userId, qualifiedName, OpenMetadataAPIMapper.PORT_ALIAS_TYPE_NAME);
    }

    /**
     * Find out if the Port object is already stored in the repository. It uses the fully qualified name to retrieve the entity.
     *
     * @param userId        the name of the calling user
     * @param qualifiedName the qualifiedName name of the process to be searched
     *
     * @return optional with entity details if found, empty optional if not found
     *
     * @throws InvalidParameterException  the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public Optional<EntityDetail> findPortEntity(String userId, String qualifiedName) throws InvalidParameterException,
                                                                                             UserNotAuthorizedException,
                                                                                             PropertyServerException {
        Optional<EntityDetail> portEntity = findPortAliasEntity(userId, qualifiedName);
        if (!portEntity.isPresent()) {
            portEntity = findPortImplementationEntity(userId, qualifiedName);
        }
        return portEntity;
    }

    private String getPortType(EntityDetail delegatedPort) {
        if (delegatedPort == null) {
            return null;
        }

        InstanceProperties instanceProperties = delegatedPort.getProperties();
        if (instanceProperties == null) {
            return null;
        }

        EnumPropertyValue portTypeValue =
                (EnumPropertyValue) delegatedPort.getProperties().getPropertyValue(OpenMetadataAPIMapper.PORT_TYPE_PROPERTY_NAME);
        if (portTypeValue == null) {
            return null;
        }

        return portTypeValue.getSymbolicName();
    }

    private void validatePortParameters(String userId, String qualifiedName, String displayName, String methodName) throws InvalidParameterException {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateName(displayName, OpenMetadataAPIMapper.DISPLAY_NAME_PROPERTY_NAME, methodName);
        invalidParameterHandler.validateName(qualifiedName, OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME, methodName);
    }
}