#!/usr/bin/env ruby
#
# Generates the project javadoc. Pass the output pathname as an argument.
#

if ARGV.length != 2
  puts 'Usage: build_javadoc.rb <version>'
  exit
end

`rm -rf website/javadoc/#{version}`
`mkdir -p website/javadoc/#{version}`
`javadoc -d #{ARGV[0]} -sourcepath src/main/java -subpackages edu.illinois.library.cantaloupe`
