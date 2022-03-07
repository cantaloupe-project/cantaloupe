package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>Uploads written data to S3 in parts without blocking on uploads.</p>
 *
 * <p>The multi-part upload process involves three types of operations:
 * creating the upload, uploading the parts, and completing the upload. Each of
 * these are encapsulated in {@link Runnable runnable} inner classes. The
 * {@link #write} methods add appropriate instances of these to a queue which
 * is consumed by a worker running in the {@link ThreadPool#getInstance()
 * application thread pool}.</p>
 *
 * <p>Clients will notice that calls to {@link #write} and {@link #close()}
 * (that would otherwise block on communication with S3) return immediately.
 * After {@link #close()} returns, the resulting object will take a little bit
 * of time to appear in the bucket.</p>
 *
 * <p>Multi-part uploads can reduce memory usage when uploading objects larger
 * than the part length, as that is roughly the maximum amount that has to be
 * buffered in memory (provided that the length of the byte array passed to
 * either of the {@link #write} methods is not greater than the part
 * length).</p>
 *
 * @author Alex Dolski UIUC
 * @since 6.0
 */
public class S3MultipartAsyncOutputStream extends CompletableOutputStream {

    private interface TerminalTask {}

    private static class Worker implements Runnable {
        private final BlockingQueue<Runnable> workQueue =
                new LinkedBlockingQueue<>();
        private boolean isDone, isStopped;

        void add(Runnable task) {
            workQueue.add(task);
        }

        void stop() {
            isStopped = true;
        }

        @Override
        public void run() {
            while (!isDone && !isStopped) {
                try {
                    Runnable task = workQueue.take();
                    task.run();
                    if (task instanceof TerminalTask) {
                        isDone = true;
                    }
                } catch (InterruptedException e) {
                    isStopped = true;
                }
            }
        }
    }

    private class RequestCreator implements Runnable {
        private final Logger logger =
                LoggerFactory.getLogger(RequestCreator.class);

        @Override
        public void run() {
            logger.trace("Creating request [bucket: {}] [key: {}]",
                    bucket, key);
            CreateMultipartUploadRequest createMultipartUploadRequest =
                    CreateMultipartUploadRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentEncoding("UTF-8")
                            .build();
            CreateMultipartUploadResponse response =
                    client.createMultipartUpload(createMultipartUploadRequest);
            uploadID = response.uploadId();
        }
    }

    private class PartUploader implements Runnable {
        private final Logger logger =
                LoggerFactory.getLogger(PartUploader.class);

        private final ByteArrayOutputStream part;

        PartUploader(ByteArrayOutputStream part) {
            this.part = part;
        }

        @Override
        public void run() {
            try {
                final int partNumber = partIndex + 1;

                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadID)
                        .partNumber(partNumber)
                        .build();

                // There is a small chance that the last part will be empty.
                if (part.size() == 0) {
                    logger.trace("Skipping empty part {} [upload ID: {}]",
                            partNumber, uploadID);
                    return;
                }

                byte[] bytes = part.toByteArray();

                logger.trace("Uploading part {} ({} bytes) [upload ID: {}]",
                        uploadPartRequest.partNumber(), bytes.length, uploadID);

                String etag = client.uploadPart(
                        uploadPartRequest,
                        RequestBody.fromBytes(bytes)).eTag();
                CompletedPart completedPart = CompletedPart.builder()
                        .partNumber(uploadPartRequest.partNumber())
                        .eTag(etag)
                        .build();
                completedParts.add(completedPart);
            } finally {
                IOUtils.closeQuietly(part);
            }
        }
    }

    private class RequestCompleter implements Runnable, TerminalTask {
        private final Logger logger =
                LoggerFactory.getLogger(RequestCompleter.class);

        @Override
        public void run() {
            try {
                logger.trace("Completing {}-part request [upload ID: {}]",
                        completedParts.size(), uploadID);

                CompletedMultipartUpload completedMultipartUpload =
                        CompletedMultipartUpload.builder()
                                .parts(completedParts)
                                .build();
                CompleteMultipartUploadRequest completeMultipartUploadRequest =
                        CompleteMultipartUploadRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .uploadId(uploadID)
                                .multipartUpload(completedMultipartUpload)
                                .build();
                client.completeMultipartUpload(completeMultipartUploadRequest);
                setComplete(true); // CompletableOutputStream method
            } catch (S3Exception e) {
                logger.warn(e.getMessage());
            } finally {
                if (observer != null) {
                    synchronized (instance) {
                        instance.notifyAll();
                    }
                }
            }
        }
    }

    private class RequestAborter implements Runnable, TerminalTask {
        private final Logger logger =
                LoggerFactory.getLogger(RequestAborter.class);

        @Override
        public void run() {
            try {
                logger.trace("Aborting multipart request [upload ID: {}]",
                        uploadID);

                AbortMultipartUploadRequest abortMultipartUploadRequest =
                        AbortMultipartUploadRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .uploadId(uploadID)
                                .build();
                client.abortMultipartUpload(abortMultipartUploadRequest);
                setComplete(false);
            } catch (S3Exception e) {
                logger.warn(e.getMessage());
            } finally {
                if (observer != null) {
                    synchronized (instance) {
                        instance.notifyAll();
                    }
                }
            }
        }
    }

    /** 5 MB is the minimum allowed by S3 for all but the last part. */
    public static final int MINIMUM_PART_LENGTH = 1024 * 1024 * 5;

    private final S3Client client;
    private final String bucket, key, contentType;

    private ByteArrayOutputStream currentPart;
    private final List<CompletedPart> completedParts = new ArrayList<>();
    private final Worker worker                      = new Worker();
    private boolean requestCreated;

    private String uploadID;
    private int partIndex;
    private long indexWithinPart;

    /** For an instance to wait for an upload notification during testing. */
    Object observer;

    /** Helps notify {@link #observer} of a completed upload during testing. */
    private final S3MultipartAsyncOutputStream instance;

    /**
     * @param client      Client.
     * @param bucket      Target bucket.
     * @param key         Target key.
     * @param contentType Content type of the created object.
     */
    public S3MultipartAsyncOutputStream(S3Client client,
                                        String bucket,
                                        String key,
                                        String contentType) {
        this.client      = client;
        this.bucket      = bucket;
        this.key         = key;
        this.contentType = contentType;
        this.instance    = this;
        ThreadPool.getInstance().submit(worker);
    }

    @Override
    public void close() throws IOException {
        if (isComplete()) {
            worker.add(new PartUploader(getCurrentPart()));
            // The worker will exit after running this.
            worker.add(new RequestCompleter());
        } else {
            // The worker will exit after running this.
            worker.add(new RequestAborter());
        }
    }

    @Override
    public void write(int b) throws IOException {
        ByteArrayOutputStream part = getCurrentPart();
        part.write(b);
        indexWithinPart++;
        createRequestIfNecessary();
        uploadPartIfNecessary();
    }

    @Override
    public void write(byte[] b) throws IOException {
        ByteArrayOutputStream part = getCurrentPart();
        part.write(b);
        indexWithinPart += b.length;
        createRequestIfNecessary();
        uploadPartIfNecessary();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ByteArrayOutputStream part = getCurrentPart();
        part.write(b, off, len);
        indexWithinPart += len;
        createRequestIfNecessary();
        uploadPartIfNecessary();
    }

    private ByteArrayOutputStream getCurrentPart() {
        if (currentPart == null) {
            currentPart = new ByteArrayOutputStream();
        }
        return currentPart;
    }

    private void createRequestIfNecessary() {
        if (!requestCreated) {
            worker.add(new RequestCreator());
            requestCreated = true;
        }
    }

    private void uploadPartIfNecessary() {
        if (indexWithinPart >= MINIMUM_PART_LENGTH) {
            worker.add(new PartUploader(currentPart));
            IOUtils.closeQuietly(currentPart);
            currentPart     = null;
            indexWithinPart = 0;
            partIndex++;
        }
    }

}
