package edu.illinois.library.cantaloupe.image.exif;

/**
 * @see <a href="http://www.cipa.jp/std/documents/e/DC-008-Translation-2016-E.pdf">
 *     EXIF Version 2.31</a> sec. 4.6.4
 */
public enum Tag {

    EXIF_IFD_POINTER            (TagSet.BASELINE_TIFF, 34665, "EXIFIFD", true),
    GPS_IFD_POINTER             (TagSet.BASELINE_TIFF, 34853, "GPSIFD", true),
    INTEROPERABILITY_IFD_POINTER(TagSet.BASELINE_TIFF, 40965, "InteroperabilityIFD", true),

    IMAGE_WIDTH                   (TagSet.BASELINE_TIFF, 256, "ImageWidth", false),
    IMAGE_LENGTH                  (TagSet.BASELINE_TIFF, 257, "ImageLength", false),
    BITS_PER_SAMPLE               (TagSet.BASELINE_TIFF, 258, "BitsPerSample", false),
    COMPRESSION                   (TagSet.BASELINE_TIFF, 259, "Compression", false),
    PHOTOMETRIC_INTERPRETATION    (TagSet.BASELINE_TIFF, 262, "PhotometricInterpretation", false),
    ORIENTATION                   (TagSet.BASELINE_TIFF, 274, "Orientation", false),
    SAMPLES_PER_PIXEL             (TagSet.BASELINE_TIFF, 277, "SamplesPerPixel", false),
    PLANAR_CONFIGURATION          (TagSet.BASELINE_TIFF, 284, "PlanarConfiguration", false),
    Y_CB_CR_SUB_SAMPLING          (TagSet.BASELINE_TIFF, 530, "YCbCrSubSampling", false),
    Y_CB_CR_POSITIONING           (TagSet.BASELINE_TIFF, 531, "YCbCrPositioning", false),
    X_RESOLUTION                  (TagSet.BASELINE_TIFF, 282, "XResolution", false),
    Y_RESOLUTION                  (TagSet.BASELINE_TIFF, 283, "YResolution", false),
    RESOLUTION_UNIT               (TagSet.BASELINE_TIFF, 296, "ResolutionUnit", false),
    STRIP_OFFSETS                 (TagSet.BASELINE_TIFF, 273, "StripOffsets", false),
    ROWS_PER_STRIP                (TagSet.BASELINE_TIFF, 278, "RowsPerStrip", false),
    STRIP_BYTE_COUNTS             (TagSet.BASELINE_TIFF, 279, "StripByteCounts", false),
    JPEG_INTERCHANGE_FORMAT       (TagSet.BASELINE_TIFF, 513, "JPEGInterchangeFormat", false),
    JPEG_INTERCHANGE_FORMAT_LENGTH(TagSet.BASELINE_TIFF, 514, "JPEGInterchangeFormatLength", false),
    TRANSFER_FUNCTION             (TagSet.BASELINE_TIFF, 301, "TransferFunction", false),
    WHITE_POINT                   (TagSet.BASELINE_TIFF, 318, "WhitePoint", false),
    PRIMARY_CHROMATICITIES        (TagSet.BASELINE_TIFF, 319, "PrimaryChromaticities", false),
    Y_CB_CR_COEFFICIENTS          (TagSet.BASELINE_TIFF, 529, "YCbCrCoefficients", false),
    REFERENCE_BLACK_WHITE         (TagSet.BASELINE_TIFF, 532, "ReferenceBlackWhite", false),
    DATE_TIME                     (TagSet.BASELINE_TIFF, 306, "DateTime", false),
    IMAGE_DESCRIPTION             (TagSet.BASELINE_TIFF, 270, "ImageDescription", false),
    MAKE                          (TagSet.BASELINE_TIFF, 271, "Make", false),
    MODEL                         (TagSet.BASELINE_TIFF, 272, "Model", false),
    SOFTWARE                      (TagSet.BASELINE_TIFF, 305, "Software", false),
    ARTIST                        (TagSet.BASELINE_TIFF, 315, "Artist", false),
    COPYRIGHT                     (TagSet.BASELINE_TIFF, 33432, "Copyright", false),

