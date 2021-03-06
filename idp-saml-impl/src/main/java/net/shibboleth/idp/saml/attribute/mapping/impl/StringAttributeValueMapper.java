/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.attribute.mapping.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.saml.attribute.mapping.AbstractSAMLAttributeValueMapper;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.core.xml.XMLObject;

/**
 * Mapping extract a {@link org.opensaml.core.xml.schema.XSString} from an {@link IdPAttributeValue}.
 */
public class StringAttributeValueMapper extends AbstractSAMLAttributeValueMapper {

    /** {@inheritDoc} */
    @Nullable protected IdPAttributeValue<?> decodeValue(final XMLObject object) {
        Constraint.isNotNull(object, "Object supplied to must not be null");
        String value = getStringValue(object);
        if (null == value) {
            return null;
        }
        return StringAttributeValue.valueOf(value);
    }

    /** {@inheritDoc} */
    @Nonnull protected String getAttributeTypeName() {
        return "String";
    }
}
