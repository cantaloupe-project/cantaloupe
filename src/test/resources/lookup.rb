require 'uri'

def get_pathname(identifier)
  '/bla/' + identifier
end

def get_url(identifier)
  'http://example.org/bla/' + URI.escape(identifier)
end
