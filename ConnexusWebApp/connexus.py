from __future__ import with_statement
from google.appengine.api import users, files, images
from google.appengine.ext import blobstore
import webapp2
from google.appengine.ext import ndb
from google.appengine.ext.webapp import blobstore_handlers
import urllib
import datetime
from google.appengine.api import mail
import time
import re
import jinja2
import os
import json
import random

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)

MAIN_PAGE_TEMPLATE_HEAD = """
<html>
	<body>
		Welcome to Connexus! <a href="%s">Logout</a><br>
		<a href="%s">Manage</a>  |  <a href="%s">Create</a>  |  <a href="%s">View</a>  |  <a href="%s">Search</a>  |  <a href="%s">Trending</a><br>"""
MAIN_PAGE_TEMPLATE_FOOTER = """
	</body>
</html>
"""

class Image(ndb.Model):
	user = ndb.StringProperty()
  	blob_key = ndb.BlobKeyProperty()
  	date = ndb.DateTimeProperty(auto_now_add=True)
  	caption = ndb.StringProperty(indexed=False)
  	loc = ndb.GeoPtProperty(required=True, default=ndb.GeoPt(0,0))


class Keywords(ndb.Model):
	globalKeywordsArray = ndb.StringProperty(repeated=True)

if Keywords.query().get() == None:
	keywords = Keywords()
	keywords.put()
	
class Stream(ndb.Model):
	streamName = ndb.StringProperty()
	creator = ndb.StringProperty()
	dateCreated = ndb.DateTimeProperty(auto_now_add=True)
	photos = ndb.StructuredProperty(Image, repeated=True)
	subscribers = ndb.StringProperty(repeated=True)
	inviteSubscribers = ndb.StringProperty()
	views = ndb.IntegerProperty()
	viewsPastHour = ndb.IntegerProperty()
	coverURL = ndb.StringProperty()
	tags = ndb.StringProperty()
	optionalMessage = ndb.StringProperty()

class Subscriber(ndb.Model):
	user = ndb.UserProperty()
	trendUpdate = ndb.StringProperty()

class MainPage(webapp2.RequestHandler):
    def get(self):
		user = users.get_current_user()
		if user:
			self.redirect('/manage')
			url = None
		else:
			url = users.create_login_url('/')
		template_values = {'url': url}
		template = JINJA_ENVIRONMENT.get_template('main.html')

		self.response.write(template.render(template_values))

class Management(webapp2.RequestHandler):
	def get(self):
		user = users.get_current_user()
		subscribedStreamList = []
		if user is None:
			self.redirect('/')
		keyWordList = ""
		keywordObject=Keywords.query()
		for q in keywordObject:
			for word in q.globalKeywordsArray:
				keyWordList=keyWordList+','+str(word)
		logoutURL = users.create_logout_url('/')
		stream_query = Stream.query().order(-Stream.dateCreated)
		for stream in stream_query:
			if str(user) in stream.subscribers:
				subscribedStreamList.append(stream)

		template_values = {
			'logoutURL' : logoutURL,
			'stream_query' : stream_query,
			'subscribedStreamList' : subscribedStreamList,
			'user' : user,
			'keyWordList':keyWordList
		}
		template = JINJA_ENVIRONMENT.get_template('manage.html')

		self.response.write(template.render(template_values))

class DeleteStreamHandler(webapp2.RequestHandler):
	def post(self):
		deleteStreamList=[]
		deleteStreamList=self.request.get_all('deleteCheckbox')
		stream_query = Stream.query()



		keywordObject = Keywords.query()





		for stream in stream_query:
			if str(stream.key.id()) in deleteStreamList:





				print stream.streamName
				try:
					for q in keywordObject:
						q.globalKeywordsArray.remove(stream.streamName)
						q.put()

					for tag in stream.tags.split(' '):
						for q in keywordObject:
							q.globalKeywordsArray.remove(tag)
							q.put()
				except:
					pass






				for pic in stream.photos:
					images.delete_serving_url(pic.blob_key)
					blobstore.delete(pic.blob_key,rpc=None)
				stream.key.delete()
		self.redirect('/manage')

class UnsubscribeHandler(webapp2.RequestHandler):
	def post(self):
		user = users.get_current_user()
		unsubscribeList = []
		unsubscribeList = self.request.get_all('unsubscribeCheckbox')
		stream_query = Stream.query()
		for stream in stream_query:
			if str(stream.key.id()) in unsubscribeList:
				if str(user) in stream.subscribers:
					stream.subscribers.remove(str(user))
					stream.put()
		self.redirect('/manage')

