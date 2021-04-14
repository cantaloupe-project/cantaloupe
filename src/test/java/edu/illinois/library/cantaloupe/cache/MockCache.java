package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;

class MockCache implements DerivativeCache, SourceCache {

    private boolean isCleanUpCalled, isInitializeCalled, isOnCacheWorkerCalled,
            isPurgeInvalidCalled, isShutdownCalled;

    @Override
    public void cleanUp() {
        isCleanUpCalled = true;
    }

    @Override
    public Optional<Info> getInfo(Identifier identifier) {
        return Optional.empty();
    }

    @Override
    public Optional<Path> getSourceImageFile(Identifier identifier)
            throws IOException {
        return Optional.empty();
    }

    @Override
    public void initialize() {
        isInitializeCalled = true;
    }

    boolean isCleanUpCalled() {
        return isCleanUpCalled;
    }

    boolean isInitializeCalled() {
        return isInitializeCalled;
    }

    boolean isOnCacheWorkerCalled() {
        return isOnCacheWorkerCalled;
    }

    boolean isPurgeInvalidCalled() {
        return isPurgeInvalidCalled;
    }

    boolean isShutdownCalled() {
        return isShutdownCalled;
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException {
        return null;
    }

    @Override
    public CompletableOutputStream
    newDerivativeImageOutputStream(OperationList opList) throws IOException {
        return null;
    }

    @Override
    public OutputStream newSourceImageOutputStream(Identifier identifier)
            throws IOException {
        return OutputStream.nullOutputStream();
    }

    @Override
    public void onCacheWorker() {
        DerivativeCache.super.onCacheWorker();
        isOnCacheWorkerCalled = true;
    }

    @Override
    public void purge() {}

    @Override
    public void purge(Identifier identifier) {}

    @Override
    public void purge(OperationList opList) {}

    @Override
    public void purgeInvalid() {
        isPurgeInvalidCalled = true;
    }

    @Override
    public void put(Identifier identifier, Info imageInfo) {}

    @Override
    public void shutdown() {
        isShutdownCalled = true;
    }

}