cd ..\java-solutions
set JAR=..\scripts\implementor.jar
set CLASS=info.kgeorgiy.ja.smirnov.implementor.Implementor
set DIR=C:\JavaProjects\java-advanced\scripts
set MAIN=info\kgeorgiy\ja\smirnov\implementor
javac %MAIN%\Implementor.java
java -jar %JAR% %CLASS% %DIR%
java -jar %JAR% -jar %CLASS% %DIR%\ImplementorImpl.jar
del %MAIN%\*.class