    EXIF_VERSION               (TagSet.EXIF, 36864, "ExifVersion", false),
    FLASHPIX_VERSION           (TagSet.EXIF, 40960, "FlashpixVersion", false),
    COLOR_SPACE                (TagSet.EXIF, 40961, "ColorSpace", false),
    GAMMA                      (TagSet.EXIF, 42240, "Gamma", false),
    COMPONENTS_CONFIGURATION   (TagSet.EXIF, 37121, "ComponentsConfiguration", false),
    COMPRESSED_BITS_PER_PIXEL  (TagSet.EXIF, 37122, "CompressedBitsPerPixel", false),
    PIXEL_X_DIMENSION          (TagSet.EXIF, 40962, "PixelXDimension", false),
    PIXEL_Y_DIMENSION          (TagSet.EXIF, 40963, "PixelYDimension", false),
    MAKER_NOTE                 (TagSet.EXIF, 37500, "MakerNote", false),
    USER_COMMENT               (TagSet.EXIF, 37510, "UserComment", false),
    RELATED_SOUND_FILE         (TagSet.EXIF, 40964, "RelatedSoundFile", false),
    DATE_TIME_ORIGINAL         (TagSet.EXIF, 36867, "DateTimeOriginal", false),
    DATE_TIME_DIGITIZED        (TagSet.EXIF, 36868, "DateTimeDigitized", false),
    OFFSET_TIME                (TagSet.EXIF, 36880, "OffsetTime", false),
    OFFSET_TIME_ORIGINAL       (TagSet.EXIF, 36881, "OffsetTimeOriginal", false),
    OFFSET_TIME_DIGITIZED      (TagSet.EXIF, 36882, "OffsetTimeDigitized", false),
    SUB_SEC_TIME               (TagSet.EXIF, 37520, "SubSecTime", false),
    SUB_SEC_TIME_ORIGINAL      (TagSet.EXIF, 37521, "SubSecTimeOriginal", false),
    SUB_SEC_TIME_DIGITIZED     (TagSet.EXIF, 37522, "SubSecTimeDigitized", false),
    TEMPERATURE                (TagSet.EXIF, 37888, "Temperature", false),
    HUMIDITY                   (TagSet.EXIF, 37889, "Humidity", false),
    PRESSURE                   (TagSet.EXIF, 37890, "Pressure", false),
    WATER_DEPTH                (TagSet.EXIF, 37891, "WaterDepth", false),
    ACCELERATION               (TagSet.EXIF, 37892, "Acceleration", false),
    CAMERA_ELEVATION_ANGLE     (TagSet.EXIF, 37893, "CameraElevationAngle", false),
    IMAGE_UNIQUE_ID            (TagSet.EXIF, 42016, "ImageUniqueID", false),
    CAMERA_OWNER_NAME          (TagSet.EXIF, 42032, "CameraOwnerName", false),
    BODY_SERIAL_NUMBER         (TagSet.EXIF, 42033, "BodySerialNumber", false),
    LENS_SPECIFICATION         (TagSet.EXIF, 42034, "LensSpecification", false),
    LENS_MAKE                  (TagSet.EXIF, 42035, "LensMake", false),
    LENS_MODEL                 (TagSet.EXIF, 42036, "LensModel", false),
    EXPOSURE_TIME              (TagSet.EXIF, 33434, "ExposureTime", false),
    F_NUMBER                   (TagSet.EXIF, 33437, "FNumber", false),
    EXPOSURE_PROGRAM           (TagSet.EXIF, 34850, "ExposureProgram", false),
    SPECTRAL_SENSITIVITY       (TagSet.EXIF, 34852, "SpectralSensitivity", false),
    PHOTOGRAPHIC_SENSITIVITY   (TagSet.EXIF, 34855, "PhotographicSensitivity", false),
    OECF                       (TagSet.EXIF, 34856, "OECF", false),
    SENSITIVITY_TYPE           (TagSet.EXIF, 34864, "SensitivityType", false),
    STANDARD_OUTPUT_SENSITIVITY(TagSet.EXIF, 34865, "StandardOutputSensitivity", false),
    RECOMMENDED_EXPOSURE_INDEX (TagSet.EXIF, 34866, "RecommendedExposureIndex", false),
    ISO_SPEED                  (TagSet.EXIF, 34867, "ISOSpeed", false),
    ISO_SPEED_LATITUDE_YYY     (TagSet.EXIF, 34868, "ISOSpeedLatitudeyyy", false),
    ISO_SPEED_LATITUDE_ZZZ     (TagSet.EXIF, 34869, "ISOSpeedLatitudezzz", false),
    SHUTTER_SPEED              (TagSet.EXIF, 37377, "ShutterSpeedValue", false),
    APERTURE                   (TagSet.EXIF, 37378, "Aperture", false),
    BRIGHTNESS                 (TagSet.EXIF, 37379, "Brightness", false),
    EXPOSURE_BIAS              (TagSet.EXIF, 37380, "ExposureBias", false),
    MAX_APERTURE_VALUE         (TagSet.EXIF, 37381, "MaxApertureValue", false),
    SUBJECT_DISTANCE           (TagSet.EXIF, 37382, "SubjectDistance", false),
    METERING_MODE              (TagSet.EXIF, 37383, "MeteringMode", false),
    LIGHT_SOURCE               (TagSet.EXIF, 37384, "LightSource", false),
    FLASH                      (TagSet.EXIF, 37385, "Flash", false),
    FOCAL_LENGTH               (TagSet.EXIF, 37386, "FocalLength", false),
    SUBJECT_AREA               (TagSet.EXIF, 37396, "SubjectArea", false),
    FLASH_ENERGY               (TagSet.EXIF, 41483, "FlashEnergy", false),
    SPATIAL_FREQUENCY_RESPONSE (TagSet.EXIF, 41484, "SpatialFrequencyResponse", false),
    FOCAL_PLANE_X_RESOLUTION   (TagSet.EXIF, 41486, "FocalPlaneXResolution", false),
    FOCAL_PLANE_Y_RESOLUTION   (TagSet.EXIF, 41487, "FocalPlaneYResolution", false),
    FOCAL_PLANE_RESOLUTION_UNIT(TagSet.EXIF, 41488, "FocalPlaneResolutionUnit", false),
    SUBJECT_LOCATION           (TagSet.EXIF, 41492, "SubjectLocation", false),
    EXPOSURE_INDEX             (TagSet.EXIF, 41493, "ExposureIndex", false),
    SENSING_METHOD             (TagSet.EXIF, 41495, "SensingMethod", false),
    FILE_SOURCE                (TagSet.EXIF, 41728, "FileSource", false),
    SCENE_TYPE                 (TagSet.EXIF, 41729, "SceneType", false),
    CFA_PATTERN                (TagSet.EXIF, 41730, "CFAPattern", false),
    CUSTOM_RENDERED            (TagSet.EXIF, 41985, "CustomRendered", false),
    EXPOSURE_MODE              (TagSet.EXIF, 41986, "ExposureMode", false),
    WHITE_BALANCE              (TagSet.EXIF, 41987, "WhiteBalance", false),
    DIGITAL_ZOOM_RATIO         (TagSet.EXIF, 41988, "DigitalZoomRatio", false),
    FOCAL_LENGTH_IN_35MM_FILM  (TagSet.EXIF, 41989, "FocalLengthIn35mmFilm", false),
    SCENE_CAPTURE_TYPE         (TagSet.EXIF, 41990, "SceneCaptureType", false),
    GAIN_CONTROL               (TagSet.EXIF, 41991, "GainControl", false),
    CONTRAST                   (TagSet.EXIF, 41992, "Contrast", false),
    SATURATION                 (TagSet.EXIF, 41993, "Saturation", false),
    SHARPNESS                  (TagSet.EXIF, 41994, "Sharpness", false),
    DEVICE_SETTING_DESCRIPTION (TagSet.EXIF, 41995, "DeviceSettingDescription", false),
    SUBJECT_DISTANCE_RANGE     (TagSet.EXIF, 41996, "SubjectDistanceRange", false),

