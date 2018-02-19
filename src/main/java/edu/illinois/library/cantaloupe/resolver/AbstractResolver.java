package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;

abstract class AbstractResolver {

    protected Identifier identifier;
    protected Format sourceFormat;
    protected DelegateProxy delegateProxy;

    /**
     * @return Delegate proxy. May be {@literal null}.
     */
    DelegateProxy getDelegateProxy() {
        return delegateProxy;
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
        this.sourceFormat = null;
    }

}
