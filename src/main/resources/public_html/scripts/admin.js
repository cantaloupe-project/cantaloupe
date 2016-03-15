var Configuration = function(data) {

    /**
     * @returns {*}
     */
    this.data = function() {
        return data;
    };

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
    };

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

    var restart_required = false;

    /**
     * Updates the form state to correspond to that of the Configuration
     * instance.
     */
    this.load = function() {
        console.info('Loading configuration');
        console.debug(config.data());

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

    this.setRestartRequired = function(bool) {
        restart_required = bool;
    };

    /**
     * Updates the Configuration instance to correspond with the form state.
     *
     * @param formElem Form element
     */
    this.save = function(formElem) {
        // Copy the values of non-checkbox form controls into the
        // configuration object.
        $(formElem).find('input, textarea, select').
        not('[type=submit], [type=checkbox]').each(function() {
            config.set($(this).attr('name'), $(this).val());
        });
        // Copy the checkbox values. This works only with one checkbox per
        // input name.
        $(formElem).find('input[type=checkbox]').each(function() {
            config.set($(this).attr('name'), $(this).is(':checked'));
        });

        console.info('Saving configuration');
        console.debug(config.data());

        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: window.location,
            data: config.toString(),
            success: function() {
                // Disable the submit button
                var submit = $(formElem).find('input[type="submit"]');
                submit.prop('disabled', true);

                // Set the success message, make it appear, and fade it out on
                // a delay.
                var msg = '&check; Configuration saved.';
                if (restart_required) {
                    msg += ' Restart is required.';
                }
                $(formElem).find('.cl-on-save-message').html(msg).show().
                delay(4000).fadeOut(800, function() {
                    submit.prop('disabled', false);
                    restart_required = false;
                });
            },
            error: function(xhr, status, error) {
                console.error(xhr);
                console.error(error);
                alert('Failed to save the configuration: ' + error);
            }
        });
    };

    /**
     * Checks the validity of the form and updates it with error messages if
     * invalid.
     *
     * @returns {Boolean}
     */
    this.validate = function() {
        // TODO: write this
        return true;
    };

};

config = null;
form = null;

$(document).ready(function() {
    $('.cl-help').popover({
        placement: 'auto',
        html: true
    });

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
            alert('Failed to load the configuration.');
        }
    });

    $('[data-requires-restart="true"], [data-requires-restart="1"]').
    on('change', function() {
        form.setRestartRequired(true);
    });

    $('input[type="submit"]').on('click', function() {
        if (form.validate()) {
            form.save($(this).parents('form'));
        }
        return false;
    });
});
