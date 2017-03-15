package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

class MockCache implements DerivativeCache, SourceCache {

    private boolean initializeCalled = false;
    private boolean shutdownCalled = false;

    @Override
    public Info getImageInfo(Identifier identifier) throws CacheException {
        return null;
    }

    @Override
    public File getSourceImageFile(Identifier identifier)
            throws CacheException {
        return null;
    }

    @Override
    public void initialize() {
        initializeCalled = true;
    }

    boolean isInitializeCalled() {
        return initializeCalled;
    }

    boolean isShutdownCalled() {
        return shutdownCalled;
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws CacheException {
        return null;
    }

    @Override
    public OutputStream newDerivativeImageOutputStream(OperationList opList)
            throws CacheException {
        return null;
    }

    @Override
    public OutputStream newSourceImageOutputStream(Identifier identifier)
            throws CacheException {
        return null;
    }

    @Override
    public void purge() throws CacheException {}

    @Override
    public void purge(Identifier identifier) throws CacheException {}

    @Override
    public void purge(OperationList opList) throws CacheException {}

    @Override
    public void purgeExpired() throws CacheException {}

    @Override
    public void put(Identifier identifier, Info imageInfo)
            throws CacheException {}

    @Override
    public void shutdown() {
        shutdownCalled = true;
    }

}