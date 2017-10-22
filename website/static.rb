=begin
	This file maintains any static pages, like the about page.
=end

require_relative 'util'

get '/' do
	erb :frontpage
end

get '/about' do
	about = getMarkdown("about.md")
	erb :markdown, :locals => {:markdown => about}
end

get '/propaganda' do
	erb :propaganda
end

get '/download' do
	erb :download
end
