################################################################
#
# BUILD STEPS
#

################################
#
# JDK base
#
FROM adoptopenjdk/openjdk11:slim AS jdk-11-base

# freeze the versions of the Tesseract+ImageMagick for reproducibility
ENV TESSERACT_VERSION 4.1.1+git4247-97079fa3-1
ENV TESSERACT_RES_VERSION 1:4.0.0+git39-6572757-1
ENV IMAGEMAGICK_VERSION 8:6.9.7.4+dfsg-16ubuntu6.8

# add tesseract key
RUN apt-get update && \
    apt-get install -y gnupg && curl https://notesalexp.org/debian/alexp_key.asc | apt-key add - && \
    echo 'deb https://notesalexp.org/tesseract-ocr/bionic/ bionic main' >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y software-properties-common && \
	apt-get install -y tesseract-ocr=$TESSERACT_VERSION tesseract-ocr-eng=$TESSERACT_RES_VERSION tesseract-ocr-osd=$TESSERACT_RES_VERSION && \
	apt-get install -y imagemagick=$IMAGEMAGICK_VERSION --fix-missing && \
	apt-get install -y python3-pip && pip3 install numpy matplotlib scikit-image && \
	apt-get clean autoclean && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*


################################
#
# Tika Server Builder
#
FROM jdk-11-base AS service-builder

# setup the build environment
RUN mkdir -p /devel
WORKDIR /devel

COPY ./gradle/wrapper /devel/gradle/wrapper
COPY ./gradlew /devel/

RUN ./gradlew --version

COPY ./settings.gradle /devel/
COPY . /devel/

# build service
# TIP: uncomment the two lines below to both build the service
#      and run the tests during the build
#COPY ./extras/ImageMagick/policy.xml /etc/ImageMagick-6/policy.xml
#RUN ./gradlew build --no-daemon

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

# freeze the versions of the Tesseract+ImageMagick for reproducibility
ENV TESSERACT_VERSION 4.1.1+git4247-97079fa3-1
ENV TESSERACT_RES_VERSION 1:4.0.0+git39-6572757-1
ENV IMAGEMAGICK_VERSION 8:6.9.7.4+dfsg-16ubuntu6.8

RUN apt-get update && \
    apt-get install -y gnupg && curl https://notesalexp.org/debian/alexp_key.asc | apt-key add - && \
    echo 'deb https://notesalexp.org/tesseract-ocr/bionic/ bionic main' >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y software-properties-common && \
	apt-get install -y tesseract-ocr=$TESSERACT_VERSION tesseract-ocr-eng=$TESSERACT_RES_VERSION tesseract-ocr-osd=$TESSERACT_RES_VERSION && \
	apt-get install -y imagemagick=$IMAGEMAGICK_VERSION --fix-missing && \
	apt-get install -y python3-pip && pip3 install numpy matplotlib scikit-image && \
	apt-get clean autoclean && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*


################################
#
# Tika Service
#
FROM jre-11-base AS service-runner

# setup env
RUN mkdir -p /app/config
WORKDIR /app

# copy tika-server artifacts
COPY --from=service-builder /devel/build/libs/service-*.jar ./
COPY --from=service-builder /devel/src/main/resources/application.yaml ./config/
COPY --from=service-builder /devel/scripts/run.sh ./

# copy external tools configuration files
COPY ./extras/ImageMagick/policy.xml /etc/ImageMagick-6/policy.xml

# uncomment below to set the limit of Open MP parallel processing threads to 1
#ENV OMP_THREAD_LIMIT 1

# entry point
CMD ["/bin/bash", "/app/run.sh"]
