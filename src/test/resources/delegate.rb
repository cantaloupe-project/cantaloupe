module Cantaloupe

  def self.authorized?(identifier, operations, output_format, full_size,
      request_uri, request_headers, client_ip, cookies)
    identifier != 'forbidden.jpg'
  end

  def self.get_iiif2_service(identifier)
    {
      '@context' => "http://iiif.io/api/image/2/context.json",
      '@id' => "bla",
      'profile' => "bla",
      'cats' => 0.85
    }
  end

  def self.get_resolver(identifier)
    'FilesystemResolver'
  end

  def self.get_s3_object_key(identifier)
    identifier
  end

end
