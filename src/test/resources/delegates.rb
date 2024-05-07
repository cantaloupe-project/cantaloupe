# This file is used by several different tests.

require 'java'
require 'uri'
require 'cgi'

class CustomDelegate

  attr_accessor :context

  ##
  # Works the same as `StandardMetaIdentifierTransformer.deserialize()`.
  #
  def deserialize_meta_identifier(meta_identifier)
    reversed_meta_id = meta_identifier.reverse
    matches  = /^((?<sc>\d+:\d+);)?((?<pg>\d+);)?(?<id>.+)/.match(reversed_meta_id)
    captures = matches.named_captures
    struct                     = {}
    struct['identifier']       = captures['id'].reverse
    struct['page_number']      = captures['pg'].reverse.to_i if captures['pg']
    struct['scale_constraint'] = captures['sc'].reverse.split(':').map(&:to_i) if captures['sc']
    struct
  end

  ##
  # Works the same as `StandardMetaIdentifierTransformer.serialize()`.
  #
  def serialize_meta_identifier(components)
    [
        components['identifier'],
        components['page_number'],
        components['scale_constraint']&.join(':')
    ].reject(&:nil?).join(';')
  end

  def pre_authorize(options = {})
    case context['identifier']
      when 'forbidden.jpg', 'forbidden-boolean.jpg'
        return false
      when 'forbidden-code.jpg', 'unauthorized.jpg'
        return {
            'status_code' => 401,
            'challenge' => 'Basic'
        }
      when 'redirect.jpg'
        return {
            'status_code' => 303,
            'location' => 'http://example.org/'
        }
      when 'reduce.jpg'
        return {
            'status_code' => 302,
            'scale_numerator' => 1,
            'scale_denominator' => 2
        }
      else
        true
    end
  end

  def authorize(options = {})
    pre_authorize
  end

  def extra_iiif2_information_response_keys(options = {})
    case context['identifier']
      when 'bogus'
        return nil
      when 'empty'
        return {}
      else
        return {
            'attribution' => 'Copyright My Great Organization. All rights reserved.',
            'license' => 'http://example.org/license.html',
            'service' => {
                '@context' => 'http://iiif.io/api/annex/services/physdim/1/context.json',
                'profile' => 'http://iiif.io/api/annex/services/physdim',
                'physicalScale' => 0.0025,
                'physicalUnits' => 'in'
            }
        }
    end
  end

  def extra_iiif3_information_response_keys(options = {})
    extra_iiif2_information_response_keys(options)
  end

  def source(options = {})
    case context['identifier']
      when 'http'
        return 'HttpSource'
      when 'jdbc'
        return 'JdbcSource'
      when 'bogus'
        return nil
      else
        return 'FilesystemSource'
    end
  end

  def azurestoragesource_blob_key(options = {})
    case context['identifier']
      when 'missing'
        return nil
      when 'jpeg.jpg'
        return 'jpg'
      else
        return context['identifier']
     end
  end

  def filesystemsource_pathname(options = {})
    case context['identifier']
      when 'missing'
        nil
      when 'FilesystemSourceTest-extension-in-identifier-but-not-filename.jpg'
        'jpg'
      else
        context['identifier']
    end
  end

  def httpsource_resource_info(options = {})
    identifier = context['identifier']

    ########################## DelegateProxyTest ############################

    case identifier
      when 'DelegateProxyTest-String'
        return 'http://example.org/foxes'
      when 'DelegateProxyTest-Hash'
        return { 'uri' => 'http://example.org/birds' }
    end

    ############################ HttpSourceTest #############################

    if identifier == 'HttpSourceTest-extension-in-identifier-but-not-filename.jpg'
      return 'jpg'
    end

    # Supply a localhost URL to return the same URL.
    if identifier.start_with?('http://localhost') or
        identifier.start_with?('https://localhost')
      return {
          'uri' => identifier,
          'headers' => {
              'X-Custom' => 'yes'
          }
      }
    # Supply a valid URL prefixed with "valid-auth-" to return a valid URL
    # with valid auth info.
    elsif identifier.start_with?('valid-auth-')
      return {
          'uri' => identifier.gsub('valid-auth-', ''),
          'username' => 'user',
          'secret' => 'secret',
      }
    # Supply a valid URL prefixed with "invalid-auth-" to return a valid URL
    # with invalid auth info.
    elsif identifier.start_with?('invalid-auth-')
      return {
          'uri' => identifier.gsub('invalid-auth-', ''),
          'username' => 'user',
          'secret' => 'bogus'
      }
    elsif context['client_ip'] == '1.2.3.4'
      if context['request_headers']['X-Forwarded-Proto'] == 'https'
        return {
            'uri' => 'https://other-example.org/bleh/' + CGI.escape(identifier)
        }
      else
        return {
            'uri' => 'http://other-example.org/bleh/' + CGI.escape(identifier)
        }
      end
    end

    case identifier
      when 'http-jpg-rgb-64x56x8-baseline.jpg'
        return {
            'uri' => 'http://example.org/bla/' + CGI.escape(identifier),
            'headers' => {
                'X-Custom' => 'yes'
            },
            'send_head_request' => true
        }
      when 'https-jpg-rgb-64x56x8-baseline.jpg'
        return {
            'uri' => 'https://example.org/bla/' + CGI.escape(identifier),
            'headers' => {
                'X-Custom' => 'yes'
            },
            'send_head_request' => true
        }
      when 'http-jpg-rgb-64x56x8-plane.jpg'
        return {
            'uri' => 'http://example.org/bla/' + CGI.escape(identifier),
            'username' => 'username',
            'secret' => 'secret',
            'headers' => {
                'X-Custom' => 'yes'
            },
            'send_head_request' => true
        }
      when 'https-jpg-rgb-64x56x8-plane.jpg'
        return {
            'uri' => 'https://example.org/bla/' + CGI.escape(identifier),
            'username' => 'username',
            'secret' => 'secret',
            'headers' => {
                'X-Custom' => 'yes'
            },
            'send_head_request' => true
        }
    end
    nil
  end

  def jdbcsource_database_identifier(options = {})
    context['identifier']
  end

  def jdbcsource_last_modified(options = {})
    'SELECT last_modified FROM items WHERE filename = ?'
  end

  def jdbcsource_media_type(options = {})
    'SELECT media_type FROM items WHERE filename = ?'
  end

  def jdbcsource_lookup_sql(options = {})
    'SELECT image FROM items WHERE filename = ?'
  end

  def s3source_object_info(options = {})
    identifier = context['identifier']
    if identifier.include?('bucket:') or identifier.include?('key:')
      parts = identifier.split(';')
      struct = {}
      parts.each do |part|
        kv = part.split(':')
        struct[kv[0]] = kv[1]
      end
      return struct
    elsif identifier == 'bogus'
      return nil
    else
      {
          'key' => identifier,
          'bucket' => 'test.cantaloupe.library.illinois.edu'
      }
    end
  end

  def overlay(options = {})
    case context['identifier']
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
            'stroke_width' => 3,
            'word_wrap' => false
        }
    end
    nil
  end

  def redactions(options = {})
    case context['identifier']
      when 'empty'
        return []
      when 'redacted'
        return [ { 'x' => 0, 'y' => 10, 'width' => 50, 'height' => 70 } ]
      else
        return nil
    end
  end

  def metadata(options = {})
    case context['identifier']
      when 'metadata'
        return '<rdf:RDF>derivative metadata</rdf:RDF>'
      else
        return nil
    end
  end

end
