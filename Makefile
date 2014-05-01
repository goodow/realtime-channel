.SUFFIXES: .java .m
.PHONY: default clean translate link

include ../resources/make/common.mk
# J2OBJC_DIST = GDChannel/Project/Pods/J2ObjC/dist

CHANNEL_GEN_DIR = GDChannel/Classes/generated
MAIN_SOURCES = $(subst $(MAIN_SRC_DIR)/,,$(shell find $(MAIN_SRC_DIR) -name *.java ! -path "*/html/*" ! -path "*/server/*"))
MAIN_GEN_SOURCES = $(MAIN_SOURCES:%.java=$(CHANNEL_GEN_DIR)/%.m)
OVERRIDE_GEN_DIR = GDChannel/Classes/override
MAIN_OBJECTS = $(MAIN_SOURCES:%.java=$(BUILD_DIR)/main/%.o)
SUPPORT_LIB = $(BUILD_DIR)/libGDChannel.a

TEMP_PATH = $(M2_REPO)/com/goodow/realtime/realtime-json/0.5.5-SNAPSHOT/realtime-json-0.5.5-SNAPSHOT.jar
CLASSPATH = $(shell echo $(TEMP_PATH) | sed 's/ //g')

default: clean translate

translate: translate_main

pre_translate_main: $(BUILD_DIR) $(CHANNEL_GEN_DIR)
	@rm -f $(MAIN_SOURCE_LIST)
	@touch $(MAIN_SOURCE_LIST)
        
$(CHANNEL_GEN_DIR)/%.m $(CHANNEL_GEN_DIR)/%.h: $(MAIN_SRC_DIR)/%.java
	@echo $? >> $(MAIN_SOURCE_LIST)

translate_main: pre_translate_main $(MAIN_GEN_SOURCES)
	@if [ `cat $(MAIN_SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR) -d $(CHANNEL_GEN_DIR) \
	    -classpath $(CLASSPATH) \
	    `cat $(MAIN_SOURCE_LIST)` ; \
	fi
	cp -r $(OVERRIDE_GEN_DIR)/ $(CHANNEL_GEN_DIR)

$(BUILD_DIR)/main/%.o: $(CHANNEL_GEN_DIR)/%.m $(MAIN_SRC_DIR)/%.java
	@mkdir -p `dirname $@`
	@$(J2OBJCC) -c $< -o $@ -g -I$(CHANNEL_GEN_DIR)

$(SUPPORT_LIB): $(MAIN_OBJECTS)
	libtool -static -o $(SUPPORT_LIB) $(MAIN_OBJECTS) $(SUPPORT_LIB)

link: translate $(SUPPORT_LIB)

$(CHANNEL_GEN_DIR):
	@mkdir -p $(CHANNEL_GEN_DIR)
$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)/main

clean:
	@rm -rf $(CHANNEL_GEN_DIR) $(BUILD_DIR)
