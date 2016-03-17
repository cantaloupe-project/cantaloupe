/**
 * Wraps an application configuration object.
 *
 * @param data {Object} of key-value configuration key-value pairs
 * @constructor
 */
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
    var self = this;

    var attachEventListeners = function() {

        /**
         * Shows or hides all the other rows in the same table as a checkbox
         * element, depending on whether it is checked or not.
         *
         * @param checkbox_selector jQuery selector for one or multiple
         *                          checkboxes
         */
        var showOrHideAllOtherTableRows = function(checkbox_selector) {
            $(checkbox_selector).on('change', function() {
                var other_rows = $(this).parents('tr').siblings('tr');
                if ($(this).prop('checked')) {
                    other_rows.show();
                } else {
                    other_rows.hide();
                }
            }).trigger('change');
        };

        ///////////////////// global form listeners /////////////////////////

        // When a form element that represents a property that requires
        // a restart is changed, save that fact.
        $('[data-requires-restart="true"], [data-requires-restart="1"]').
        on('change', function() {
            self.setRestartRequired(true);
        });

        // Override the form submit behavior to invoke custom validation and
        // save functions.
        $('input[type="submit"]').on('click', function() {
            if (self.validate()) {
                self.save($(this).parents('form'));
            }
            return false;
        });

        ////////////////////// individual sections //////////////////////////

        // Server
        showOrHideAllOtherTableRows(
            '[name="http.enabled"], [name="https.enabled"], [name="auth.basic.enabled"]');
        // Endpoints
        showOrHideAllOtherTableRows(
            '[name="endpoint.iiif.1.enabled"], [name="endpoint.iiif.2.enabled"]');
        // Caches
        showOrHideAllOtherTableRows(
            '[name="cache.client.enabled"], [name="cache.worker.enabled"]');
        // Overlays
        showOrHideAllOtherTableRows(
            '[name="watermark.enabled"], [name="redaction.enabled"]');
        // Delegate Script
        showOrHideAllOtherTableRows('[name="delegate_script.enabled"]');
        // Logging
        showOrHideAllOtherTableRows(
            '[name="log.application.ConsoleAppender.enabled"], ' +
            '[name="log.application.FileAppender.enabled"], ' +
            '[name="log.application.RollingFileAppender.enabled"], ' +
            '[name="log.application.SyslogAppender.enabled"], ' +
            '[name="log.access.ConsoleAppender.enabled"], ' +
            '[name="log.access.FileAppender.enabled"], ' +
            '[name="log.access.RollingFileAppender.enabled"], ' +
            '[name="log.access.SyslogAppender.enabled"]');
    };

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

        attachEventListeners();
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

$(document).ready(function() {
    $('.cl-help').popover({
        placement: 'auto',
        html: true
    });

    // Download configuration data into a Configuration instance, and
    // initialize a Form instance on success.
    $.ajax({
        dataType: 'json',
        url: window.location,
        data: null,
        success: function(data) {
            new Form(new Configuration(data)).load();
        },
        error: function(xhr, status, error) {
            console.error(xhr);
            console.error(error);
            alert('Failed to load the configuration: ' + error);
        }
    });
});
