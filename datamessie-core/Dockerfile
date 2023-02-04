FROM tomcat:9-jdk8-corretto
COPY target/datamessie.war /usr/local/tomcat/webapps
EXPOSE 8080
CMD ["catalina.sh", "run"]
