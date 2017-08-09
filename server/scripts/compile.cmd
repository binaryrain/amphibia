CD ..

CALL %MAVEN_HOME%/bin/mvn dependency:copy-dependencies

%JAVA_HOME%/bin/javac -d ext -sourcepath src -classpath ext/dependency/* src/com/equinix/converter/Converter.java
%JAVA_HOME%/bin/javac -d ext -sourcepath src -classpath ext/dependency/* src/com/equinix/builder/Builder.java

CD ext

%JAVA_HOME%/bin/jar cvfe Converter.jar com.equinix.converter.Converter com/equinix/converter/*.class
%JAVA_HOME%/bin/jar cvfe Builder.jar com.equinix.builder.Builder com/equinix/builder/*.class

REM java -cp Converter.jar;dependency/* com.equinix.converter.Converter
REM java -cp Builder.jar;dependency/* com.equinix.builder.Builder

PAUSE