    GPS_VERSION_ID         (TagSet.GPS, 0, "GPSVersionID", false),
    GPS_LATITUDE_REF       (TagSet.GPS, 1, "GPSLatitudeRef", false),
    GPS_LATITUDE           (TagSet.GPS, 2, "GPSLatitude", false),
    GPS_LONGITUDE_REF      (TagSet.GPS, 3, "GPSLongitudeRef", false),
    GPS_LONGITUDE          (TagSet.GPS, 4, "GPSLongitude", false),
    GPS_ALTITUDE_REF       (TagSet.GPS, 5, "GPSAltitudeRef", false),
    GPS_ALTITUDE           (TagSet.GPS, 6, "GPSAltitude", false),
    GPS_TIME_STAMP         (TagSet.GPS, 7, "GPSTimeStamp", false),
    GPS_SATELLITES         (TagSet.GPS, 8, "GPSSatellites", false),
    GPS_STATUS             (TagSet.GPS, 9, "GPSStatus", false),
    GPS_MEASURE_MODE       (TagSet.GPS, 10, "GPSMeasureMode", false),
    GPS_DOP                (TagSet.GPS, 11, "GPSDOP", false),
    GPS_SPEED_REF          (TagSet.GPS, 12, "GPSSpeedRef", false),
    GPS_SPEED              (TagSet.GPS, 13, "GPSSpeed", false),
    GPS_TRACK_REF          (TagSet.GPS, 14, "GPSTrackRef", false),
    GPS_TRACK              (TagSet.GPS, 15, "GPSTrack", false),
    GPS_IMG_DIRECTION_REF  (TagSet.GPS, 16, "GPSImgDirectionRef", false),
    GPS_IMG_DIRECTION      (TagSet.GPS, 17, "GPSImgDirection", false),
    GPS_MAP_DATUM          (TagSet.GPS, 18, "GPSMapDatum", false),
    GPS_DEST_LATITUDE_REF  (TagSet.GPS, 19, "GPSDestLatitudeRef", false),
    GPS_DEST_LATITUDE      (TagSet.GPS, 20, "GPSDestLatitude", false),
    GPS_DEST_LONGITUDE_REF (TagSet.GPS, 21, "GPSDestLongitudeRef", false),
    GPS_DEST_LONGITUDE     (TagSet.GPS, 22, "GPSDestLongitude", false),
    GPS_DEST_BEARING_REF   (TagSet.GPS, 23, "GPSDestBearingRef", false),
    GPS_DEST_BEARING       (TagSet.GPS, 24, "GPSDestBearing", false),
    GPS_DEST_DISTANCE_REF  (TagSet.GPS, 25, "GPSDestDistanceRef", false),
    GPS_DEST_DISTANCE      (TagSet.GPS, 26, "GPSDestDistance", false),
    GPS_PROCESSING_METHOD  (TagSet.GPS, 27, "GPSProcessingMethod", false),
    GPS_AREA_INFORMATION   (TagSet.GPS, 28, "GPSAreaInformation", false),
    GPS_DATE_STAMP         (TagSet.GPS, 29, "GPSDateStamp", false),
    GPS_DIFFERENTIAL       (TagSet.GPS, 30, "GPSDifferential", false),
    GPS_H_POSITIONING_ERROR(TagSet.GPS, 31, "GPSHPositioningError", false),

    INTEROPERABILITY_INDEX(TagSet.INTEROPERABILITY, 1, "InteroperabilityIndex", false);

    private TagSet ifd;
    private int id;
    private String fieldName;
    private boolean isIFDPointer;

    Tag(TagSet ifd, int id, String fieldName, boolean isIFDPointer) {
        this.ifd = ifd;
        this.id = id;
        this.fieldName = fieldName;
        this.isIFDPointer = isIFDPointer;
    }

    public String getFieldName() {
        return fieldName;
    }

    public int getID() {
        return id;
    }

    public TagSet getTagSet() {
        return ifd;
    }

    /**
     * @return Whether the tag's field's value contains an offset to a sub-IFD.
     */
    public boolean isIFDPointer() {
        return isIFDPointer;
    }

    @Override
    public String toString() {
        return fieldName + " (" + id + ")";
    }

}
