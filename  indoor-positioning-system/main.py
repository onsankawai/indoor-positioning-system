import cgi
import urllib

import json
import webapp2

from google.appengine.ext import ndb

class Rssi(ndb.Model):
	"""Models a rssi data set with BSSID and rssi strength"""
	bssid = ndb.StringProperty()
	level = ndb.IntegerProperty()

class Location(ndb.Model):
	"""Models a location signature entry with x,y coordinates and RSSI + other possible signatures"""
	xcoor = ndb.IntegerProperty()
	ycoor = ndb.IntegerProperty()
	rssis = ndb.StructuredProperty(Rssi, repeated=True)
	credibility = ndb.FloatProperty()
	date = ndb.DateTimeProperty(auto_now_add=True)

	@classmethod
	def query_map(cls, ancestor_key):
	# ancestor key must be place at the back because it is a "magic word"!
		return cls.query(ancestor=ancestor_key)		

class ViewMap(webapp2.RequestHandler):
	def get(self):
		# View map location data here
		self.response.out.write('<html><body>Hello World!')
		map_name = self.request.get('map_name')
		
		location_key = ndb.Key("Map", map_name or "*no_name*")
		locations = Location.query_map(location_key).fetch(10)
	
		self.response.out.write('Map: %s\n' % map_name)
		for location in locations:
			self.response.out.write('<blockquote>X:%s Y:%s Credibility:%s Date:%s</blockquote>' %
                              (location.xcoor, location.ycoor, location.credibility, location.date ))
		
