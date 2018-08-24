#!/bin/bash

#echo `java -version`
#export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home
#export PATH=$JAVA_HOME/bin:$PATH:.

FilePath=""
ClassName=""
Runtime=5
RunTimes=3
ExitCode=124

#ProjectPath="./../../.."
ProjectPath="."
ClassPath="${ProjectPath}/out/production/FMCR"
LibPath="${ProjectPath}/lib"
#add realWorldApplications class path
realWorldAppliactionsRootPath="${ProjectPath}/realWorldApplications"
Dbcp3Path="${ProjectPath}/out/production/Dbcp3"
Dbcp3LibPath="${realWorldAppliactionsRootPath}/Dbcp3/realLib/*"
JDK64Path="${ProjectPath}/out/production/JDK64"
JDK64LibPath="${realWorldAppliactionsRootPath}/JDK64/realLib/*"
Log4j1Path="${ProjectPath}/out/production/Log4j1"
Log4j1LibPath="${realWorldAppliactionsRootPath}/Log4j1/realLib/*"
Log4j3Path="${ProjectPath}/out/production/Log4j3"
Log4j3LibPath="${realWorldAppliactionsRootPath}/Log4j3/realLib/*"
Pool1Path="${ProjectPath}/out/production/Pool1"
Pool1LibPath="${realWorldAppliactionsRootPath}/Pool1/realLib/*"
ClassPathS="${ClassPath}:${LibPath}/*:\
            ${Dbcp3Path}:${Dbcp3LibPath}:\
            ${JDK64Path}:${JDK64LibPath}:\
            ${Log4j1Path}:${Log4j1LibPath}:\
            ${Log4j3Path}:${Log4j1LibPath}:\
            ${Pool1Path}:${Pool1LibPath}"
ClassPathS=${ClassPathS// }
#default testing program
ClassName="test.examples.SimpleTest.SimpleMainTest"

while getopts "f:c:t:n:" opt ; do
    case $opt in
        f)
            FilePath="$OPTARG"
            ;;
        c)
            ClassName="$OPTARG"
            ;;
        t)
            Runtime="$OPTARG"
            ;;
        n)
            RunTimes="$OPTARG"
            ;;
        \?)
          echo "Invalid option: -$OPTARG"
          exit 1
          ;;
        :)
          echo "Option -$OPTARG requires an argument."
          exit 1
          ;;
    esac
done

if [ -f "${FilePath}" ];then
    #execute filelist program
    for  line  in  `cat ${FilePath}`
    do
        for ((runtime = 0; runtime < RunTimes; runtime++)) ; do

#           kill `jps | grep "JUnitStarter" | cut -d " " -f1`
            echo "Running tests from: ${line}"
            gtimeout -k 10s ${Runtime}m java -ea -Xmx4g -javaagent:${LibPath}/magent.jar \
                              -cp "${ClassPathS}" \
                              org.junit.runner.JUnitCore ${line}
            while [ $? -eq ${ExitCode} ]
            do
                echo "Reach timeout , restart running tests from: ${lne}"
                gtimeout -k 10s ${Runtime}m java -ea -Xmx4g -javaagent:${LibPath}/magent.jar \
                                  -cp "${ClassPathS}" \
                                  org.junit.runner.JUnitCore ${line};
            done
        done
    done
else
    #execute single program
    for ((runtime = 0; runtime < RunTimes; runtime++)) ; do

#        kill `jps | grep "JUnitStarter" | cut -d " " -f1`
        echo "Running tests from: ${ClassName}"
        gtimeout -k 10s ${Runtime}m java -ea -Xmx4g -javaagent:${LibPath}/magent.jar \
                          -cp "${ClassPathS}" \
                          org.junit.runner.JUnitCore ${ClassName};
        while [ $? -eq ${ExitCode} ]
        do
            echo "Reach timeout , restart running tests from: ${ClassName}"
            gtimeout -k 10s ${Runtime}m java -ea -Xmx4g -javaagent:${LibPath}/magent.jar \
                              -cp "${ClassPathS}" \
                              org.junit.runner.JUnitCore ${ClassName};
        done
    done
fi

# functions
# $1 -- test class
#function runTests()
#{
#    echo "Running tests from: ${1}"
#
#    java -ea -Xmx4g -javaagent:${LibPath}/magent.jar \
#    -cp "${ClassPath}:${LibPath}/*" \
#    org.junit.runner.JUnitCore $1
#}


