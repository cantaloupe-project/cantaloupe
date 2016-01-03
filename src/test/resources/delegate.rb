module Cantaloupe

  def self.get_iiif2_service(identifier, image_uri, compliance_level)
    {
      '@context' => "http://iiif.io/api/image/2/context.json",
      '@id' => image_uri,
      'profile' => compliance_level,
      'cats' => 0.85
    }
  end

end
