set mainpath=info\kgeorgiy\ja\smirnov\implementor
set artifacts=java-advanced-2023/artifacts/info.kgeorgiy.java.advanced.implementor.jar
cd ..\..

javac --class-path %artifacts% java-advanced/java-solutions/%mainpath%/*.java

cd java-advanced/java-solutions

jar --create --manifest=..\scripts\MANIFEST.MF --file=..\scripts\implementor.jar %mainpath%\*.java %mainpath%\*.class

del %mainpath%\*.class

cd ..\scripts
