/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.rest.impl;

import com.google.common.base.Optional;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.opendaylight.netconf.sal.rest.api.Draft02;
import org.opendaylight.netconf.sal.rest.api.RestconfNormalizedNodeWriter;
import org.opendaylight.netconf.sal.rest.api.RestconfService;
import org.opendaylight.netconf.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.netconf.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.restconf.Draft17;
import org.opendaylight.restconf.utils.RestconfConstants;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Provider
@Produces({ Draft02.MediaTypes.API + RestconfService.JSON, Draft02.MediaTypes.DATA + RestconfService.JSON,
        Draft02.MediaTypes.OPERATION + RestconfService.JSON, Draft17.MediaTypes.DATA + RestconfConstants.JSON,
        MediaType.APPLICATION_JSON })
public class NormalizedNodeJsonBodyWriter implements MessageBodyWriter<NormalizedNodeContext> {

    private static final int DEFAULT_INDENT_SPACES_NUM = 2;

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return type.equals(NormalizedNodeContext.class);
    }

    @Override
    public long getSize(final NormalizedNodeContext t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final NormalizedNodeContext t, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
                    throws IOException, WebApplicationException {
        final NormalizedNode<?, ?> data = t.getData();
        if (data == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        final InstanceIdentifierContext<SchemaNode> context = (InstanceIdentifierContext<SchemaNode>) t.getInstanceIdentifierContext();

        final SchemaPath path = context.getSchemaNode().getPath();
        final JsonWriter jsonWriter = createJsonWriter(entityStream, t.getWriterParameters().isPrettyPrint());
        jsonWriter.beginObject();
        writeNormalizedNode(jsonWriter,path,context,data, t.getWriterParameters().getDepth());
        jsonWriter.endObject();
        jsonWriter.flush();
    }

    private void writeNormalizedNode(final JsonWriter jsonWriter, SchemaPath path,
            final InstanceIdentifierContext<SchemaNode> context, NormalizedNode<?, ?> data, final Optional<Integer> depth) throws
            IOException {
        final RestconfNormalizedNodeWriter nnWriter;
        if (SchemaPath.ROOT.equals(path)) {
            /*
             *  Creates writer without initialNs and we write children of root data container
             *  which is not visible in restconf
             */
            nnWriter = createNormalizedNodeWriter(context,path,jsonWriter, depth);
            writeChildren(nnWriter,(ContainerNode) data);
        } else if (context.getSchemaNode() instanceof RpcDefinition) {
            /*
             *  RpcDefinition is not supported as initial codec in JSONStreamWriter,
             *  so we need to emit initial output declaratation..
             */
            path = ((RpcDefinition) context.getSchemaNode()).getOutput().getPath();
            nnWriter = createNormalizedNodeWriter(context,path,jsonWriter, depth);
            jsonWriter.name("output");
            jsonWriter.beginObject();
            writeChildren(nnWriter, (ContainerNode) data);
            jsonWriter.endObject();
        } else {
            path = path.getParent();

            if(data instanceof MapEntryNode) {
                data = ImmutableNodes.mapNodeBuilder(data.getNodeType()).withChild(((MapEntryNode) data)).build();
            }
            nnWriter = createNormalizedNodeWriter(context,path,jsonWriter, depth);
            nnWriter.write(data);
        }
        nnWriter.flush();
    }

    private void writeChildren(final RestconfNormalizedNodeWriter nnWriter, final ContainerNode data) throws IOException {
        for(final DataContainerChild<? extends PathArgument, ?> child : data.getValue()) {
            nnWriter.write(child);
        }
    }

    private RestconfNormalizedNodeWriter createNormalizedNodeWriter(final InstanceIdentifierContext<SchemaNode> context,
            final SchemaPath path, final JsonWriter jsonWriter, final Optional<Integer> depth) {

        final SchemaNode schema = context.getSchemaNode();
        final JSONCodecFactory codecs = getCodecFactory(context);

        final URI initialNs;
        if ((schema instanceof DataSchemaNode)
                && !((DataSchemaNode)schema).isAugmenting()
                && !(schema instanceof SchemaContext)) {
            initialNs = schema.getQName().getNamespace();
        } else if (schema instanceof RpcDefinition) {
            initialNs = schema.getQName().getNamespace();
        } else {
            initialNs = null;
        }
        final NormalizedNodeStreamWriter streamWriter = JSONNormalizedNodeStreamWriter.createNestedWriter(codecs,path,initialNs,jsonWriter);
        if (depth.isPresent()) {
            return DepthAwareNormalizedNodeWriter.forStreamWriter(streamWriter, depth.get());
        } else {
            return RestconfDelegatingNormalizedNodeWriter.forStreamWriter(streamWriter);
        }
    }

    private JsonWriter createJsonWriter(final OutputStream entityStream, final boolean prettyPrint) {
        if (prettyPrint) {
            return JsonWriterFactory.createJsonWriter(new OutputStreamWriter(entityStream, StandardCharsets.UTF_8),
                    DEFAULT_INDENT_SPACES_NUM);
        } else {
            return JsonWriterFactory.createJsonWriter(new OutputStreamWriter(entityStream, StandardCharsets.UTF_8));
        }
    }

    private JSONCodecFactory getCodecFactory(final InstanceIdentifierContext<?> context) {
        // TODO: Performance: Cache JSON Codec factory and schema context
        return JSONCodecFactory.create(context.getSchemaContext());
    }

}
