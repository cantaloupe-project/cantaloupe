package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resolver.InputStreamStreamSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processor that uses the ffmpeg command-line tool to extract video frames,
 * and the ffprobe tool to get video information. Works with ffmpeg 2.8 (other
 * versions untested).
 */
class FfmpegProcessor extends AbstractJava2DProcessor implements FileProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(FfmpegProcessor.class);

    private static final String PATH_TO_BINARIES_CONFIG_KEY =
            "FfmpegProcessor.path_to_binaries";

    private static final Pattern timePattern =
            Pattern.compile("[0-9][0-9]:[0-5][0-9]:[0-5][0-9]");

    private double durationSec = 0;
    private Info imageInfo;
    private File sourceFile;

    /**
     * @param binaryName Name of one of the ffmpeg binaries
     * @return
     */
    private static String getPath(String binaryName) {
        String path = Configuration.getInstance().
                getString(PATH_TO_BINARIES_CONFIG_KEY);
        if (path != null && path.length() > 0) {
            path = StringUtils.stripEnd(path, File.separator) + File.separator +
                    binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        final Set<Format> outputFormats = new HashSet<>();
        if (format.isVideo()) {
            outputFormats.addAll(ImageWriter.supportedFormats());
        }
        return outputFormats;
    }

    /**
     * Gets information about the video by invoking ffprobe and parsing its
     * output. The result is cached.
     */
    @Override
    public Info readImageInfo() throws ProcessorException {
        if (imageInfo == null) {
            final List<String> command = new ArrayList<>();
            command.add(getPath("ffprobe"));
            command.add("-v");
            command.add("quiet");
            command.add("-select_streams");
            command.add("v:0");
            command.add("-show_entries");
            command.add("stream=width,height,duration");
            command.add("-of");
            command.add("default=noprint_wrappers=1:nokey=1");
            command.add(sourceFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            try {
                logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
                Process process = pb.start();
                InputStream processInputStream = process.getInputStream();
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(processInputStream))) {
                    int width = Integer.parseInt(reader.readLine());
                    int height = Integer.parseInt(reader.readLine());
                    try {
                        durationSec = Double.parseDouble(reader.readLine());
                    } catch (NumberFormatException e) {
                        logger.debug("readImageInfo(): {}", e.getMessage());
                    }
                    imageInfo = new Info(width, height, width, height,
                            getSourceFormat());
                }
            } catch (IOException e) {
                throw new ProcessorException(e.getMessage(), e);
            }
        }
        return imageInfo;
    }

    @Override
    public File getSourceFile() {
        return this.sourceFile;
    }

    @Override
    public void process(final OperationList opList,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ProcessBuilder pb = getProcessBuilder(opList);
            logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            try (final InputStream processInputStream = process.getInputStream();
                 final InputStream processErrorStream = process.getErrorStream()) {
                ThreadPool.getInstance().submit(
                        new StreamCopier(processErrorStream, errorBucket));

                final ImageReader reader = new ImageReader(
                        new InputStreamStreamSource(processInputStream),
                        Format.BMP);
                final BufferedImage image = reader.read();
                try {
                    postProcess(image, null, opList, imageInfo, null,
                            false, outputStream);
                    final int code = process.waitFor();
                    if (code != 0) {
                        logger.error("ffmpeg returned with code {}", code);
                        final String errorStr = errorBucket.toString();
                        if (errorStr != null && errorStr.length() > 0) {
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
            final String errorStr = errorBucket.toString();
            if (errorStr != null && errorStr.length() > 0) {
                msg += " (command output: " + msg + ")";
            }
            throw new ProcessorException(msg, e);
        }
    }

    @Override
    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * @param opList
     * @return Command string
     * @throws IllegalArgumentException
     */
    private ProcessBuilder getProcessBuilder(OperationList opList) {
        final List<String> command = new ArrayList<>();
        command.add(getPath("ffmpeg"));
        command.add("-i");
        command.add(sourceFile.getAbsolutePath());

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

    @Override
    public void validate(OperationList opList, Dimension fullSize)
            throws ValidationException, ProcessorException {
        FileProcessor.super.validate(opList, fullSize);

        if (durationSec < 1) {
            readImageInfo();
        }
        // Check that the "time" option, if supplied, is in the correct format.
        final String timeStr = (String) opList.getOptions().get("time");
        if (timeStr != null) {
            Matcher matcher = timePattern.matcher(timeStr);
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
