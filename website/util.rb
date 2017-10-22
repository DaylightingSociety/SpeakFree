#!/usr/bin/env ruby

require 'kramdown'
require_relative 'configuration'

=begin
	This file contains utility functions several different files may
	want access to, like markdown rendering.
=end

def getMarkdown(filename)
	begin
		t = File.read(Configuration::Private + "/" + filename)
		return Kramdown::Document.new(t).to_html
	rescue Exception => e
		return ""
	end
end

def getDirSize(dirname)
	size = 0
	for filename in Dir.entries(dirname)
		if( filename != "." and filename != ".." )
			size += File.size(dirname + "/" + filename)
		end
	end
	return size
end
