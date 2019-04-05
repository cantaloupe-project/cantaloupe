#!/usr/bin/env ruby
#
# Prepares a new user manual version based on a clone of an existing version.
#

require 'FileUtils'

def print_usage
  puts 'Usage:
  prep_version.rb <current version> <new version>'
end

if ARGV.length != 2
  print_usage
  exit
end

def update_version_in_text_in_pathname(pathname, source_version, dest_version)
  Dir.glob(pathname + '/**/*.html').each do |file|
    text = File.read(file)
    new_contents = text.gsub(">#{source_version}<", ">#{dest_version}<").
        gsub("/#{source_version}", "/#{dest_version}").
        gsub("/_#{source_version}/", "/_#{dest_version}/").
        gsub("layout: manual_#{source_version}", "layout: manual_#{dest_version}")
    File.open(file, 'w') { |file| file.puts new_contents }
  end
end

source_version = ARGV[0]
dest_version = ARGV[1]
source_pathname = "#{Dir.getwd}/website/manual/#{source_version}"
dest_pathname = "#{Dir.getwd}/website/manual/#{dest_version}"

# Copy the manual
FileUtils.rm_r(dest_pathname) if File.exist?(dest_pathname)
FileUtils.cp_r(source_pathname, dest_pathname)
update_version_in_text_in_pathname(dest_pathname, source_version, dest_version)

# Copy the table of contents include
source_pathname = Dir.getwd + '/website/_includes/_manual/_' + source_version
dest_pathname = Dir.getwd + '/website/_includes/_manual/_' + dest_version

FileUtils.rm_r(dest_pathname) if File.exist?(dest_pathname)
FileUtils.cp_r(source_pathname, dest_pathname)
update_version_in_text_in_pathname(dest_pathname, source_version, dest_version)

# Copy the layout
source_pathname = "#{Dir.getwd}/website/_layouts/manual_#{source_version}.html"
dest_pathname = "#{Dir.getwd}/website/_layouts/manual_#{dest_version}.html"

FileUtils.rm(dest_pathname) if File.exist?(dest_pathname)
FileUtils.cp(source_pathname, dest_pathname)
update_version_in_text_in_pathname(dest_pathname, source_version, dest_version)

# Update the main page redirect
source_pathname = "#{Dir.getwd}/website/manual/index.html"
text = File.read(source_pathname)
text.gsub!(/\/manual\/#{source_version.gsub('.', '\.')}/, "/manual/#{dest_version}")
File.write(source_pathname, text)
