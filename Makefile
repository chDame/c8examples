all: buildfront run

runfront:
	cd src/main/chatbot; npm run start

buildall: buildfront package

buildfront:
ifeq ("$(wildcard src/main/chatbot/node_modules)","")
	cd src/main/chatbot; npm install
endif
	cd src/main/chatbot; npm run build
	-rm -rf src/main/resources/static/chatbot
	cp -r src/main/chatbot/dist/chatbot src/main/resources/static/chatbot
	# -rm -rf target

package:	
	mvn clean package

run:
	mvn spring-boot:run

npminstall:
	cd src/main/chatbot; npm install
