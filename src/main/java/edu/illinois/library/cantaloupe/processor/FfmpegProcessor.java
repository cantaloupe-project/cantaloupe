package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFacade;
import edu.illinois.library.cantaloupe.util.CommandLocator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processor using the {@literal ffmpeg} command-line tool to extract video
 * frames, and the {@literal ffprobe} tool to get video information. Works with
 * ffmpeg 2.8 (other versions untested).
 */
class FfmpegProcessor extends AbstractProcessor implements FileProcessor {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(FfmpegProcessor.class);

    private static final String FFMPEG_NAME   = "ffmpeg";
    private static final String FFPROBE_NAME  = "ffprobe";
    private static final Pattern TIME_PATTERN =
            Pattern.compile("[0-9][0-9]:[0-5][0-9]:[0-5][0-9]");

    private static final AtomicBoolean IS_INITIALIZATION_ATTEMPTED =
            new AtomicBoolean(false);
    private static String initializationError;

    private Path sourceFile;

    private double durationSec = 0;
    private Info info;

    /**
     * @param binaryName Name of one of the ffmpeg binaries.
     */
    private static String getPath(String binaryName) {
        String searchPath = Configuration.getInstance().
                getString(Key.FFMPEGPROCESSOR_PATH_TO_BINARIES);
        return CommandLocator.locate(binaryName, searchPath);
    }

    private static synchronized void initialize() {
        IS_INITIALIZATION_ATTEMPTED.set(true);
        try {
            // Check for the presence of ffprobe and ffmpeg.
            invoke(FFPROBE_NAME);
            invoke(FFMPEG_NAME);
        } catch (IOException e) {
            initializationError = e.getMessage();
        }
    }

    private static void invoke(String ffmpegBinary) throws IOException {
        final ProcessBuilder pb = new ProcessBuilder();
        List<String> command = new ArrayList<>();
        command.add(getPath(ffmpegBinary));
        pb.command(command);
        String commandString = String.join(" ", pb.command());
        LOGGER.trace("invoke(): {}", commandString);
        pb.start();
    }

    /**
     * For testing only!
     */
    static synchronized void resetInitialization() {
        IS_INITIALIZATION_ATTEMPTED.set(false);
        initializationError = null;
    }

    FfmpegProcessor() {
        if (!IS_INITIALIZATION_ATTEMPTED.get()) {
            initialize();
        }
    }

    @Override
    public void close() {
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        final Set<Format> outputFormats;
        if (getSourceFormat().isVideo()) {
            outputFormats = ImageWriterFactory.supportedFormats();
        } else {
            outputFormats = Collections.unmodifiableSet(Collections.emptySet());
        }
        return outputFormats;
    }

    @Override
    public String getInitializationError() {
        if (!IS_INITIALIZATION_ATTEMPTED.get()) {
            initialize();
        }
        return initializationError;
    }

