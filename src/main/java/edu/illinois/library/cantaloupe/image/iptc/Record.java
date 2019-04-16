package edu.illinois.library.cantaloupe.image.iptc;

public enum Record {

    ENVELOPE(1),
    APPLICATION(2);

    private int recordNum;

    Record(int recordNum) {
        this.recordNum = recordNum;
    }

    public int getRecordNum() {
        return recordNum;
    }

}
