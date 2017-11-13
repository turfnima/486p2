#
# To rebuild the whole thing, run make rebuild
# (After make clean one should rune make rebuild twice.)
# To recompile incrementally, use make
# To run with/out the GUI use make runnogui or make rungui
# To run through the debugger, use make gebug
# To generate project templates, use make templates
# To run the project generator, use make gen
#
DEST_DIR=./build/classes
RM_DEST_DIR=./build
DEL_COMMAND=-@rm -rf
DOC_DIR=./html
BACKUP_WILDCARD=*~
BACKUP_WILDCARD2=*.bak
HTML_DIR=./html
VERSION_WILDCARD=.\#*
JAVAC_FLAGS=-g  -nowarn
JAVA_FLAGS=-enableassertions
JAVAC=javac
JAVA=java

OPTS=

# SOURCES and CLASSES are defined here
include .depend


default: prepare build


prepare:
	-@mkdir $(RM_DEST_DIR)
	-@mkdir $(DEST_DIR)



build: prepare
	$(JAVAC) $(JAVAC_FLAGS) -classpath .:$(DEST_DIR):$(CLASSPATH) -d $(DEST_DIR) $(SOURCES)


rebuild: clean prepare build

run:
	$(JAVA) $(JAVA_FLAGS)  -classpath $(DEST_DIR) jnachos.Main



debug: build
	jdb -classpath .:$(DEST_DIR):$(CLASSPATH) jnachos.Main 


docs:
	javadoc -private -d $(HTML_DIR) -sourcepath $(DOCSOURCES) 


cleandocs:
	$(DEL_COMMAND) $(HTML_DIR)


clean:
	$(DEL_COMMAND) $(RM_DEST_DIR)


# DO NOT DELETE
