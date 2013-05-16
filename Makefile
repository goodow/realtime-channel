.SUFFIXES: .java .m
.PHONY: default clean translate link

J2OBJC_DIST = ~/dev/tools/lib/j2objc/dist
M2_REPO = ~/.m2/repository

ROOT_DIR = ..
GDREALTIME_DIR = $(ROOT_DIR)/GDRealtime
BUILD_DIR = target/j2objc
SOURCE_LIST = $(BUILD_DIR)/java.sources.list
MAIN_SRC_DIR = src/main/java
MAIN_GEN_DIR = $(GDREALTIME_DIR)/Classes/generated
MAIN_SOURCES = $(shell find $(MAIN_SRC_DIR) -name *.java ! -name NativeInterface.java)
MAIN_TEMP_SOURCES = $(subst $(MAIN_SRC_DIR), $(MAIN_GEN_DIR), $(MAIN_SOURCES))
MAIN_GEN_SOURCES = $(MAIN_TEMP_SOURCES:.java=.m)

J2OBJC = $(J2OBJC_DIST)/j2objc --prefixes $(ROOT_DIR)/resources/j2objc/package-prefixes.properties \
  --mapping $(ROOT_DIR)/resources/j2objc/method-mappings.properties

default: clean translate

translate: translate_main

pre_translate_main: $(MAIN_GEN_DIR)
	@rm -f $(SOURCE_LIST)
	@mkdir -p `dirname $(SOURCE_LIST)`
	@touch $(SOURCE_LIST)
        
$(MAIN_GEN_DIR)/%.m $(MAIN_GEN_DIR)/%.h: $(MAIN_SRC_DIR)/%.java
	@echo $? >> $(SOURCE_LIST)
$(BUILD_DIR)/%.placeholder: $(JNI_SRC_DIR)/%.java
	@echo $? >> $(SOURCE_LIST)
	@mkdir -p `dirname $@`
	@touch $@

translate_main: pre_translate_main $(MAIN_GEN_SOURCES)
	@if [ `cat $(SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR) -d $(MAIN_GEN_DIR) \
	    -classpath '$(J2OBJC_DIST)/lib/guava-13.0.jar:$(M2_REPO)/org/timepedia/exporter/gwtexporter/2.5.0-SNAPSHOT/gwtexporter-2.5.0-SNAPSHOT.jar:\
$(M2_REPO)/com/goodow/gwt/gwt-elemental/2.5.1-SNAPSHOT/gwt-elemental-2.5.1-SNAPSHOT.jar:$(M2_REPO)/com/google/gwt/gwt-user/2.5.1/gwt-user-2.5.1.jar' \
	    `cat $(SOURCE_LIST)` ; \
	fi

$(MAIN_GEN_DIR):
	@mkdir -p $(MAIN_GEN_DIR)
$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

clean:
	@rm -rf $(MAIN_GEN_DIR) $(BUILD_DIR)