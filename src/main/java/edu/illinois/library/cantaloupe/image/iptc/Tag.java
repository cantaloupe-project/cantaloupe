package edu.illinois.library.cantaloupe.image.iptc;

/**
 * Tag values recognized by {@link Reader}. This is not a comprehensive list of
 * tags, but includes the ones that are most commonly used in embedded
 * metadata.
 *
 * @see <a href="https://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/IPTC.html">
 *     IPTC Tags</a>
 */
public enum Tag {

    // envelope record
    ENVELOPE_RECORD_VERSION(Record.ENVELOPE, 0, "EnvelopeRecordVersion", DataType.UNSIGNED_INT_16),
    DESTINATION            (Record.ENVELOPE, 5, "Destination", DataType.STRING),
    FILE_FORMAT            (Record.ENVELOPE, 20, "FileFormat", DataType.UNSIGNED_INT_16),
    FILE_VERSION           (Record.ENVELOPE, 22, "FileVersion", DataType.UNSIGNED_INT_16),
    SERVICE_IDENTIFIER     (Record.ENVELOPE, 30, "ServiceIdentifier", DataType.STRING),
    ENVELOPE_NUMBER        (Record.ENVELOPE, 40, "EnvelopeNumber", DataType.DIGITS),
    PRODUCT_ID             (Record.ENVELOPE, 50, "ProductID", DataType.STRING),
    ENVELOPE_PRIORITY      (Record.ENVELOPE, 60, "EnvelopePriority", DataType.DIGITS),
    DATE_SENT              (Record.ENVELOPE, 70, "DateSent", DataType.DIGITS),
    TIME_SENT              (Record.ENVELOPE, 80, "TimeSent", DataType.STRING),
    CODED_CHARACTER_SET    (Record.ENVELOPE, 90, "CodedCharacterSet", DataType.STRING),
    UNIQUE_OBJECT_NAME     (Record.ENVELOPE, 100, "UniqueObjectName", DataType.STRING),
    ARM_IDENTIFIER         (Record.ENVELOPE, 120, "ARMIdentifier", DataType.UNSIGNED_INT_16),
    ARM_VERSION            (Record.ENVELOPE, 122, "ARMVersion", DataType.UNSIGNED_INT_16),

