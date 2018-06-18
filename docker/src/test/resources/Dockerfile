FROM java:8-jre

MAINTAINER dev@karaf.apache.org

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

ENV KARAF_VERSION=4.2.0

RUN cd /opt; \
	wget http://www-us.apache.org/dist/karaf/${KARAF_VERSION}/apache-karaf-${KARAF_VERSION}.tar.gz; \
	tar -xzf apache-karaf-${KARAF_VERSION}.tar.gz; \
	mv apache-karaf-${KARAF_VERSION} apache-karaf; \
	rm -f apache-karaf-${KARAF_VERSION}.tar.gz

EXPOSE 1099 8101 44444

ENTRYPOINT ["/opt/apache-karaf/bin/karaf"]