    @Override
    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public void process(final OperationList opList,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException, FormatException {
        super.process(opList, imageInfo, outputStream);

        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ProcessBuilder pb = getProcessBuilder(opList);
            LOGGER.trace("Invoking {}", String.join(" ", pb.command()));
            final Process process = pb.start();

            try (final InputStream processInputStream = process.getInputStream();
                 final InputStream processErrorStream = process.getErrorStream()) {
                ThreadPool.getInstance().submit(
                        new StreamCopier(processErrorStream, errorBucket));

                final ImageReader reader = new ImageReaderFactory().newImageReader(
                        Format.get("bmp"), processInputStream);
                try {
                    BufferedImage image = reader.read();
                    image = Java2DPostProcessor.postProcess(
                            image, null, opList, imageInfo, null);
                    ImageWriterFacade.write(
                            image,
                            (Encode) opList.getFirst(Encode.class),
                            outputStream);
                    final int code = process.waitFor();
                    if (code != 0) {
                        LOGGER.error("{} returned with code {}",
                                FFMPEG_NAME, code);
                        final String errorStr = errorBucket.toString(StandardCharsets.UTF_8);
                        if (errorStr != null && errorStr.isBlank()) {
                            throw new ProcessorException(errorStr);
                        }
                    }
                } finally {
                    reader.dispose();
                }
            } finally {
                process.destroy();
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            final String errorStr = errorBucket.toString(StandardCharsets.UTF_8);
            if (errorStr != null && !errorStr.isBlank()) {
                msg += " (command output: " + msg + ")";
            }
            throw new ProcessorException(msg, e);
        }
    }

    /**
     * @return Command string corresponding to the given operation list.
     */
    private ProcessBuilder getProcessBuilder(OperationList opList) {
        final List<String> command = new ArrayList<>(20);
        command.add(getPath(FFMPEG_NAME));
        command.add("-i");
        command.add(sourceFile.toString());

        // Seeking to a particular time is supported via a "time" URL query
        // parameter which gets injected into an -ss flag. FFmpeg supports
        // additional syntax, but this will do for now.
        // https://trac.ffmpeg.org/wiki/Seeking
        String time = (String) opList.getOptions().get("time");
        if (time != null) { // we assume it's already been validated.
            command.add("-ss");
            command.add(time);
        }

        command.add("-nostdin");
        command.add("-v");
        command.add("quiet");
        command.add("-vframes");
        command.add("1");
        command.add("-an"); // disable audio
        command.add("-vcodec");
        command.add("bmp");
        command.add("-f"); // source or output format depending on position
        command.add("image2pipe");
        command.add("pipe:1");

        return new ProcessBuilder(command);
    }

    /**
     * Gets information about the video by invoking ffprobe and parsing its
     * output. The result is cached.
     */
    @Override
    public Info readInfo() throws IOException {
        if (info == null) {
            final List<String> command = new ArrayList<>();
            command.add(getPath(FFPROBE_NAME));
            command.add("-v");
            command.add("quiet");
            command.add("-select_streams");
            command.add("v:0");
            command.add("-show_entries");
            command.add("stream=width,height,duration");
            command.add("-of");
            command.add("default=noprint_wrappers=1:nokey=1");
            command.add(sourceFile.toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            LOGGER.trace("Invoking {}", StringUtils.join(pb.command(), " "));
            Process process = pb.start();

            try (InputStream processInputStream = process.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(
                                 processInputStream, StandardCharsets.UTF_8))) {
                String tmpWidth  = reader.readLine();
                String tmpHeight = reader.readLine();
                if (tmpWidth != null && tmpHeight != null) {
                    int width  = Integer.parseInt(tmpWidth);
                    int height = Integer.parseInt(tmpHeight);
                    try {
                        durationSec = Double.parseDouble(reader.readLine());
                    } catch (NumberFormatException e) {
                        LOGGER.info("readInfo(): {}", e.getMessage());
                    }
                    info = Info.builder()
                            .withSize(width, height)
                            .withTileSize(width, height)
                            .withFormat(getSourceFormat())
                            .build();
                    info.setNumResolutions(1);
                } else {
                    throw new SourceFormatException();
                }
            }
        }
        return info;
    }

    private void reset() {
        durationSec = 0;
        info = null;
    }

    @Override
    public void setSourceFile(Path sourceFile) {
        reset();
        this.sourceFile = sourceFile;
    }

    @Override
    public void setSourceFormat(Format format)
            throws SourceFormatException {
        super.setSourceFormat(format);
        reset();
    }

    @Override
    public void validate(OperationList opList, Dimension fullSize)
            throws ValidationException, ProcessorException {
        FileProcessor.super.validate(opList, fullSize);

        if (durationSec < 1) {
            try {
                readInfo();
            } catch (IOException e) {
                throw new ProcessorException(e.getMessage(), e);
            }
        }
        // Check that the "time" option, if supplied, is in the correct format.
        final String timeStr = (String) opList.getOptions().get("time");
        if (timeStr != null) {
            Matcher matcher = TIME_PATTERN.matcher(timeStr);
            if (matcher.matches()) {
                // Check that the supplied time is within the bounds of the
                // video's duration.
                final String[] parts = timeStr.split(":");
                final long seconds = (Integer.parseInt(parts[0]) * 60 * 60) +
                        (Integer.parseInt(parts[1]) * 60) +
                        Integer.parseInt(parts[2]);
                if (seconds > durationSec) {
                    throw new IllegalArgumentException(
                            "Time is beyond the length of the video.");
                }
            } else {
                throw new IllegalArgumentException("Invalid time format. " +
                        "(HH:MM::SS is required.)");
            }
        }
    }

}