    // application record
    APPLICATION_RECORD_VERSION     (Record.APPLICATION, 0, "ApplicationRecordVersion", DataType.UNSIGNED_INT_16),
    OBJECT_TYPE_REFERENCE          (Record.APPLICATION, 3, "ObjectTypeReference", DataType.STRING),
    OBJECT_ATTRIBUTE_REFERENCE     (Record.APPLICATION, 4, "ObjectAttributeReference", DataType.STRING),
    OBJECT_NAME                    (Record.APPLICATION, 5, "ObjectName", DataType.STRING),
    EDIT_STATUS                    (Record.APPLICATION, 7, "EditStatus", DataType.STRING),
    EDITORIAL_UPDATE               (Record.APPLICATION, 8, "EditorialUpdate", DataType.DIGITS),
    URGENCY                        (Record.APPLICATION, 10, "Urgency", DataType.DIGITS),
    SUBJECT_REFERENCE              (Record.APPLICATION, 12, "SubjectReference", DataType.STRING),
    CATEGORY                       (Record.APPLICATION, 15, "Category", DataType.STRING),
    SUPPLEMENTAL_CATEGORIES        (Record.APPLICATION, 20, "SupplementalCategories", DataType.STRING),
    FIXTURE_IDENTIFIER             (Record.APPLICATION, 22, "FixtureIdentifier", DataType.STRING),
    KEYWORDS                       (Record.APPLICATION, 25, "Keywords", DataType.STRING),
    CONTENT_LOCATION_CODE          (Record.APPLICATION, 26, "ContentLocationCode", DataType.STRING),
    CONTENT_LOCATION_NAME          (Record.APPLICATION, 27, "ContentLocationName", DataType.STRING),
    RELEASE_DATE                   (Record.APPLICATION, 30, "ReleaseDate", DataType.DIGITS),
    RELEASE_TIME                   (Record.APPLICATION, 35, "ReleaseTime", DataType.STRING),
    EXPIRATION_DATE                (Record.APPLICATION, 37, "ExpirationDate", DataType.DIGITS),
    EXPIRATION_TIME                (Record.APPLICATION, 38, "ExpirationTime", DataType.STRING),
    SPECIAL_INSTRUCTIONS           (Record.APPLICATION, 40, "SpecialInstructions", DataType.STRING),
    ACTION_ADVISED                 (Record.APPLICATION, 42, "ActionAdvised", DataType.DIGITS),
    REFERENCE_SERVICE              (Record.APPLICATION, 45, "ReferenceService", DataType.STRING),
    REFERENCE_DATE                 (Record.APPLICATION, 47, "ReferenceDate", DataType.DIGITS),
    REFERENCE_NUMBER               (Record.APPLICATION, 50, "ReferenceNumber", DataType.DIGITS),
    DATE_CREATED                   (Record.APPLICATION, 55, "DateCreated", DataType.DIGITS),
    TIME_CREATED                   (Record.APPLICATION, 60, "TimeCreated", DataType.STRING),
    DIGITAL_CREATION_DATE          (Record.APPLICATION, 62, "DigitalCreationDate", DataType.DIGITS),
    DIGITAL_CREATION_TIME          (Record.APPLICATION, 63, "DigitalCreationTime", DataType.STRING),
    ORIGINATING_PROGRAM            (Record.APPLICATION, 65, "OriginatingProgram", DataType.STRING),
    PROGRAM_VERSION                (Record.APPLICATION, 70, "ProgramVersion", DataType.STRING),
    OBJECT_CYCLE                   (Record.APPLICATION, 75, "ObjectCycle", DataType.STRING),
    BYLINE                         (Record.APPLICATION, 80, "ByLine", DataType.STRING),
    BYLINE_TITLE                   (Record.APPLICATION, 85, "ByLineTitle", DataType.STRING),
    CITY                           (Record.APPLICATION, 90, "City", DataType.STRING),
    SUBLOCATION                    (Record.APPLICATION, 92, "SubLocation", DataType.STRING),
    PROVINCE_STATE                 (Record.APPLICATION, 95, "Province/State", DataType.STRING),
    COUNTRY_PRIMARY_LOCATION_CODE  (Record.APPLICATION, 100, "Country/PrimaryLocationCode", DataType.STRING),
    COUNTRY_PRIMARY_LOCATION_NAME  (Record.APPLICATION, 101, "Country/PrimaryLocationName", DataType.STRING),
    ORIGINAL_TRANSMISSION_REFERENCE(Record.APPLICATION, 103, "OriginalTransmissionReference", DataType.STRING),
    HEADLINE                       (Record.APPLICATION, 105, "Headline", DataType.STRING),
    CREDIT                         (Record.APPLICATION, 110, "Credit", DataType.STRING),
    SOURCE                         (Record.APPLICATION, 115, "Source", DataType.STRING),
    COPYRIGHT_NOTICE               (Record.APPLICATION, 116, "CopyrightNotice", DataType.STRING),
    CONTACT                        (Record.APPLICATION, 118, "Contact", DataType.STRING),
    CAPTION_ABSTRACT               (Record.APPLICATION, 120, "Caption/Abstract", DataType.STRING),
    WRITER_EDITOR                  (Record.APPLICATION, 122, "Writer/Editor", DataType.STRING),
    IMAGE_TYPE                     (Record.APPLICATION, 130, "ImageType", DataType.STRING),
    IMAGE_ORIENTATION              (Record.APPLICATION, 131, "ImageOrientation", DataType.STRING),
    LANGUAGE_IDENTIFIER            (Record.APPLICATION, 135, "LanguageIdentifier", DataType.STRING),
    AUDIO_TYPE                     (Record.APPLICATION, 150, "AudioType", DataType.STRING),
    AUDIO_SAMPLING_RATE            (Record.APPLICATION, 151, "AudioSamplingRate", DataType.DIGITS),
    AUDIO_SAMPLING_RESOLUTION      (Record.APPLICATION, 152, "AudioSamplingResolution", DataType.DIGITS),
    AUDIO_DURATION                 (Record.APPLICATION, 153, "AudioDuration", DataType.DIGITS),
    AUDIO_OUTCUE                   (Record.APPLICATION, 154, "AudioOutcue", DataType.STRING),
    JOB_ID                         (Record.APPLICATION, 184, "JobID", DataType.STRING),
    MASTER_DOCUMENT_ID             (Record.APPLICATION, 185, "MasterDocumentID", DataType.STRING),
    SHORT_DOCUMENT_ID              (Record.APPLICATION, 186, "ShortDocumentID", DataType.STRING),
    UNIQUE_DOCUMENT_ID             (Record.APPLICATION, 187, "UniqueDocumentID", DataType.STRING),
    OWNER_ID                       (Record.APPLICATION, 188, "OwnerID", DataType.STRING),
    OBJECT_PREVIEW_FILE_FORMAT     (Record.APPLICATION, 200, "ObjectPreviewFileFormat", DataType.UNSIGNED_INT_16),
    OBJECT_PREVIEW_FILE_VERSION    (Record.APPLICATION, 201, "ObjectPreviewFileFormatVer", DataType.UNSIGNED_INT_16),
    PREFS                          (Record.APPLICATION, 221, "Prefs", DataType.STRING),
    CLASSIFY_STATE                 (Record.APPLICATION, 225, "ClassifyState", DataType.STRING),
    SIMILARITY_INDEX               (Record.APPLICATION, 228, "SimilarityIndex", DataType.STRING),
    DOCUMENT_NOTES                 (Record.APPLICATION, 230, "DocumentNotes", DataType.STRING),
    DOCUMENT_HISTORY               (Record.APPLICATION, 231, "DocumentHistory", DataType.STRING),
    EXIF_CAMERA_INFO               (Record.APPLICATION, 232, "ExifCameraInfo", DataType.STRING),
    CATALOG_SETS                   (Record.APPLICATION, 255, "CatalogSets", DataType.STRING);

    private Record record;
    private int code;
    private String name;
    private DataType dataType;

    Tag(Record record, int code, String name, DataType dataType) {
        this.record   = record;
        this.code     = code;
        this.name     = name;
        this.dataType = dataType;
    }

    public Record getRecord() {
        return record;
    }

    public int getDataSetNum() {
        return code;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

}
