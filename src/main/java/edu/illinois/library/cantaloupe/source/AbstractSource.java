package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;

abstract class AbstractSource {

    protected Identifier identifier;
    protected DelegateProxy delegateProxy;

    /**
     * @return Delegate proxy. May be {@literal null}.
     */
    protected DelegateProxy getDelegateProxy() {
        return delegateProxy;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public void setDelegateProxy(DelegateProxy proxy) {
        this.delegateProxy = proxy;
    }

    /**
     * <p>Sets the identifier used by the instance.</p>
     *
     * <p>N.B.: The identifier property of the {@link #getDelegateProxy()
     * delegate proxy}'s {@link RequestContext} must also be set.</p>
     */
    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

}
