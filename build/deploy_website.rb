#!/usr/bin/env ruby
#
# Generates the project website using jekyll and uploads it to GitHub Pages.
#

require 'tmpdir'

# make sure there are no outstanding changes before beginning
#raise 'Outstanding changes' unless
#    `git status`.include?('nothing to commit, working directory clean')

# make sure there are no outstanding changes before beginning
#raise 'Outstanding changes' unless
#    `git status`.include?('nothing to commit, working directory clean')

# get the current git branch
starting_branch = nil
orphan_exists = false
`git branch --no-color`.each_line do |line|
  branch = line.gsub('*', '').strip
  starting_branch = branch if line[0] == '*'
  orphan_exists = true if branch == 'gh-pages'
end

# generate site in a temp dir
Dir.mktmpdir('website') do |tmp_dir|
  puts "Building site in #{tmp_dir}"
  `jekyll build -d #{tmp_dir}`

  # switch to gh-pages branch
  if orphan_exists
    result = system('git checkout gh-pages')
  else
    result = system('git checkout --orphan gh-pages')
  end
  puts result
  raise 'Failed to checkout gh-pages' unless result

  # wipe it clean and copy the docs back into it
  `git rm -rf .`
  `cp -r #{File.join(tmp_dir, '*')} .`

  # commit and push
  `git add *`
  `git commit -m 'Update website'`
  `git push origin gh-pages`
end

`git checkout #{starting_branch}`
