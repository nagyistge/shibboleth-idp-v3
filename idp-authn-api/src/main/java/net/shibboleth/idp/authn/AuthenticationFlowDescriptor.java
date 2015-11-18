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

package net.shibboleth.idp.authn;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.authn.principal.PrincipalSupportingComponent;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageSerializer;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

/**
 * A descriptor for an authentication flow.
 * 
 * <p>
 * A flow models a sequence of profile actions that performs authentication in a particular way and satisfies various
 * constraints that may apply to an authentication request. Some of these constraints are directly exposed as properties
 * of the flow, and others can be found by examining the list of extended {@link Principal}s that the flow exposes.
 * </p>
 */
public class AuthenticationFlowDescriptor extends AbstractIdentifiableInitializableComponent implements
        PrincipalSupportingComponent, Predicate<ProfileRequestContext>, StorageSerializer<AuthenticationResult> {

    /** Prefix convention for flow IDs. */
    @Nonnull @NotEmpty public static final String FLOW_ID_PREFIX = "authn/";

    /** Additional allowance for storage of result records to avoid race conditions during use. */
    public static final long STORAGE_EXPIRATION_OFFSET;

    /** Whether this flow supports non-browser clients. */
    private boolean supportsNonBrowser;
    
    /** Whether this flow supports passive authentication. */
    private boolean supportsPassive;

    /** Whether this flow supports forced authentication. */
    private boolean supportsForced;

    /** Maximum amount of time in milliseconds, since first usage, a flow should be considered active. */
    @Duration @NonNegative private long lifetime;

    /** Maximum amount of time in milliseconds, since last usage, a flow should be considered active. */
    @Duration @Positive private long inactivityTimeout;

    /**
     * Supported principals, indexed by type, that the flow can produce. Implemented for the moment using the Subject
     * class for convenience to allow for class-based lookup in the {@link #getSupportedPrincipals} method.
     */
    @Nonnull private Subject supportedPrincipals;

    /** Predicate that must be true for this flow to be usable for a given request. */
    @Nonnull private Predicate<ProfileRequestContext> activationCondition;
    
    /** Custom serializer for the results generated by this flow. */
    @Nullable private StorageSerializer<AuthenticationResult> resultSerializer;

    /** Constructor. */
    public AuthenticationFlowDescriptor() {
        supportsNonBrowser = true;
        supportedPrincipals = new Subject();
        activationCondition = Predicates.alwaysTrue();
        inactivityTimeout = 30 * 60 * 1000;
    }
    
    /**
     * Get whether this flow supports non-browser clients.
     * 
     * @return whether this flow supports non-browser clients
     */
    public boolean isNonBrowserSupported() {
        return supportsNonBrowser;
    }
    
    /**
     * Set whether this flow supports non-browser clients.
     * 
     * @param isSupported whether this flow supports non-browser clients
     */
    public void setNonBrowserSupported(final boolean isSupported) {
        supportsNonBrowser = isSupported;
    }

    /**
     * Get whether this flow supports passive authentication.
     * 
     * @return whether this flow supports passive authentication
     */
    public boolean isPassiveAuthenticationSupported() {
        return supportsPassive;
    }

    /**
     * Set whether this flow supports passive authentication.
     * 
     * @param isSupported whether this flow supports passive authentication
     */
    public void setPassiveAuthenticationSupported(final boolean isSupported) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        supportsPassive = isSupported;
    }

    /**
     * Get whether this flow supports forced authentication.
     * 
     * @return whether this flow supports forced authentication
     */
    public boolean isForcedAuthenticationSupported() {
        return supportsForced;
    }

    /**
     * Set whether this flow supports forced authentication.
     * 
     * @param isSupported whether this flow supports forced authentication.
     */
    public void setForcedAuthenticationSupported(final boolean isSupported) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        supportsForced = isSupported;
    }

    /**
     * Get the maximum amount of time in milliseconds, since first usage, a flow should be considered active. A value
     * of 0 indicates that there is no upper limit on the lifetime on an active flow.
     * 
     * @return maximum amount of time in milliseconds a flow should be considered active, never less than 0
     */
    @NonNegative public long getLifetime() {
        return lifetime;
    }

    /**
     * Set the maximum amount of time in milliseconds, since first usage, a flow should be considered active. A value
     * of 0 indicates that there is no upper limit on the lifetime on an active flow.
     * 
     * @param flowLifetime the lifetime for the flow, must be 0 or greater
     */
    public void setLifetime(@Duration @NonNegative final long flowLifetime) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        lifetime = Constraint.isGreaterThanOrEqual(0, flowLifetime, "Lifetime must be greater than or equal to 0");
    }

    /**
     * Get the maximum amount of time in milliseconds, since the last usage, a flow should be considered active.
     * 
     * <p>
     * Defaults to 30 minutes.
     * </p>
     * 
     * @return the duration
     */
    @Positive public long getInactivityTimeout() {
        return inactivityTimeout;
    }

    /**
     * Set the maximum amount of time in milliseconds, since the last usage, a flow should be considered active.
     * 
     * @param timeout the flow inactivity timeout, must be greater than zero
     */
    public void setInactivityTimeout(@Duration @Positive final long timeout) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        inactivityTimeout = Constraint.isGreaterThan(0, timeout, "Inactivity timeout must be greater than 0");
    }

    /**
     * Check if a result generated by this flow is still active.
     * 
     * @param result {@link AuthenticationResult} to check
     * 
     * @return true iff the result remains valid
     */
    public boolean isResultActive(@Nonnull final AuthenticationResult result) {
        Constraint.isNotNull(result, "AuthenticationResult cannot be null");
        Constraint.isTrue(result.getAuthenticationFlowId().equals(getId()),
                "AuthenticationResult was not produced by this flow");

        long now = System.currentTimeMillis();
        if (getLifetime() > 0 && result.getAuthenticationInstant() + getLifetime() <= now) {
            return false;
        } else if (getInactivityTimeout() > 0 && result.getLastActivityInstant() + getInactivityTimeout() <= now) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NonnullElements @Unmodifiable public <T extends Principal> Set<T> getSupportedPrincipals(
            @Nonnull final Class<T> c) {
        return supportedPrincipals.getPrincipals(c);
    }

    /**
     * Get a collection of supported non-user-specific principals that the flow may produce when it operates.
     * 
     * <p>
     * The {@link Collection#remove(java.lang.Object)} method is not supported.
     * </p>
     * 
     * @return a live collection of supported principals
     */
    @Nonnull @NonnullElements public Collection<Principal> getSupportedPrincipals() {
        return Collections2.filter(supportedPrincipals.getPrincipals(), Predicates.notNull());
    }

    /**
     * Set supported non-user-specific principals that the flow may produce when it operates.
     * 
     * @param <T> a type of principal to add, if not generic
     * @param principals supported principals to add
     */
    public <T extends Principal> void setSupportedPrincipals(@Nonnull @NonnullElements final Collection<T> principals) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(principals, "Principal collection cannot be null.");

        supportedPrincipals.getPrincipals().clear();
        supportedPrincipals.getPrincipals().addAll(Collections2.filter(principals, Predicates.notNull()));
    }

    /**
     * Set the activation condition in the form of a {@link Predicate} such that iff the condition evaluates to true
     * should the corresponding flow be allowed/possible.
     * 
     * @param condition predicate that controls activation of the flow
     */
    public void setActivationCondition(@Nonnull final Predicate<ProfileRequestContext> condition) {
        activationCondition = Constraint.isNotNull(condition, "Activation condition predicate cannot be null");
    }

    /** {@inheritDoc} */
    @Override public boolean apply(ProfileRequestContext input) {
        return activationCondition.apply(input);
    }
    
    /**
     * Set a custom serializer for results produced by this flow.
     * 
     * @param serializer the custom serializer
     */
    public void setResultSerializer(@Nonnull final StorageSerializer<AuthenticationResult> serializer) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        resultSerializer = Constraint.isNotNull(serializer, "StorageSerializer cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (resultSerializer == null) {
            throw new ComponentInitializationException("AuthenticationResult serializer cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NotEmpty public String serialize(@Nonnull final AuthenticationResult instance)
            throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        return resultSerializer.serialize(instance);
    }

    /** {@inheritDoc} */
    @Override @Nonnull public AuthenticationResult deserialize(long version, @Nonnull @NotEmpty final String context,
            @Nonnull @NotEmpty final String key, @Nonnull @NotEmpty final String value, @Nonnull final Long expiration)
            throws IOException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        // Back the expiration off by the inactivity timeout to recover the last activity time.
        return resultSerializer.deserialize(version, context, key, value, (expiration != null) ? expiration
                - inactivityTimeout - STORAGE_EXPIRATION_OFFSET : null);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return getId().hashCode();
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof AuthenticationFlowDescriptor) {
            return getId().equals(((AuthenticationFlowDescriptor) obj).getId());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("flowId", getId()).add("supportsPassive", supportsPassive)
                .add("supportsForcedAuthentication", supportsForced).add("lifetime", lifetime)
                .add("inactivityTimeout", inactivityTimeout).toString();
    }

    static {
        STORAGE_EXPIRATION_OFFSET = 10 * 60 * 1000;
    }
}