class UpdateLocation(webapp2.RequestHandler):
	def get(self):
		# updating grid coordinates with corresponding RSSI/ other indexes
		self.response.headers['Content-Type'] = 'application/json'
		
		map_name = self.request.get('map_name')
		
		location_key = ndb.Key("Map", map_name or "*no_name*")
		local_x = int(self.request.get('xcoor'))
		local_y = int(self.request.get('ycoor'))
		locations = Location.query( Location.xcoor == local_x,
																Location.ycoor == local_y,
																ancestor=location_key).fetch(1)
		
		local_credibility = float(self.request.get('credibility'))
		local_rssi_str_list = self.request.get('rssi').split('|')
		local_rssi_data_list = []
		for local_rssi_str in local_rssi_str_list:
			rssi_list = local_rssi_str.split('!')
			rssi_data = Rssi(bssid=rssi_list[0], level=int(rssi_list[1]))
			local_rssi_data_list.append(rssi_data)
		
		result = json.dumps({'status':'NO_CHANGE'})
		
		# location found
		if len(locations) > 0:
			location = locations[0]
			if location.credibility <= local_credibility:
				# update database as local data is more trustworthy
				location.rssis = local_rssi_data_list
				location.credibility = local_credibility
				location.put()
				result = json.dumps({'status':'SERVER_UPDATE'})
			elif local_credibility < 0.7:
				# update mobile client with database entry as local data is not trustworthy
				# closest_matched_location = getClosestMatch(local_rssi, location_key)
				closest_matched_location = self.getClosestMatch(local_rssi_data_list, location_key)
				if closest_matched_location is not None:
					if self.getDistance(local_x, local_y, closest_matched_location) <= 5:
						if closest_matched_location.credibility > local_credibility:
							# Update mobile client
							result = json.dumps({'status':'MOBILE_UPDATE', 
																		'xcoor':closest_matched_location.xcoor,
																		'ycoor':closest_matched_location.ycoor,
																		'credibility':closest_matched_location.credibility})
		else:
			if local_credibility >= 0.3:
				location = location = Location(parent=ndb.Key('Map', map_name), 
																				xcoor = local_x, 
																				ycoor = local_y, 
																				rssis = local_rssi_data_list,
																				credibility = local_credibility)
				location.put()
				result = json.dumps({'status':'SERVER_UPDATE'})
																			
		self.response.out.write(result)
		
	def getClosestMatch(self, rssi_list, location_key):
		# Return location entry with closest rssi value
		# Match 3 out of 4 BSSIDs, 2 out of 3, 2 out of 2...
		if len(rssi_list) == 4:
			locations = Location.query(ndb.OR(ndb.AND(Location.rssis.bssid == rssi_list[1].bssid,
																								Location.rssis.bssid == rssi_list[2].bssid,
																								Location.rssis.bssid == rssi_list[3].bssid),
																				ndb.AND(Location.rssis.bssid == rssi_list[0].bssid,
																								Location.rssis.bssid == rssi_list[2].bssid,
																								Location.rssis.bssid == rssi_list[3].bssid),
																				ndb.AND(Location.rssis.bssid == rssi_list[0].bssid,
																								Location.rssis.bssid == rssi_list[1].bssid,
																								Location.rssis.bssid == rssi_list[3].bssid),
																				ndb.AND(Location.rssis.bssid == rssi_list[0].bssid,
																								Location.rssis.bssid == rssi_list[1].bssid,
																								Location.rssis.bssid == rssi_list[2].bssid)),ancestor=location_key).fetch(10)
		elif len(rssi_list) == 3:
			locations = Location.query(ndb.OR(ndb.AND(Location.rssis.bssid == rssi_list[1].bssid,
																								Location.rssis.bssid == rssi_list[2].bssid),
																				ndb.AND(Location.rssis.bssid == rssi_list[0].bssid,
																								Location.rssis.bssid == rssi_list[2].bssid),
																				ndb.AND(Location.rssis.bssid == rssi_list[0].bssid,
																								Location.rssis.bssid == rssi_list[1].bssid)),ancestor=location_key).fetch(10)
		elif len(rssi_list) == 2:
			locations = Location.query(ndb.AND(Location.rssis.bssid == rssi_list[1].bssid,
																				 Location.rssis.bssid == rssi_list[2].bssid),ancestor=location_key).fetch(10)
			
																								
		for location in locations:
				matches = [rssi for rssi in rssi_list if self.matchRssiData(rssi, location.rssis, 10)]
				
				if len(rssi_list) > 2:
					if len(matches) >= len(rssi_list) - 1:
						return location
				else:
					if len(matches) >= len(rssi_list):
						return location
		
		# No matches
		return None
		
	def matchRssiData(self, local_rssi, db_rssi_list, threshold):
		# Match a single rssi data with threshold on the level
		for db_rssi in db_rssi_list:
			if local_rssi.bssid == db_rssi.bssid:
				if local_rssi.level >= db_rssi.level - threshold and local_rssi.level <= db_rssi.level + threshold:
					return True
			
		return False
		
	def getDistance(self, local_x, local_y, db_loc):
		# Compute the distance between 2 grid point
		return math.sqrt((local_x - db_loc.xcoor)*(local_x - db_loc.xcoor) + (local_y - db_loc.ycoor)*(local_y - db_loc.ycoor))
		
class InitMapData(webapp2.RequestHandler):
	def get(self):
		# Initialize map data with all possible x,y with credibility set to 0
		map_name = self.request.get('map_name')
		rows = int(self.request.get('rows'))
		cols = int(self.request.get('cols'))
		
		for j in range(rows):
			for i in range(cols):
				location = Location(parent=ndb.Key('Map', map_name), 
																				xcoor = i, 
																				ycoor = j, 
																				rssis = [Rssi(bssid='empty', level=0)],
																				credibility = 0.0)
				location.put()
				
		self.redirect('/?' + urllib.urlencode({'map_name':map_name}))
		
class ClearMapData(webapp2.RequestHandler):
	def get(self):
		# Clear all map data
		map_name = self.request.get('map_name')
		location_key = ndb.Key("Map", map_name or "*no_name*")
		ndb.delete_multi(Location.query(ancestor=location_key).fetch(keys_only=True))
		
		
app = webapp2.WSGIApplication([
	('/', ViewMap),
	('/init', InitMapData),
	('/update', UpdateLocation),
	('/clear', ClearMapData)
])