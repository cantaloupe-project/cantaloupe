require 'uri'

module Cantaloupe

  def self.authorized?(identifier, full_size, operations, resulting_size,
      output_format, request_uri, request_headers, client_ip, cookies)
    identifier != 'forbidden.jpg'
  end

  def self.get_iiif2_service(identifier)
    if identifier == 'jpg-rgb-64x56x8-baseline.jpg'
      return {
        '@context' => 'http://iiif.io/api/image/2/context.json',
        '@id' => 'bla',
        'profile' => 'bla',
        'cats' => 0.85
      }
    end
    nil
  end

  def self.get_resolver(identifier)
    if identifier == 'http'
      return 'HttpResolver'
    elsif identifier == 'jdbc'
      return 'JdbcResolver'
    else
      return 'FilesystemResolver'
    end
  end

  module FilesystemResolver
    def self.get_pathname(identifier)
      '/bla/' + identifier
    end
  end

  module AzureStorageResolver
    def self.get_blob_key(identifier)
      identifier
    end
  end

  module AmazonS3Resolver
    def self.get_object_key(identifier)
      identifier
    end
  end

  module HttpResolver
    def self.get_url(identifier)
      'http://example.org/bla/' + URI.escape(identifier)
    end
  end

  module JdbcResolver
    def self.get_database_identifier(identifier)
      identifier
    end

    def self.get_media_type
      'SELECT media_type FROM items WHERE filename = ?'
    end

    def self.get_lookup_sql
      'SELECT image FROM items WHERE filename = ?'
    end
  end

  def self.redactions(identifier, request_headers, client_ip, cookies)
    redactions = []
    if identifier == 'cats'
      redactions << { 'x' => 0, 'y' => 10, 'width' => 50, 'height' => 70 }
    end
    redactions
  end

  def self.watermark(identifier, operations, resulting_size, output_format,
      request_uri, request_headers, client_ip, cookies)
    if identifier == 'cats'
      return {
          'pathname' => '/dev/cats',
          'inset' => 5,
          'position' => 'bottom left'
      }
    end
    false
  end

end
