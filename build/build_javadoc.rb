#!/usr/bin/env ruby
#
# Generates the project javadoc. Pass the output pathname as an argument.
#

if ARGV.length != 1
  puts 'Usage: build_javadoc.rb <version>'
  exit
end

version = ARGV[0]

`rm -rf website/javadoc/#{version}`
`javadoc -d website/javadoc/#{version} -sourcepath src/main/java -subpackages edu.illinois.library.cantaloupe`