class UnsubscribeHandler2(webapp2.RequestHandler):
	def post(self,streamid):
		user = users.get_current_user()
		stream_query = Stream.query()
		for stream in stream_query:
			if str(stream.key.id()) == streamid:
				if str(user) in stream.subscribers: 
					stream.subscribers.remove(str(user))
					stream.put()
		self.redirect('/view/%s/0_2'%streamid)

class Create(webapp2.RequestHandler):
	def get(self):
		user = users.get_current_user()
		if user is None:
			self.redirect('/')
		logoutURL = users.create_logout_url('/')

		keyWordList = ""
		keywordObject=Keywords.query()
		for q in keywordObject:
			for word in q.globalKeywordsArray:
				keyWordList=keyWordList+','+str(word)

		template_values = {
			'logoutURL' : logoutURL,
			'user' : user,
			'keyWordList': keyWordList
		}
		template = JINJA_ENVIRONMENT.get_template('create.html')

		self.response.write(template.render(template_values))
class CreateStreamHandler(webapp2.RequestHandler):
	def post(self):
		user = users.get_current_user()
		stream_query=Stream.query()
		keywordObject = Keywords.query()
		for s in stream_query:
			if str(s.streamName) == str(self.request.get('streamName')):
				self.redirect('/error/streamNameDuplicatedError')
				return
		if str(self.request.get("streamName")) == '':
			self.redirect('/error/streamNameEmptyError')	
		else:
			stream = Stream(streamName=self.request.get("streamName"),creator=users.get_current_user().user_id(),photos=[],subscribers=[],views=0,viewsPastHour=0,
				coverURL=self.request.get("coverURL"),optionalMessage=self.request.get("message"),tags=self.request.get("tags"),inviteSubscribers=self.request.get("subscribers"))
			currStreamKey=stream.put()
			



			print keywordObject.get()
			for q in keywordObject:
				q.globalKeywordsArray.append(self.request.get("streamName"))
				q.put()
			for keyword in self.request.get("tags").split(" "):
				for q in keywordObject:
					q.globalKeywordsArray.append(keyword)
					q.put()
			



			if self.request.get('subscribers') != '':
				emailList = re.split('\r\n',self.request.get('subscribers'))
				for email in emailList:
					to_addr = email
		         	if not mail.is_email_valid(to_addr):
			            # Return an error message...
			            pass

			        message = mail.EmailMessage()
			        message.sender = user.email()
			        message.to = to_addr
			        message.subject = "Subscribe to " + str(user) + "'s New Stream!"
			        message.body = """Hello there!\n%s has invited you to subscribe to his new stream named %s. \nHere is what %s has to say:\n%s\nFollow this link to subscribe:\nhttp://connexusminiproject.appspot.com/subscriptionPage/%s

			        """ %(user,self.request.get("streamName"),user,self.request.get("message"),currStreamKey.id())

			        message.send()
			self.redirect('/manage')

class SubscriptionPage(webapp2.RequestHandler):
	def get(self,url):
		user = users.get_current_user()
		if user is None:
			login_url = users.create_login_url(self.request.path)
			self.redirect(login_url)
			return
		self.response.out.write(MAIN_PAGE_TEMPLATE_HEAD%(users.create_logout_url('/'),'/manage','/create','/viewAllStreams','/search','/trending'))
		self.response.out.write('<br><h3>Are you sure you want to subscribe?</h3><br><form action="/subscriptionHandler/%s" method="POST"><input type="submit" value="YES!"></form>'%url)

class SubscriptionHandler(webapp2.RequestHandler):
	def post(self,url):
		user = users.get_current_user()
		# find the stream
		stream_query = Stream.query()
		for stream in stream_query:
			if str(stream.key.id())==url:
				if str(user) not in stream.subscribers:
					stream.subscribers.append(str(user))
					stream.put()
		self.redirect('/view/%s/0_2'%url)

