package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Parameters;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.process.Pipe;

import java.io.FileInputStream;
import java.io.OutputStream;

public class ImageMagickProcessor implements Processor {

    public void process(Parameters params, OutputStream outputStream)
            throws Exception {
        String filePath = this.getResolvedPathname(params.getIdentifier());

        IMOperation op = new IMOperation();
        op.addImage(filePath);
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

    /**
     * @param identifier Identifier component of an IIIF 2.0 URL
     * @return Full filesystem path of the file corresponding to the given
     * identifier
     * @throws Exception
     */
    private String getResolvedPathname(String identifier) throws Exception {
        Resolver resolver = ResolverFactory.getResolver();
        return resolver.resolve(identifier);
    }

}
