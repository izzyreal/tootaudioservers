CLASSPATH=../../bin
# Note that the Makefile doesn't support building other platforms than your current

arch :=  $(shell echo `/bin/arch`)
os := $(shell echo `/bin/uname`)

ifeq (${arch},i686)
 ARCH=i386
else ifeq (${arch},x86_64)
 ARCH=amd64
endif 


LIBPATH=../../lib/${os}/${ARCH}

# (PJS) JAVA_HOME should be set externally??

# JAVA_HOME=/opt/jdk1.6.0
JAVAC=${JAVA_HOME}/bin/javac
JAVAH=${JAVA_HOME}/bin/javah

INCDIR = -I$(JAVA_HOME)/include/ -I$(JAVA_HOME)/include/linux 

all: libpriority.so install

libpriority.h: ${CLASSPATH}/com/frinika/priority/Priority.class
	${JAVAH} -classpath ${CLASSPATH} -o libpriority.h  ${CLASPATH} com.frinika.priority.Priority 

libpriority.so: libpriority.cpp libpriority.h  Makefile
	g++ -fPIC -shared libpriority.cpp  ${INCDIR}  -o libpriority.so -lpthread 
 

install:
	mkdir -p ${LIBPATH}
	mv libpriority.so ${LIBPATH}

clean:
	rm *.class libpriority.h *.so