class ViewSingleStream(webapp2.RequestHandler):
	def get(self,url,photoIndexes):
		user = users.get_current_user()
		if user is None:
			self.redirect('/')
		keyWordList = ""
		keywordObject=Keywords.query()
		for q in keywordObject:
			for word in q.globalKeywordsArray:
				keyWordList=keyWordList+','+str(word)
		logoutURL = users.create_logout_url('/')
		q = Stream.query()
		indexList = str(photoIndexes).split('_')
		stream = ''
		picURLList = []
		captionList = []
		indexURL = photoIndexes
		image = []
		for s in q:
			if str(s.key.id())==url:
				stream = s
				if photoIndexes == '0_2':
					s.views+=1
					s.viewsPastHour+=1
					s.put()
				image = s.photos
				image = image[::-1]

		if len(image)-1>int(indexList[1]):
			for i in range(int(indexList[0]),int(indexList[1])+1):
				blob_key = image[i]
				picURL = images.get_serving_url(image[i].blob_key)
				picURLList.append(picURL)
				captionList.append(image[i].caption)
			indexURL = str(int(indexList[0])+3)+'_'+str(int(indexList[1])+3)

		else:
			for i in range(int(indexList[0]),min(int(indexList[1])+1,len(image))):
				blob_key = image[i]
				picURL = images.get_serving_url(image[i].blob_key)
				picURLList.append(picURL)
				captionList.append(image[i].caption)
		if stream == '':
			upload_url = ''
		else:
			upload_url = str((stream.key.id()))

		template_values = {
			'logoutURL' : logoutURL,
			'q' : q,
			'user' : user,
			'url' : url,
			'indexList' : indexList,
			'stream' : stream,
			'picURLList' :picURLList,
			'captionList' : captionList,
			'indexURL' : indexURL,
			'image' : image,
			'upload_url' : upload_url,
			'keyWordList':keyWordList
		}
		template = JINJA_ENVIRONMENT.get_template('viewSingleStream.html')

		self.response.write(template.render(template_values))

class ViewAllStreams(webapp2.RequestHandler):
	def get(self):
		user = users.get_current_user()
		if user is None:
			self.redirect('/')
		keyWordList = ""
		keywordObject=Keywords.query()
		for q in keywordObject:
			for word in q.globalKeywordsArray:
				keyWordList=keyWordList+','+str(word)
		logoutURL = users.create_logout_url('/')
		stream_query = Stream.query().order(-Stream.dateCreated)

		template_values = {
			'logoutURL' : logoutURL,
			'user' : user,
			'stream_query' : stream_query,
			'keyWordList' : keyWordList

		}
		template = JINJA_ENVIRONMENT.get_template('viewAllStreams.html')

		self.response.write(template.render(template_values))

class SearchHandler(webapp2.RequestHandler):
	def post(self):
		
		userInputString = self.request.get("searchContent")
		stream_query = Stream.query()
		outputStreams = []
		keyWordList = ""
		keywordObject=Keywords.query()
		for q in keywordObject:
			for word in q.globalKeywordsArray:
				keyWordList=keyWordList+','+str(word)
		if userInputString == "":
			streamsFound = []
		else:
			userInput = userInputString.split(' ')
			matchingStreamIDs=[]
			
			streamsFound = []
			for stream in stream_query:
				streamNameArray = stream.streamName.split(' ')
				streamNameArray2=[]
				tagsArray2=[]
				for s in streamNameArray:
					s=s.lower()
					s=str(s)
					streamNameArray2.append(s)
				tagsArray = stream.tags.split(' ')
				for tag in tagsArray:
					#tag=tag.replace('#','')
					tag=tag.lower()
					tag=str(tag)
					tagsArray2.append(tag)
				for keyword in userInput:
					if str(keyword.lower()) in (tagsArray2) or str(keyword.lower()) in (streamNameArray2) and stream not in streamsFound:
						streamsFound.append(stream)
		if len(streamsFound) > 5:
			outputStreams = streamsFound[:5]
		else:
			outputStreams = streamsFound
		template_values = {
			'streamsFound' : streamsFound,
			'userInputString' : userInputString,
			'outputStreams': outputStreams,
			'keyWordList': keyWordList
		}
		template = JINJA_ENVIRONMENT.get_template('search.html')

		self.response.write(template.render(template_values))	

