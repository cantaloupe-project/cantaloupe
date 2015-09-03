package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Parameters;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.process.Pipe;

import java.io.FileInputStream;
import java.io.OutputStream;

public class ImageMagickProcessor implements Processor {

    public void process(Parameters params, OutputStream outputStream)
            throws Exception {
        IMOperation op = new IMOperation();
        op.addImage("/Users/alexd/Pictures/Cars/f50.jpg");
        op.addImage(params.getFormat().getExtension() + ":-"); // write to stdout

        FileInputStream fis = new FileInputStream("/Users/alexd/Pictures/Cars/f50.jpg");
        Pipe pipeIn = new Pipe(fis, null);
        Pipe pipeOut = new Pipe(null, outputStream);

        ConvertCmd convert = new ConvertCmd();
        convert.setInputProvider(pipeIn);
        convert.setOutputConsumer(pipeOut);
        convert.run(op);
        fis.close();
    }

}
