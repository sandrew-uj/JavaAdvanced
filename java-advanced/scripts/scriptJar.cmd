@echo off

set MODULE=info.kgeorgiy.ja.osipov.implementor
set DIR=info\kgeorgiy\ja\osipov\implementor

mkdir %MODULE%
mkdir %MODULE%\%DIR%
copy ..\java-solutions\module-info.java %MODULE%
copy MANIFEST.MF %MODULE%
copy ..\java-solutions\%DIR%\*.java %MODULE%\%DIR%

mkdir javac_files

javac -target 17 ^
	-d .\javac_files ^
	-p ..\..\java-advanced-2023\artifacts\;..\..\java-advanced-2023\lib ^
	--module-source-path . ^
	--module info.kgeorgiy.ja.osipov.implementor

jar -c -f Implementor.jar -m %MODULE%\MANIFEST.MF -C javac_files/%MODULE% .
del /s/q %MODULE%
rmdir /s/q %MODULE%
del /s/q javac_files
rmdir /s/q javac_files
