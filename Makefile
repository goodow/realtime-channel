.SUFFIXES: .java .m
.PHONY: default clean translate link

J2OBJC_DIST = ~/dev/tools/lib/j2objc/dist
M2_REPO = ~/.m2/repository

ROOT_DIR = ..
GDREALTIME_DIR = $(ROOT_DIR)/GDRealtime
BUILD_DIR = target/j2objc
SOURCE_LIST = $(BUILD_DIR)/java.sources.list
MAIN_SRC_DIR = src/main/java
MAIN_GEN_DIR = $(GDREALTIME_DIR)/Classes/generated/channel
MAIN_SOURCES = $(shell find $(MAIN_SRC_DIR) -name *.java ! -name ChannelNative.java \
  ! -name JreChannelFactory.java ! -name Js*.java)
MAIN_TEMP_SOURCES = $(subst $(MAIN_SRC_DIR), $(MAIN_GEN_DIR), $(MAIN_SOURCES))
MAIN_GEN_SOURCES = $(MAIN_TEMP_SOURCES:.java=.m)
OVERRIDE_GEN_DIR = $(GDREALTIME_DIR)/Classes/override_generated/channel

OCNI_SRC_DIR = src/main/objectivec
OCNI_SOURCES = $(shell find $(OCNI_SRC_DIR) -name *.java)
OCNI_TEMP_SOURCES = $(subst $(OCNI_SRC_DIR), $(BUILD_DIR), $(OCNI_SOURCES))
OCNI_GEN_SOURCES = $(OCNI_TEMP_SOURCES:.java=.placeholder)

J2OBJC = $(J2OBJC_DIST)/j2objc -use-arc \
  --prefixes $(ROOT_DIR)/resources/j2objc/package-prefixes.properties \
  --mapping $(ROOT_DIR)/resources/j2objc/method-mappings.properties
JUNIT_JAR = $(J2OBJC_DIST)/lib/junit-4.10.jar
TEMP_PATH = $(J2OBJC_DIST)/lib/guava-13.0.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/gwt/gwt-elemental/2.5.1-SNAPSHOT/gwt-elemental-2.5.1-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/org/timepedia/exporter/gwtexporter/2.5.0-SNAPSHOT/gwtexporter-2.5.0-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/com/google/gwt/gwt-user/2.5.1/gwt-user-2.5.1.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/realtime/realtime-model/0.0.1-SNAPSHOT/realtime-model-0.0.1-SNAPSHOT.jar
CLASSPATH = $(shell echo $(TEMP_PATH) | sed 's/ //g')
    
default: clean translate pod_update

translate: translate_main

pod_update: 
	@cd $(GDREALTIME_DIR)/Project;pod update

pre_translate_main: $(MAIN_GEN_DIR)
	@rm -f $(SOURCE_LIST)
	@mkdir -p `dirname $(SOURCE_LIST)`
	@touch $(SOURCE_LIST)
        
$(MAIN_GEN_DIR)/%.m $(MAIN_GEN_DIR)/%.h: $(MAIN_SRC_DIR)/%.java
	@echo $? >> $(SOURCE_LIST)
$(BUILD_DIR)/%.placeholder: $(OCNI_SRC_DIR)/%.java
	@echo $? >> $(SOURCE_LIST)
	@mkdir -p `dirname $@`
	@touch $@

translate_main: pre_translate_main $(MAIN_GEN_SOURCES) $(OCNI_GEN_SOURCES)
	@if [ `cat $(SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR) -d $(MAIN_GEN_DIR) \
	    -classpath $(CLASSPATH) \
	    `cat $(SOURCE_LIST)` ; \
	fi
	@cp -r $(OVERRIDE_GEN_DIR)/ $(MAIN_GEN_DIR)
	@cd $(MAIN_GEN_DIR);tar -c . | tar -x -C ../include --include=*.h

$(MAIN_GEN_DIR):
	@mkdir -p $(MAIN_GEN_DIR)
$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

clean:
	@rm -rf $(MAIN_GEN_DIR) $(BUILD_DIR)