class Trending(webapp2.RequestHandler):
	def get(self):
		user = users.get_current_user()
		if user is None:
			self.redirect('/')
		logoutURL = users.create_logout_url('/')
		keyWordList = ""
		keywordObject=Keywords.query()
		for q in keywordObject:
			for word in q.globalKeywordsArray:
				keyWordList=keyWordList+','+str(word)
		stream_query = Stream.query().order(-Stream.viewsPastHour)
		top3Streams = []
		counter = 3
		for stream in stream_query:
			counter-=1
			top3Streams.append(stream)
			if counter==0:
				break
		
		userFound = False
		subscriber_query = Subscriber.query()
		for sub in subscriber_query:
			if sub.user == user:
				userFound = sub

		template_values = {
			'logoutURL' : logoutURL,
			'user' : user,
			'stream_query' : stream_query,
			'top3Streams' : top3Streams,
			'userFound' : userFound,
			'keyWordList' : keyWordList
		}
		template = JINJA_ENVIRONMENT.get_template('trending.html')

		self.response.write(template.render(template_values))
class TrendHandler(webapp2.RequestHandler):
	def post(self):
		userFound = False
		user = users.get_current_user()
		subscriber_query = Subscriber.query()
		for sub in subscriber_query:
			if sub.user == user:
				userFound = True
				sub.trendUpdate = self.request.get('report')
				print "trendUpdate"
				print sub.trendUpdate
				sub.put()
		if userFound == False:
			print "User Not found"
			subscriberInstance = Subscriber(user=user,trendUpdate=self.request.get('report'))
			subscriberInstance.put()
		else:
			print "User FOUND"
		self.redirect('/trending')

class ViewResetHandler(webapp2.RequestHandler):
	def get(self):
		stream_query = Stream.query()
		for stream in stream_query:
			stream.viewsPastHour = 0
			stream.put()

class TrendEmail(webapp2.RequestHandler):
	def get(self):
		updateRate = ''
		updateResultList=[]
		stream_query = Stream.query().order(-Stream.viewsPastHour)
		counter = 3
		for stream in stream_query:
			counter-=1
			updateResultList.append(stream.streamName)
			updateResultList.append(str(stream.viewsPastHour))
			if counter==0:
				break
		
		subscriber_query = Subscriber.query()
		for sub in subscriber_query:
			updateRate = sub.trendUpdate

			message = mail.EmailMessage(sender="kevzsolo@gmail.com",subject="Connexus Trending Stream Update")
			message.to = sub.user.email()
			if len(stream_query.fetch(3))>2:
				message.body = """
				Hi there!

				Here are the trending streams on Connexus:

				1. %s with %s views in the past hour
				2. %s with %s views in the past hour
				3. %s with %s views in the past hour

				To change the rate of this subscription, click the following link:
				http://connexusminiproject.appspot.com/trending
				""" %(updateResultList[0],updateResultList[1],updateResultList[2],updateResultList[3],updateResultList[4],updateResultList[5])

			elif len(stream_query.fetch(3))==2:
				message.body = """
				Hi there!

				Here are the trending streams on Connexus:

				1. %s with %s views in the past hour
				2. %s with %s views in the past hour

				To change the rate of this subscription, click the following link:
				http://connexusminiproject.appspot.com/trending
				""" %(updateResultList[0],updateResultList[1],updateResultList[2],updateResultList[3])
			elif len(stream_query.fetch(3))==1:
				message.body = """
				Hi there!

				Here are the trending streams on Connexus:

				1. %s with %s views in the past hour

				To change the rate of this subscription, click the following link:
				http://connexusminiproject.appspot.com/trending
				""" %(updateResultList[0],updateResultList[1])

			else:
				message.body = """
				Hi there!

				There are currently no trending streams on Connexus.

				To change the rate of this subscription, click the following link:
				http://connexusminiproject.appspot.com/trending
				"""

			if updateRate == 'noReports' or updateRate == '':
				pass
			elif updateRate == 'every5Minutes':
				message.send()
			elif updateRate == 'every1hour':
				message.send()
			elif updateRate == 'everyday':
				message.send()



class ErrorHandler(webapp2.RequestHandler):
	def get(self,errorMessage):
		user = users.get_current_user()
		if user is None:
			self.redirect('/')
		displayError=""

		keyWordList = ""
		keywordObject=Keywords.query()
		for q in keywordObject:
			for word in q.globalKeywordsArray:
				keyWordList=keyWordList+','+str(word)
		
		if errorMessage == 'streamNameEmptyError':
			displayError='ERROR: You tried to create a new stream whose name is the same as an existing stream; operation did not complete.'
		elif errorMessage == 'streamNameDuplicatedError':
			displayError='ERROR: Please enter a stream name for you stream; operation did not complete.'
		else:
			displayError= errorMessage
		template_values = {
			'displayError' : displayError,
			'keyWordList' : keyWordList
		}
		template = JINJA_ENVIRONMENT.get_template('error.html')

		self.response.write(template.render(template_values))

