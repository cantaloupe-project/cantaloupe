function CustomDelegate() {

    const self = this;

    this.context = null;

    this.authorize = function (options = {}) {
        switch (self.context.identifier) {
            case "forbidden-boolean.jpg":
                return false;
            case "redirect.jpg":
                return {
                    "status_code": 303,
                    "location": "http://example.org/"
                };
            default:
                return true;
        }
    };

    this.azurestoragesource_blob_key = function(options = {}) {
        switch (self.context.identifier) {
            case "missing":
                return null;
            default:
                return self.context.identifier;
        }
    };

    this.extra_iiif_information_response_keys = function(options = {}) {
        switch (self.context.identifier) {
            case "bogus":
                return null;
            case "empty":
                return {};
            default:
                return {
                    "attribution": "Copyright My Great Organization. All rights reserved.",
                    "license": "http://example.org/license.html",
                    "service": {
                        "@context": "http://iiif.io/api/annex/services/physdim/1/context.json",
                        "profile": "http://iiif.io/api/annex/services/physdim",
                        "physicalScale": 0.0025,
                        "physicalUnits": "in"
                    }
                }
        }
    };

    this.filesystemsource_pathname = function(options = {}) {
        switch (self.context.identifier) {
            case "missing":
                return null;
            default:
                return self.context.identifier;
        }
    };

    this.httpsource_resource_info = function(options = {}) {
        switch (self.context.identifier) {
            case "string":
                return "http://example.org/foxes";
            case "hash":
                return {
                    "uri": "http://example.org/birds"
                };
            default:
                return null;
        }
    };

    this.jdbcsource_database_identifier = function(options = {}) {
        return self.context.identifier;
    };

    this.jdbcsource_media_type = function(options = {}) {
        return "SELECT media_type FROM items WHERE filename = ?";
    };

    this.jdbcsource_lookup_sql = function(options = {}) {
        return "SELECT image FROM items WHERE filename = ?";
    };

    this.metadata = function(options = {}) {
        switch (self.context.identifier) {
            case "metadata":
                return "<rdf:RDF>derivative metadata</rdf:RDF>";
            default:
                return null;
        }
    };

    this.overlay = function(options = {}) {
        switch (self.context.identifier) {
            case "image":
                return {
                    "image": "/dev/cats",
                    "inset": 5,
                    "position": "bottom left"
                };
            default:
                return null;
        }
    };

    this.redactions = function(options = {}) {
        switch (self.context.identifier) {
            case "bogus":
                return null;
            case "empty":
                return [];
            default:
                return [
                    {
                        "x": 0,
                        "y": 10,
                        "width": 50,
                        "height": 70
                    }
                ];
        }
    };

    this.s3source_object_info = function(options = {}) {
        switch (self.context.identifier) {
            case "bogus":
                return null;
            default:
                return {
                    "key": self.context.identifier,
                    "bucket": "test.cantaloupe.library.illinois.edu"
                }
        }
    };

    this.source = function(options = {}) {
        switch (self.context.identifier) {
            case "bogus":
                return null;
            default:
                return "FilesystemSource";
        }
    };

}
