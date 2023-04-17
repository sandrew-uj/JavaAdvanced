SET link="https://docs.oracle.com/en/java/javase/19/docs/api/"
set mainpath="info\kgeorgiy\ja\smirnov\implementor"
set artifacts="../java-advanced-2023/artifacts/info.kgeorgiy.java.advanced.implementor.jar"

cd..

javadoc -cp %artifacts% -link %link% -private java-solutions/%mainpath%/Implementor.java java-solutions/%mainpath%/MyMethod.java -d javadoc