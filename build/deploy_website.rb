#!/usr/bin/env ruby
#
# Generates the project website using jekyll and uploads it to GitHub Pages.
#

require 'tmpdir'

# make sure there are no outstanding changes before beginning
raise 'Outstanding changes' unless
    `git status`.include?('nothing to commit, working directory clean')

# get the current git branch
starting_branch = nil
orphan_exists = false
`git branch --no-color`.each_line do |line|
  branch = line.gsub('*', '').strip
  starting_branch = branch if line[0] == '*'
  orphan_exists = true if branch == 'gh-pages'
end

# generate the site in a temp dir
Dir.mktmpdir('website') do |tmp_dir|
  puts "Building site in #{tmp_dir}"
  `jekyll build -s website -d #{tmp_dir}`
  `build/build_javadoc.rb #{tmp_dir}/javadoc`

  # switch to the gh-pages branch
  if orphan_exists
    puts 'Checking out gh-pages'
    result = system('git checkout gh-pages')
  else
    puts 'Creating gh-pages'
    result = system('git checkout --orphan gh-pages')
  end

  # wipe it clean and copy the new website into the place of the old one
  `git rm -rf .`
  `cp -r #{File.join(tmp_dir, '*')} .`

  # commit and push
  `git add *.html */*`
  `git commit -m 'Update website'`
  `git push origin gh-pages`
end

`git checkout #{starting_branch}`
