#!/usr/bin/env ruby
#
# Generates the project javadoc.
#

def print_usage
  puts 'Usage: build_javadoc.rb <source pathname> <manual version>'
end

if ARGV.length != 2
  print_usage
  exit
end

version = ARGV[1]
source_pathname = ARGV[0]
doc_pathname = "javadoc/#{version}"

`rm -rf #{doc_pathname}`
`mkdir -p #{doc_pathname}`
`javadoc -d #{doc_pathname} -sourcepath #{source_pathname} -subpackages edu.illinois.library.cantaloupe`
