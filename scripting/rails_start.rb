ROOT = File.expand_path(File.join(Dir.pwd, '..', '..', 'scripting', 'rails'))
$LOAD_PATH.unshift(ROOT)
Dir.chdir(ROOT)
require 'rubygems'
# Gem.clear_paths
# $BUNDLE=true
Gem.path.unshift(File.expand_path(ROOT+"/vendor/gems"))
Gem.refresh

require "#{ROOT}/config/boot"
require 'commands/server'