################################################################
#
# BUILD STEPS
#

################################
#
# JDK base
#

FROM openjdk:17.0.2-bullseye AS jdk-base-builder

# freeze the versions of the Tesseract+ImageMagick for reproducibility
ENV TESSERACT_VERSION 4.1.1-2.1
ENV TESSERACT_RES_VERSION 1:4.00~git30-7274cfa-1.1
ENV IMAGEMAGICK_VERSION 8:6.9.11.60+dfsg-1


# add tesseract key
RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y apt-transport-https apt-utils && \
    apt-get install -y curl software-properties-common && \
    apt-add-repository non-free && \
    apt-add-repository contrib && \
    apt-get update && apt-get upgrade -y
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends ocl-icd-dev ocl-icd-opencl-dev ocl-icd-libopencl1 oclgrind opencl-headers libtiff-dev build-essential nvidia-driver nvidia-egl-icd pocl-opencl-icd intel-opencl-icd mesa-opencl-icd libpocl-dev beignet-opencl-icd nvidia-opencl-icd nvidia-egl-common nvidia-cuda-dev nvidia-cuda-toolkit
RUN apt-get install -y libtika-java libtomcat9-java libtomcat9-embed-java libtcnative-1 && \
	apt-get install -y python3-pip && pip3 install numpy pycuda matplotlib scikit-image && \
    apt-get install -y fonts-deva gsfonts fonts-gfs-didot fonts-gfs-didot-classic fonts-junicode fonts-ebgaramond ttf-mscorefonts-installer && \
    apt-get install -y --fix-missing ghostscript ghostscript-x gsfonts gsfonts-other gsfonts-x11 && \
    apt-get install -y --fix-missing imagemagick tesseract-ocr tesseract-ocr-eng tesseract-ocr-osd tesseract-ocr-lat tesseract-ocr-fra tesseract-ocr-deu && \
	#apt-get install -y tesseract-ocr=$TESSERACT_VERSION tesseract-ocr-eng=$TESSERACT_RES_VERSION tesseract-ocr-osd=$TESSERACT_RES_VERSION && \
	#apt-get install -y imagemagick=$IMAGEMAGICK_VERSION --fix-missing && \
	apt-get clean autoclean && \
    apt-get autoremove --purge -y && \
    rm -rf /var/lib/apt/lists/*

# setup the build environment
RUN mkdir -p /docker_build

COPY ./gradle/wrapper /docker_build/gradle/wrapper
COPY ./gradlew /docker_build/
COPY ./settings.gradle /docker_build/
COPY . /docker_build/

COPY ./extras/ImageMagick/policy.xml /etc/ImageMagick-6/policy.xml

WORKDIR /docker_build

# build service
# TIP: uncomment the two lines below to both build the service and run the tests during the build
#RUN ./gradlew build --no-daemon
RUN ./gradlew bootJar --no-daemon

################################
#
# Tika Service
#
FROM jdk-base-builder AS service-runner

# setup env
RUN mkdir -p /app/config

WORKDIR /app
# copy tika-server artifacts
COPY --from=jdk-base-builder /docker_build/build/libs/ /app
COPY --from=jdk-base-builder /docker_build/src/main/resources/application.yaml /app/config/
COPY --from=jdk-base-builder /docker_build/scripts/run.sh /app

# remove old build folder
RUN rm -rf /docker_build

# entry point
CMD ["/bin/bash", "/app/run.sh"]
