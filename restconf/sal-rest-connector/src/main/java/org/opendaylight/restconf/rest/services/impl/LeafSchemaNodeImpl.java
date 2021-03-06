/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.rest.services.impl;

import java.util.List;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.util.type.BaseTypes;

/**
 * Special case only use by GET restconf/operations (since moment of old Yang
 * parser and old yang model API removal) to build and use fake leaf like child
 * in container
 *
 */
class LeafSchemaNodeImpl implements LeafSchemaNode {

    private final SchemaPath path;
    private final QName qname;

    /**
     * Base values for fake leaf schema node
     *
     * @param basePath
     *            - schema path
     * @param qname
     *            - qname
     */
    LeafSchemaNodeImpl(final SchemaPath basePath, final QName qname) {
        this.path = basePath.createChild(qname);
        this.qname = qname;
    }

    @Override
    public boolean isAugmenting() {
        return true;
    }

    @Override
    public boolean isAddedByUses() {
        return false;
    }

    @Override
    public boolean isConfiguration() {
        return false;
    }

    @Override
    public ConstraintDefinition getConstraints() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public QName getQName() {
        return this.qname;
    }

    @Override
    public SchemaPath getPath() {
        return this.path;
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported operations.");
    }

    @Override
    public String getReference() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Status getStatus() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TypeDefinition<?> getType() {
        return BaseTypes.emptyType();
    }

    @Override
    public String getDefault() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String getUnits() {
        throw new UnsupportedOperationException("Not supported.");
    }

}
