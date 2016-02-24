#!/usr/bin/env ruby
#
# Prepares a new user manual version based on a clone of an existing version.
#

require 'FileUtils'

def print_usage
  puts 'Usage:
  prep_version.rb <path to manual> <new version number>'
end

if ARGV.length < 2
  print_usage
  exit
end

def update_version_in_text_in_pathname(pathname)
  Dir.glob(pathname + '/**/*.html').each do |file|
    text = File.read(file)
    new_contents = text.gsub(">#{source_version}<", ">#{dest_version}<").
        gsub("/#{source_version}", "/#{dest_version}").
        gsub("/_#{source_version}/", "/_#{dest_version}/")
    File.open(file, 'w') { |file| file.puts new_contents }
  end
end

# Copy the manual
source_pathname = ARGV[0]
dest_version = ARGV[1]
source_version = source_pathname.chomp(File::SEPARATOR).
    split(File::SEPARATOR).last
dest_pathname = source_pathname.gsub(
    'manual' + File::SEPARATOR + source_version,
    'manual' + File::SEPARATOR + dest_version)


FileUtils.rm_r(dest_pathname) if File.exist?(dest_pathname)
FileUtils.cp_r(source_pathname, dest_pathname)
update_version_in_text_in_pathname(dest_pathname)

# Copy the table of contents include
source_pathname = '_includes/_manual/_' + source_version
dest_pathname = source_pathname.gsub(
    '_manual/_' + source_version,
    '_manual/_' + dest_version)

FileUtils.rm_r(dest_pathname) if File.exist?(dest_pathname)
FileUtils.cp_r(source_pathname, dest_pathname)
update_version_in_text_in_pathname(dest_pathname)
