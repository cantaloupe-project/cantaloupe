require 'uri'

module Cantaloupe

    def self.get_pathname(identifier)
      '/bla/' + identifier
    end

    def self.get_url(identifier)
      'http://example.org/bla/' + URI.escape(identifier)
    end

end