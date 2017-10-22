#!/usr/bin/env ruby

require 'sinatra'
require 'tilt/erb'

require_relative 'configuration'
require_relative 'static'
require_relative 'api'

error Sinatra::NotFound do
	erb :notfound
end

# This block forces SSL for all users all the time.
before '*' do
	if( request.url.start_with?("http://") )
		redirect to(request.url.sub("http", "https"))
	end
end

unless( Dir.exists?(Configuration::TipDir) )
	Dir.mkdir(Configuration::TipDir)
end
