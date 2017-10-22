#!/usr/bin/env ruby

module Configuration
	# Pathnames
	Private = File.dirname(__FILE__) + "/private"
	TipDir = Private + "/submitted"
	TipFile = Private + "/tips.lst"
	MaxTipLength = 256
	MaxTipDirSize = 2097152 # 2 MB
	Tips = File.read(TipFile).split("\n")
end
