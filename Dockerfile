################################################################
#
# BUILD STEPS
#

################################
#
# JDK base
#
FROM adoptopenjdk/openjdk11:slim AS jdk-11-base

ENV TESSERACT_VERSION 4.00~git2288-10f4998a-2
ENV TESSERACT_RES_VERSION 4.00~git24-0e00fe6-1.2
ENV IMAGEMAGICK_VERSION 8:6.9.7.4+dfsg-16ubuntu6.7

RUN apt-get update && \
#	apt-get dist-upgrade -y && \
#	apt-get install -y tesseract-ocr && \
    apt-get update && \
	apt-get install -y tesseract-ocr=$TESSERACT_VERSION tesseract-ocr-eng=$TESSERACT_RES_VERSION tesseract-ocr-osd=$TESSERACT_RES_VERSION && \
###	apt-get install -y tesseract-ocr-osd=3.04.00-1 tesseract-ocr-eng=3.04.00-1 tesseract-ocr=3.04.01-5 && \
	apt-get install -y imagemagick=$IMAGEMAGICK_VERSION --fix-missing && \
	apt-get clean autoclean && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*


################################
#
# Tika Server Builder
#
FROM jdk-11-base AS server-builder

# setup the build environment
RUN mkdir -p /devel
WORKDIR /devel

COPY ./gradle/wrapper /devel/gradle/wrapper
COPY ./gradlew /devel/

RUN ./gradlew --version

COPY ./settings.gradle /devel/
COPY . /devel/

# build service
RUN ./gradlew bootJar --no-daemon



################################################################
#
# RUN STEPS
#

################################
#
# JRE base
#
FROM adoptopenjdk/openjdk11:jre AS jre-11-base

ENV TESSERACT_VERSION 4.00~git2288-10f4998a-2
ENV TESSERACT_RES_VERSION 4.00~git24-0e00fe6-1.2
ENV IMAGEMAGICK_VERSION 8:6.9.7.4+dfsg-16ubuntu6.7

RUN apt-get update && \
#	apt-get dist-upgrade -y && \
#	apt-get install -y tesseract-ocr && \
    apt-get update && \
	apt-get install -y tesseract-ocr=$TESSERACT_VERSION tesseract-ocr-eng=$TESSERACT_RES_VERSION tesseract-ocr-osd=$TESSERACT_RES_VERSION && \
###	apt-get install -y tesseract-ocr-osd=3.04.00-1 tesseract-ocr-eng=3.04.00-1 tesseract-ocr=3.04.01-5 && \
	apt-get install -y imagemagick=$IMAGEMAGICK_VERSION --fix-missing && \
	apt-get clean autoclean && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*


################################
#
# Tika Server
#
FROM jre-11-base AS server-runner

# setup env
RUN mkdir -p /app
WORKDIR /app

# copy artifacts
COPY --from=server-builder /devel/build/libs/server-*.jar ./

# copy external tools configuration files
COPY ./extras/ImageMagick/policy.xml /etc/ImageMagick-6/policy.xml

# entry point
ENTRYPOINT /bin/bash
CMD ["java", "-jar", "server-*.jar"]