class GeoView(webapp2.RequestHandler):
	def get(self,streamID):
		user = users.get_current_user()
		if user is None:
			self.redirect('/')
		keyWordList = ""
		keywordObject=Keywords.query()
		for q in keywordObject:
			for word in q.globalKeywordsArray:
				keyWordList=keyWordList+','+str(word)
		photoList=[]
		photoKeyList=[]
		stream_query = Stream.query()
		for stream in stream_query:
			if str(stream.key.id())==streamID:
				photoList=stream.photos
		for photo in photoList:
			photoKeyList.append(str(photo.blob_key))

		template_values = {
			'keyWordList' : keyWordList,
			'photoList' : photoList,
			'streamID' : streamID

		}

		template = JINJA_ENVIRONMENT.get_template('geoview.html')
		self.response.write(template.render(template_values))

class GeoViewHandler(webapp2.RequestHandler):
	def get(self):
		streamID=self.request.get("streamID")
		photoList=[]
		photoTimeDict={}
		stream_query = Stream.query()
		for stream in stream_query:
			if str(stream.key.id())==streamID:
				photoList=stream.photos
		for photo in photoList:
			#photoKeyList.append(str(photo.blob_key))
			#photodate = (str(photo.date))[0:10]
			#photodate = photodate.replace('-','')
			photoInfo = []
			photoInfo.append(photo.date.strftime('%s'))
			photoLatCord = -57.32652122521709+114.65304245043419*random.random()
			photoLngCord = -123.046875+246.09375*random.random()
			photoInfo.append(photoLatCord)
			photoInfo.append(photoLngCord)
			photoTimeDict[str(images.get_serving_url(photo.blob_key))]=photoInfo

		# print "here is dumped photoTimeDict"
		# print photoTimeDict
		# print "finish printing"

		self.response.write(json.dumps(photoTimeDict))

MIN_FILE_SIZE = 1  # bytes
MAX_FILE_SIZE = 5000000  # bytes
IMAGE_TYPES = re.compile('image/(gif|p?jpeg|(x-)?png)')
ACCEPT_FILE_TYPES = IMAGE_TYPES
THUMBNAIL_MODIFICATOR = '=s80'  # max width / height
EXPIRATION_TIME = 300  # seconds

