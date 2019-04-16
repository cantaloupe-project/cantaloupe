package edu.illinois.library.cantaloupe.image.iptc;

import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>IPTC IIM reader.</p>
 *
 * <p>The default character set for data sets is ASCII. This can be overridden
 * as UTF-8 or ISO-8859-1 depending on the data set corresponding to the
 * {@literal CodedCharacterSet} tag. Many other encodings are supported by IIM
 * but not by this reader.</p>
 *
 * @author Alex Dolski UIUC
 * @see <a href="https://www.iptc.org/std/IIM/4.1/specification/IIMV4.1.pdf">
 *     IPTC-NAA Information Interchange Model Version 4</a>
 */
public final class Reader implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Reader.class);

    private static final int INITIAL_DATASET_LIST_SIZE = 50;
    private static final byte[] ISO_8859_1_CCS = new byte[] { 0x1b, 0x25, 0x47 };
    private static final byte[] UTF_8_CCS      = new byte[] { 0x1b, 0x2e, 0x41 };

    private ImageInputStream inputStream;

    private List<DataSet> dataSets = new ArrayList<>(INITIAL_DATASET_LIST_SIZE);

    @Override
    public void close() throws IOException {
        dataSets = new ArrayList<>(INITIAL_DATASET_LIST_SIZE);
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
    }

    public void setSource(byte[] iptcBytes) {
        setSource(new MemoryCacheImageInputStream(
                new ByteArrayInputStream(iptcBytes)));
    }

    public void setSource(ImageInputStream inputStream) {
        if (this.inputStream != null) {
            throw new IllegalStateException("Source has already been set. " +
                    "close() must be called before setting it again.");
        }
        this.inputStream = inputStream;
    }

    public List<DataSet> read() throws IOException {
        final Stopwatch watch = new Stopwatch();

        readNextDataSet();
        updateDataSetEncodings();

        LOGGER.trace("read(): read in {}", watch);
        return dataSets;
    }

    private void readNextDataSet() throws IOException {
        if (inputStream.read() != 0x1c) {
            return;
        }
        final int recordNum = inputStream.read();
        final int dataSetNum = inputStream.read();
        final Tag tag = Arrays
                .stream(Tag.values())
                .filter(t -> t.getRecord().getRecordNum() == recordNum &&
                        t.getDataSetNum() == dataSetNum)
                .findFirst()
                .orElse(null);
        if (tag != null) {
            int b4 = inputStream.read();
            int b5 = inputStream.read();
            int dataLength = ((b4 & 0xff) << 8) | (b5 & 0xff);
            // if it's an extended tag (sec. 4.5.3; highly unlikely since
            // these shouldn't fit into the segment)
            if (b4 >> 7 == 1) {
                int numLengthOctets = ((b4 & 0xff) << 8) | (b5 & 0xff);
                byte[] lengthOctets = new byte[numLengthOctets];
                inputStream.readFully(lengthOctets);
                ByteBuffer buf = ByteBuffer.wrap(lengthOctets);
                dataLength = buf.getInt();
            }
            byte[] data = new byte[dataLength];
            inputStream.readFully(data);
            dataSets.add(new DataSet(tag, data));
        }
        readNextDataSet();
    }

    /**
     * Searches for a data set with a {@link Tag#CODED_CHARACTER_SET
     * CodedCharacterSet tag}, and updates all of the data sets with a
     * corresponding {@link Charset}.
     */
    private void updateDataSetEncodings() {
        dataSets.stream()
                .filter(ds -> Tag.CODED_CHARACTER_SET.equals(ds.getTag()))
                .findFirst()
                .ifPresent(ds -> {
                    // This is an ISO 2022 value.
                    final byte[] ccsBytes = ds.getDataField();
                    Charset charset = null;
                    if (Arrays.equals(UTF_8_CCS, ccsBytes)) {
                        charset = StandardCharsets.UTF_8;
                    } else if (Arrays.equals(ISO_8859_1_CCS, ccsBytes)) {
                        charset = StandardCharsets.ISO_8859_1;
                    }
                    if (charset != null) {
                        final Charset c = charset;
                        dataSets.forEach(s -> s.setStringEncoding(c));
                    }
                });
    }

}
