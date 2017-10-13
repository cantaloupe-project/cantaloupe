require 'uri'

module Cantaloupe

  def self.authorized?(identifier, full_size, operations, resulting_size,
      output_format, request_uri, request_headers, client_ip, cookies)
    case identifier
      when 'forbidden.jpg'
        return false
      when 'redirect.jpg'
        return {
          'location' => 'http://example.org/',
          'status_code' => 303
        }
    end
    true
  end

  def self.extra_iiif2_information_response_keys(identifier)
    {
        'attribution' =>  'Copyright My Great Organization. All rights reserved.',
        'license' =>  'http://example.org/license.html',
        'service' => {
            '@context' => 'http://iiif.io/api/annex/services/physdim/1/context.json',
            'profile' => 'http://iiif.io/api/annex/services/physdim',
            'physicalScale' => 0.0025,
            'physicalUnits' => 'in'
        }
    }
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
    def self.get_pathname(identifier, context)
      '/bla/' + identifier
    end
  end

  module AzureStorageResolver
    def self.get_blob_key(identifier, context)
      identifier
    end
  end

  module AmazonS3Resolver
    def self.get_object_key(identifier, context)
      identifier
    end
  end

  module HttpResolver
    def self.get_url(identifier, context)
      if context['clientIP'] == '1.2.3.4' && context['headers']['x-test-header'] == 'foo'
        return 'http://other-example.org/bleh/' + URI.escape(identifier)
      end

      case identifier
        when 'jpg-rgb-64x56x8-baseline.jpg'
          return 'http://example.org/bla/' + URI.escape(identifier)
        when 'jpg-rgb-64x56x8-plane.jpg'
          return {
              'uri' => 'http://example.org/bla/' + URI.escape(identifier),
              'username' => 'username',
              'secret' => 'secret'
          }
      end
      nil
    end
  end

  module JdbcResolver
    def self.get_database_identifier(identifier, context)
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

  def self.overlay(identifier, operations, resulting_size, output_format,
      request_uri, request_headers, client_ip, cookies)
    case identifier
      when 'image'
        return {
            'image' => '/dev/cats',
            'inset' => 5,
            'position' => 'bottom left'
        }
      when 'string'
        return {
            'background_color' => 'rgba(12, 23, 34, 45)',
            'string' => "dogs\ndogs",
            'inset' => 5,
            'position' => 'bottom left',
            'color' => 'red',
            'font' => 'SansSerif',
            'font_size' => 20,
            'font_min_size' => 11,
            'font_weight' => 1.5,
            'glyph_spacing' => 0.1,
            'stroke_color' => 'blue',
            'stroke_width' => 3
        }
    end
    false
  end

end