class PhotoUploadHandler(webapp2.RequestHandler):

	def initialize(self, request, response):
		super(PhotoUploadHandler, self).initialize(request, response)
		self.response.headers['Access-Control-Allow-Origin'] = '*'
		self.response.headers[
			'Access-Control-Allow-Methods'
		] = 'OPTIONS, HEAD, GET, POST, PUT, DELETE'
		self.response.headers[
			'Access-Control-Allow-Headers'
		] = 'Content-Type, Content-Range, Content-Disposition'

	def validate(self, file):
		if file['size'] < MIN_FILE_SIZE:
			file['error'] = 'File is too small'
		elif file['size'] > MAX_FILE_SIZE:
			file['error'] = 'File is too big'
		elif not ACCEPT_FILE_TYPES.match(file['type']):
			file['error'] = 'Filetype not allowed'
		else:
			return True
		return False

	def get_file_size(self, file):
		file.seek(0, 2)  # Seek to the end of the file
		size = file.tell()  # Get the position of EOF
		file.seek(0)  # Reset the file position to the beginning
		return size

	'''
	keys = [0] * 10000
	keyIndex = 0
	visited = False
	'''

	def write_blob(self, s, data, info):
		blob = files.blobstore.create()
		with files.open(blob, 'a') as f:
			f.write(data)
		files.finalize(blob)
		# currFlag=False
		# while currFlag==False:
		# 	for flag in PutFlag.query():
		# 		currFlag=flag.allowPut
		# 		print currFlag
		q = Stream.query()
		for stream in q:
			if str(stream.key.id()) == s:

				print 'this is the correct blobkey: '
				print files.blobstore.get_blob_key(blob)
				user_photo = Image(user=users.get_current_user().user_id(),blob_key = files.blobstore.get_blob_key(blob))
				stream.photos.append(user_photo)
				stream.dateCreated=datetime.datetime.now()
				print 'put in'
				

				strkey = stream.put()
				# for flag in PutFlag.query():
				# 	flag.allowPut=True
				# 	flag.put()
				print 'put finishes'
				print strkey
				'''
				user_photo = Image(user=users.get_current_user().user_id(),blob_key = files.blobstore.get_blob_key(blob))
				PhotoUploadHandler.keys[PhotoUploadHandler.keyIndex] = user_photo
				print 'PUT IN! index is ' + str(PhotoUploadHandler.keyIndex)
				PhotoUploadHandler.keyIndex = PhotoUploadHandler.keyIndex + 1
				'''

		return files.blobstore.get_blob_key(blob)

	def handle_upload(self, stream):
		time.sleep(random.uniform(1.0,4.0))
		results = []
		for name, fieldStorage in self.request.POST.items():
			if type(fieldStorage) is unicode:
				continue
			result = {}
			result['name'] = re.sub(r'^.*\\', '', fieldStorage.filename)
			result['type'] = fieldStorage.type
			result['stream'] = stream
			result['size'] = self.get_file_size(fieldStorage.file)
			if self.validate(result):
				blob_key = str(
					self.write_blob(stream, fieldStorage.value, result)
				)
				

				result['deleteType'] = 'DELETE'
				result['deleteUrl'] = self.request.host_url +\
					'/upload?stream=' + str(stream) + '&key=' + urllib.quote(blob_key, '')
				if (IMAGE_TYPES.match(result['type'])):
					try:
						result['url'] = images.get_serving_url(
							blob_key,
							secure_url=self.request.host_url.startswith(
								'https'
							)
						)
						result['thumbnailUrl'] = result['url'] +\
							THUMBNAIL_MODIFICATOR
					except:  # Could not get an image serving url
						pass
				if not 'url' in result:
					result['url'] = self.request.host_url +\
						'/' + blob_key + '/' + urllib.quote(
							result['name'].encode('utf-8'), '')
			results.append(result)
		return results

	def options(self):
		pass

	def head(self):
		pass

	def post(self):
		try:
			streamVal = self.request.get('stream')

			if (self.request.get('_method') == 'DELETE'):
				return self.delete()
			result = {'files': self.handle_upload(streamVal)}
			s = json.dumps(result, separators=(',', ':'))
			redirect = self.request.get('redirect')
			if redirect:
				return self.redirect(str(
					redirect.replace('%s', urllib.quote(s, ''), 1)
				))
			if 'application/json' in self.request.headers.get('Accept'):
				self.response.headers['Content-Type'] = 'application/json'
			self.response.write(s)
		except Exception,e:
			print str(e)
			self.redirect('/error/%s'%e)

	def delete(self):
		key = self.request.get('key') or ''
		key = str(urllib.unquote(key))

		s = json.dumps({key : True}, separators=(',', ':'))
		if 'application/json' in self.request.headers.get('Accept'):
			self.response.headers['Content-Type'] = 'application/json'
		self.response.write(s)

		# delete images in blobstore
		images.delete_serving_url(key)
		blobstore.delete(key,rpc=None)

		# delete keys in ndb
		stream_query = Stream.query()
		for stream in stream_query:
			if str(stream.key.id()) == self.request.get('stream'):
				for pic in stream.photos:
					if pic.blob_key == key:
						index = stream.photos.index(pic)
						del stream.photos[index]
						stream.put()


application = webapp2.WSGIApplication([
    ('/', MainPage),
	('/manage', Management),
	('/create',Create),
	('/createStreamHandler',CreateStreamHandler),
	('/view/([^/]+)/([^/]+)',ViewSingleStream),
	('/viewAllStreams',ViewAllStreams),
	('/trending',Trending),
	('/searchHandler',SearchHandler),
	('/deleteStreamHandler',DeleteStreamHandler),
	('/unsubscribeHandler',UnsubscribeHandler),
	('/unsubscribeHandler2/([^/]+)',UnsubscribeHandler2),
	('/subscriptionPage/([^/]+)',SubscriptionPage),
	('/subscriptionHandler/([^/]+)',SubscriptionHandler),
	('/trendHandler',TrendHandler),
	('/viewResetHandler',ViewResetHandler),
	('/trendEmail',TrendEmail),
	('/geoview/([^/]+)',GeoView),
	('/geoviewHandler',GeoViewHandler),
	('/upload',PhotoUploadHandler),
	('/error/([^/]+)',ErrorHandler)

], debug=True)
