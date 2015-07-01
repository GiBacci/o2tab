Config file fileds:

ENV: the directory where the executable file is located. This token accepts
     only one argument and this argument must be a directory 
     (ex: ENV /path/to/executable/)
CMD: specifies the name of the executable file. This token accepts only a 
     value and this value must not be a directory 
     (ex: CMD executable_name)
ARG: options with argument. This token accepts one option and one argument
     (ex: ARG arg1 arg2)
VAL: flag value. This token accepts only a value after it. (ex: VAL flag)

# comment