var Configuration = function(data) {

    /**
     * @param key
     * @returns {*}
     */
    this.get = function(key) {
        return data[key];
    };

    /**
     * @returns {*}
     */
    this.keys = function() {
        return Object.keys(data);
    }

    /**
     * @param key {String}
     * @param value {*}
     */
    this.set = function(key, value) {
        data[key] = value;
    };

    /**
     * @returns {*}
     */
    this.toString = function() {
        return JSON.stringify(data);
    };

};

/**
 * @param config {Configuration}
 * @constructor
 */
var Form = function(config) {

    /**
     * Updates the form state to correspond to that of the Configuration
     * instance.
     */
    this.load = function() {
        config.keys().forEach(function(key) {
            var configValue = config.get(key);
            var elements = $('[name="' + key + '"]');

            if (elements.is(":checkbox")) {
                elements.each(function() {
                    $(this).prop('checked', (configValue == 'true'));
                });
            } else {
                elements.val(configValue);
            }

            if (elements.type == 'text') {
                elements.val(configValue);
            } else if (elements.type == 'checkbox') {
                elements.forEach(function(e) {
                    $(this).prop('checked', ($(this).val() == configValue));
                });
            }
        });
    };

    /**
     * Updates the Configuration instance to correspond with the form state.
     */
    this.save = function() {
        config.keys().forEach(function(key) {
            config.set(key, $('[name="' + key + '"]').val());
        });

        console.log(config.toString());
        /*
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: window.location,
            data: config.toString(),
            success: function(data) {
                alert('Configuration updated.');
            }
        });
        */
    };

    /**
     * Checks the validity of the form and updates it with error messages if
     * invalid.
     *
     * @returns Boolean
     */
    this.validate = function() {
        // TODO: write this
        return true;
    };

};

config = null;
form = null;

$(document).ready(function() {
    // Download configuration data into a Configuration instance.
    $.ajax({
        dataType: 'json',
        url: window.location,
        data: null,
        success: function(data) {
            config = new Configuration(data);
            form = new Form(config);
            form.load();
        },
        error: function() {
            alert('Error');
        }
    });

    $(document).on('submit', 'form', function() {
        if (form.validate()) {
            form.save();
        }
        return false;
    });
});
