.SUFFIXES: .java .m
.PHONY: default clean translate link

include ../resources/make/common.mk

MAIN_SOURCES = $(subst ./,,$(shell cd $(MAIN_SRC_DIR); find . -name *.java \
  ! -name ChannelNative.java ! -name JreChannelFactory.java ! -name Js*.java))
MAIN_GEN_SOURCES = $(MAIN_SOURCES:%.java=$(CHANNEL_GEN_DIR)/%.m)
OVERRIDE_GEN_DIR = $(GDREALTIME_DIR)/Classes/override_generated/channel

OCNI_SOURCES = $(subst ./,,$(shell cd $(OCNI_SRC_DIR); find . -name *.java))
OCNI_GEN_SOURCES = $(OCNI_SOURCES:%.java=$(BUILD_DIR)/%.placeholder)

TEMP_PATH = $(J2OBJC_DIST)/lib/guava-13.0.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/gwt/gwt-elemental/2.5.1-SNAPSHOT/gwt-elemental-2.5.1-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/org/timepedia/exporter/gwtexporter/2.5.0-SNAPSHOT/gwtexporter-2.5.0-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/com/google/gwt/gwt-user/2.5.1/gwt-user-2.5.1.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/realtime/realtime-operation/0.3.0-SNAPSHOT/realtime-operation-0.3.0-SNAPSHOT.jar
CLASSPATH = $(shell echo $(TEMP_PATH) | sed 's/ //g')
    
default: clean translate pod_update

translate: translate_main

pod_update: 
	@cd $(GDREALTIME_DIR)/Project;pod update

pre_translate_main: $(CHANNEL_GEN_DIR)
	@rm -f $(MAIN_SOURCE_LIST)
	@mkdir -p `dirname $(MAIN_SOURCE_LIST)`
	@touch $(MAIN_SOURCE_LIST)
        
$(CHANNEL_GEN_DIR)/%.m $(CHANNEL_GEN_DIR)/%.h: $(MAIN_SRC_DIR)/%.java
	@echo $? >> $(MAIN_SOURCE_LIST)
$(BUILD_DIR)/%.placeholder: $(OCNI_SRC_DIR)/%.java
	@echo $? >> $(MAIN_SOURCE_LIST)
	@mkdir -p `dirname $@`
	@touch $@

translate_main: pre_translate_main $(MAIN_GEN_SOURCES) $(OCNI_GEN_SOURCES)
	@if [ `cat $(MAIN_SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR) -d $(CHANNEL_GEN_DIR) \
	    -classpath $(CLASSPATH) \
	    `cat $(MAIN_SOURCE_LIST)` ; \
	fi
	@cp -r $(OVERRIDE_GEN_DIR)/ $(CHANNEL_GEN_DIR)
	@cd $(CHANNEL_GEN_DIR);mkdir -p ../include;tar -c . | tar -x -C ../include --include=*.h

$(CHANNEL_GEN_DIR):
	@mkdir -p $(CHANNEL_GEN_DIR)
$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

clean:
	@rm -rf $(CHANNEL_GEN_DIR) $(BUILD_DIR)
