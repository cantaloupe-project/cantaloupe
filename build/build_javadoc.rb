#!/usr/bin/env ruby
#
# Generates the project javadoc. Pass the output pathname as an argument.
#

if ARGV.empty?
  puts 'Missing destination pathname argument.'
  exit
end

`rm -rf website/javadoc`
`mkdir -p website/javadoc`
`javadoc -d #{ARGV[0]} -sourcepath src/main/java -subpackages edu.illinois.library.cantaloupe`
