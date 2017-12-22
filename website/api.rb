#!/usr/bin/env ruby

require 'digest/sha1'
require_relative 'configuration'
require_relative 'util'

get '/getTips/iOS' do
	header = <<END
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<array>
END
	footer = <<END
</array>
</plist>
END
	response = header
	for tip in Configuration::Tips
		response << "<string>#{tip}</string>\n"
	end
	response << footer
	return response
end

get '/getTips/android' do
	header = <<END
<?xml version="1.0" encoding="utf-8"?>
<resources>
	<string-array name="tips">
END
	footer = <<END
	</string-array>
</resources>
END
	response = header
	for tip in Configuration::Tips
		response << "<item>#{tip.gsub(/'/){"\\'"}}</item>\n"
	end
	response << footer
	return response
end

post '/submitTip' do
	tip = params['tip']
	if( tip.nil? || tip.strip.length == 0 )
		halt 400, "No tip submitted!"
	elsif( tip.bytesize > Configuration::MaxTipLength )
		halt 400, "Tip too large (max #{Configuration::MaxTipLength} bytes)!"
	elsif( getDirSize(Configuration::TipDir) > Configuration::MaxTipDirSize )
		halt 507, "Server is full, please try again later."
	end
	filename = Digest::SHA1.hexdigest(tip) + ".txt"
	f = File.open("#{Configuration::TipDir}/#{filename}", "w")
	f.write(tip.strip + "\n")
	f.close
	return "Tip saved. Thank you for your submission.\n"
end
