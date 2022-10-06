################################################################
#
# BUILD STEPS
#

################################
#
# JDK base
#

FROM ubuntu:kinetic AS jdk-base-builder

# freeze the versions of the Tesseract+ImageMagick for reproducibility
ENV TESSERACT_VERSION 4.1.1-2.1
ENV TESSERACT_RES_VERSION 1:4.00~git30-7274cfa-1.1
ENV IMAGEMAGICK_VERSION 8:6.9.11.60+dfsg-1

ENV DEBIAN_FRONTEND=noninteractive
ENV DEBIAN_PRIORITY=critical

ENV NVIDIA_DRIVER_VERSION=510
ENV OPENJDK_VERSION=17

# nvidia-container-runtime
ENV NVIDIA_VISIBLE_DEVICES all
ENV NVIDIA_DRIVER_CAPABILITIES compute,utility

# add tesseract key
RUN apt-get update && apt-get upgrade -y
RUN apt-get install -y apt-transport-https apt-utils curl software-properties-common gnupg
RUN apt-get update && apt-get upgrade -y

# add extra repos
RUN apt-add-repository multiverse universe # ppa:graphics-drivers/ppa
RUN apt-get update && apt-get upgrade -y

# Nvidia cuda
#RUN wget https://developer.download.nvidia.com/compute/cuda/11.6.2/local_installers/cuda-repo-debian11-11-6-local_11.6.2-510.47.03-1_amd64.deb
#RUN dpkg -i ./cuda-repo-debian11-11-6-local_11.6.2-510.47.03-1_amd64.deb
#RUN apt-key add /var/cuda-repo-debian11-11-6-local/7fa2af80.pub
#RUN apt-get update --fix-missing && apt-get upgrade -y
#RUN apt-get -y install cuda


# OpenJDK
RUN apt-get install -y openjdk-$OPENJDK_VERSION-jdk

# OpenCL
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends initramfs-tools xz-utils ocl-icd-dev ocl-icd-opencl-dev ocl-icd-libopencl1 oclgrind opencl-headers libtiff-dev build-essential clinfo dkms pocl-opencl-icd intel-opencl-icd mesa-opencl-icd libpocl-dev # nvidia-opencl-icd  nvidia-driver nvidia-egl-icd nvidia-egl-common nvidia-cuda-dev nvidia-cuda-toolkit

# NVIDIA Docker
RUN export distribution=$(. /etc/os-release;echo $ID$VERSION_ID)

RUN curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | apt-key add -
RUN curl -s -L https://nvidia.github.io/nvidia-docker/$(. /etc/os-release;echo $ID$VERSION_ID)/nvidia-docker.list | tee /etc/apt/sources.list.d/nvidia-docker.list
RUN curl -s -L https://nvidia.github.io/libnvidia-container/experimental/$(. /etc/os-release;echo $ID$VERSION_ID)/libnvidia-container-experimental.list | tee /etc/apt/sources.list.d/libnvidia-container-experimental.list

RUN apt-get update && apt-get upgrade -y

# RUN apt-get install -y nvidia-docker2 nvidia-container-toolkit

# Other requirements for Tika & Tesseract OCR
RUN echo "ttf-mscorefonts-installer msttcorefonts/accepted-mscorefonts-eula select true" | debconf-set-selections
RUN apt-get install -y --no-install-recommends fontconfig ttf-mscorefonts-installer
ADD ./extras/localfonts.conf /etc/fonts/local.conf
RUN fc-cache -f -v

RUN apt-get install -y libimage-exiftool-perl libtika-java libtomcat9-java libtomcat9-embed-java libtcnative-1 && \
	apt-get install -y python3-pip && pip3 install numpy matplotlib scikit-image && \
    apt-get install -y ttf-mscorefonts-installer fontconfig && \
    apt-get install -y ffmpeg gstreamer1.0-libav fonts-deva fonts-dejavu fonts-gfs-didot fonts-gfs-didot-classic fonts-junicode fonts-ebgaramond fonts-noto-cjk fonts-takao-gothic fonts-vlgothic && \
    apt-get install -y --fix-missing ghostscript ghostscript-x gsfonts gsfonts-other gsfonts-x11 fonts-croscore fonts-crosextra-caladea fonts-crosextra-carlito fonts-liberation fonts-open-sans fonts-noto-core fonts-ibm-plex fonts-urw-base35 && \
    apt-get install -y --fix-missing imagemagick tesseract-ocr tesseract-ocr-eng tesseract-ocr-osd tesseract-ocr-lat tesseract-ocr-fra tesseract-ocr-deu && \
	#apt-get install -y tesseract-ocr=$TESSERACT_VERSION tesseract-ocr-eng=$TESSERACT_RES_VERSION tesseract-ocr-osd=$TESSERACT_RES_VERSION && \
	#apt-get install -y imagemagick=$IMAGEMAGICK_VERSION --fix-missing && \
	apt-get clean autoclean && \
    apt-get autoremove --purge -y && \
    rm -rf /var/lib/apt/lists/*

# RUN apt-get install x11-xserver-utils
# RUN xhost +local:username

RUN mkdir -p /etc/OpenCL/vendors && \
    echo "libnvidia-opencl.so.1" > /etc/OpenCL/vendors/nvidia.icd

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
