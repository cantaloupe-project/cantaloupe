package edu.illinois.library.cantaloupe.config;

abstract class AbstractConfiguration {

    public void clearProperty(Key key) {
        clearProperty(key.key());
    }

    abstract public void clearProperty(String key);

    public boolean getBoolean(Key key) {
        return getBoolean(key.key());
    }

    abstract public boolean getBoolean(String key);

    public boolean getBoolean(Key key, boolean defaultValue) {
        return getBoolean(key.key(), defaultValue);
    }

    abstract public boolean getBoolean(String key, boolean defaultValue);

    public double getDouble(Key key) {
        return getDouble(key.key());
    }

    abstract public double getDouble(String key);

    public double getDouble(Key key, double defaultValue) {
        return getDouble(key.key(), defaultValue);
    }

    abstract public double getDouble(String key, double defaultValue);

    public int getInt(Key key) {
        return getInt(key.key());
    }

    abstract public int getInt(String key);

    public int getInt(Key key, int defaultValue) {
        return getInt(key.key(), defaultValue);
    }

    abstract public int getInt(String key, int defaultValue);

    public float getFloat(Key key) {
        return getFloat(key.key());
    }

    abstract public float getFloat(String key);

    public float getFloat(Key key, float defaultValue) {
        return getFloat(key.key(), defaultValue);
    }

    abstract public float getFloat(String key, float defaultValue);

    public long getLong(Key key) {
        return getLong(key.key());
    }

    abstract public long getLong(String key);

    public long getLong(Key key, long defaultValue) {
        return getLong(key.key(), defaultValue);
    }

    abstract public long getLong(String key, long defaultValue);

    public Object getProperty(Key key) {
        return getProperty(key.key());
    }

    abstract public Object getProperty(String key);

    public String getString(Key key) {
        return getString(key.key());
    }

    abstract public String getString(String key);

    public String getString(Key key, String defaultValue) {
        return getString(key.key(), defaultValue);
    }

    abstract public String getString(String key, String defaultValue);

    public void setProperty(Key key, Object value) {
        setProperty(key.key(), value);
    }

    abstract public void setProperty(String key, Object value);

}
