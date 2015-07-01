# o2tab Help #

## Overview ##
o2tab is an automatic pipeline for clustering of DNA sequences generated with the most common NGS machines. This pipeline works using 2 main softwares:  
1.  pandaseq assembler, available [here](https://github.com/neufeld/pandaseq "pandaseq")  
2.  usearch suite, available [here](http://www.drive5.com/usearch/ "usearch")  
In addition to these components, o2tab needs to know the path to the "python scripts" needed by usearch for clustering. A description of these scripts can be found [here](http://drive5.com/python/ "scripts"). In particular, this pipeline needs the script named: ``uc2otutab.py``, so you can download only this script if the others are not needed.

## Installing ##
o2tab is a maven project so the best way to compile it is using [maven](https://maven.apache.org/ "maven"). In case you are not familiar with this tool you can download a precompliled verision of o2tab [here](http://www.filehosting.org/file/details/495631/o2tab.zip "precompiled").

### Maven Installation ##
First of all download (or clone) the o2tab repository from GitHub. Then navigate to the repository path until you see the ``pom.xml`` file (it is in the main directory of the cloned repository). Finally, run these maven commands:  
``mvn package clean``  
If everithing goes well, now you should see a ``jar`` file named ``o2tab-[version].jar`` where ``[version]`` corresponds to the program version. During its lifecicle maven performs some JUnit tests to check if all programs are correctly installed and the config files does not contains any errors. In case the maven ``test`` phase ends with error, check the messages displayed in the console to figure out what is going on. If you want to test your configuration files again you can do it with the ``mvn test clean`` command. In this way only the JUnit test will be executed without re-compiling any other ``jar`` file (for a complete description of the configuration files read the ``readme.txt`` file inside the ``config`` directory).

## Usage examples ##
Say that you have a bunch of paired-end sequence files in a folder named ``raw_data``. The files are named with a prefix ``Samples`` followed by the number of the sample and the mate pair descriptor, say ``R1`` for forward reads and ``R2`` for reverse ones. If you want to assemble and clustering these file using o2tab you can simply run:
``java -jar o2tab-[version].jar --in /path/to/raw_data/ --mate R1,R2 --filter Sample --out /path/to/output/``
this command will assemble all paired sequence and convert them into an otu table, saving results in the output path.