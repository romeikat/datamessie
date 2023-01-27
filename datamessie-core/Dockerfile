FROM tomcat:9-jdk8-corretto
COPY target/datamessie.war /usr/local/tomcat/webapps
COPY src/conf/*.properties /usr/local/tomcat/conf
EXPOSE 8080
CMD ["catalina.sh", "run"]
