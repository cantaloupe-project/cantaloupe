package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.request.Format;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import edu.illinois.library.cantaloupe.request.Rotation;
import edu.illinois.library.cantaloupe.request.Size;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.process.Pipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageMagickProcessor implements Processor {

    public ImageInfo getImageInfo(String identifier, String imageBaseUri) {
        String filePath = this.getResolvedPathname(identifier);
        ImageInfo imageInfo = new ImageInfo();
        try {
            Info sourceInfo = new Info(filePath, true);
            imageInfo.setId(imageBaseUri);
            imageInfo.setHeight(sourceInfo.getImageHeight());
            imageInfo.setWidth(sourceInfo.getImageWidth());

            Map<String,List<String>> profile = new HashMap<String, List<String>>();
            imageInfo.getProfile().add(profile);

            List<String> formats = new ArrayList<String>();
            for (Format format : Format.values()) {
                formats.add(format.getExtension());
            }
            profile.put("formats", formats);

            List<String> qualities = new ArrayList<String>();
            for (Quality quality : Quality.values()) {
                qualities.add(quality.toString().toLowerCase());
            }
            profile.put("qualities", qualities);

            List<String> supports = new ArrayList<String>();
            // TODO: is this list accurate?
            supports.add("mirroring");
            supports.add("regionByPx");
            supports.add("rotationArbitrary");
            supports.add("sizeByWhListed");
            supports.add("sizeByForcedWh");
            supports.add("sizeByH");
            supports.add("sizeByPct");
            supports.add("sizeByW");
            supports.add("sizeWh");
            profile.put("supports", supports);

            return imageInfo;
        } catch (InfoException e) {
            return imageInfo;
        }
    }

    public void process(Parameters params, OutputStream outputStream)
            throws Exception {
        String filePath = this.getResolvedPathname(params.getIdentifier());

        IMOperation op = new IMOperation();
        op.addImage("-"); // read from stdin

        // region transformation
        Region region = params.getRegion();
        if (!region.isFull()) {
            if (region.isPercent()) {
                op.crop(region.getWidth(), region.getHeight(),
                        Math.round(region.getX()), Math.round(region.getY()),
                        "%".charAt(0)); // TODO: this doesn't work
            } else {
                op.crop(region.getWidth(), region.getHeight(),
                        Math.round(region.getX()), Math.round(region.getY()));
            }
        }

        // size transformation
        Size size = params.getSize();
        if (size.getScaleMode() != Size.ScaleMode.FULL) {
            if (size.getScaleMode() == Size.ScaleMode.FILL_WIDTH) {
                op.resize(size.getWidth());
            } else if (size.getScaleMode() == Size.ScaleMode.FILL_HEIGHT) {
                op.resize(null, size.getHeight());
            } else if (size.getScaleMode() == Size.ScaleMode.NON_ASPECT_FIT_INSIDE) {
                op.resize(size.getWidth(), size.getHeight(), "!".charAt(0));
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_INSIDE) {
                op.resize(size.getWidth(), size.getHeight());
            } else if (size.getPercent() != null) {
                op.resize(Math.round(size.getPercent()),
                        Math.round(size.getPercent()),
                        "%".charAt(0));
            }
        }

        // rotation transformation
        Rotation rotation = params.getRotation();
        if (rotation.getDegrees() != 0) {
            if (rotation.shouldMirror()) {
                op.flop();
            }
            op.rotate(rotation.getDegrees().doubleValue());
        }

        // quality transformation
        Quality quality = params.getQuality();
        if (quality != Quality.COLOR && quality != Quality.DEFAULT) {
            switch (quality) {
                case GRAY:
                    op.colorspace("Gray");
                    break;
                case BITONAL:
                    op.monochrome();
                    break;
            }
        }

        // format transformation
        op.addImage(params.getFormat().getExtension() + ":-"); // write to stdout

        FileInputStream fis = new FileInputStream(filePath);
        Pipe pipeIn = new Pipe(fis, null);
        Pipe pipeOut = new Pipe(null, outputStream);

        ConvertCmd convert = new ConvertCmd();
        convert.setInputProvider(pipeIn);
        convert.setOutputConsumer(pipeOut);
        convert.run(op);
        fis.close();
    }

    public boolean resourceExists(String identifier) {
        String filePath = this.getResolvedPathname(identifier);
        File file = new File(filePath);
        return file.exists() && !file.isDirectory();
    }

    /**
     * @param identifier Identifier component of an IIIF 2.0 URL
     * @return Full filesystem path of the file corresponding to the given
     * identifier
     */
    private String getResolvedPathname(String identifier) {
        Resolver resolver = ResolverFactory.getResolver();
        return resolver.resolve(identifier);
    }

}
